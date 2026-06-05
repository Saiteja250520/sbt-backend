package com.platform.hierarchy.service;

import com.platform.hierarchy.dto.FirstLoginSetupRequest;
import com.platform.hierarchy.dto.LoginRequest;
import com.platform.hierarchy.dto.LoginResponse;
import com.platform.hierarchy.dto.RegisterRequest;
import com.platform.hierarchy.model.PlatformSettings;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.repository.PlatformSettingsRepository;
import com.platform.hierarchy.repository.UserRepository;
import com.platform.hierarchy.security.TokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    // Thread-safe map to cache generated OTPs (loginId -> OTP string)
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PlatformSettingsRepository platformSettingsRepository,
                       PasswordEncoder passwordEncoder, TokenProvider tokenProvider,
                       AuditLogService auditLogService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.platformSettingsRepository = platformSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public String registerCustomer(RegisterRequest request) {
        // Uniqueness check for email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered.");
        }

        if (request.getParentId() == null) {
            throw new IllegalArgumentException("Customers must select a Management Account.");
        }

        User parentManager = userRepository.findById(request.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Selected Management Account not found."));

        if (!"MANAGER".equalsIgnoreCase(parentManager.getRole())) {
            throw new IllegalArgumentException("Selected parent must be a Management Account.");
        }

        if (!"ACTIVE".equalsIgnoreCase(parentManager.getStatus())) {
            throw new IllegalArgumentException("Selected Manager is not currently Active.");
        }

        // Generate temporary placeholder Login ID using email (overwritten on approval)
        String placeholderLoginId = request.getEmail();
        if (userRepository.existsByLoginId(placeholderLoginId)) {
            placeholderLoginId = "pending_" + System.currentTimeMillis();
        }

        User newCustomer = new User();
        newCustomer.setLoginId(placeholderLoginId);
        newCustomer.setPassword(passwordEncoder.encode("TemporaryPass@123")); // Will be overwritten on approval
        newCustomer.setEmail(request.getEmail());
        newCustomer.setFullName(request.getFullName());
        newCustomer.setPhoneNumber(request.getPhoneNumber());
        newCustomer.setAddress(request.getAddress());
        newCustomer.setNotes(request.getNotes());
        newCustomer.setRole("CUSTOMER");
        newCustomer.setStatus("PENDING_APPROVAL");
        newCustomer.setParent(parentManager);
        newCustomer.setFirstLogin(true);

        userRepository.save(newCustomer);

        // Notify Manager
        notificationService.sendNotification(
                parentManager,
                "New Customer Registration: '" + newCustomer.getFullName() + "' is pending approval."
        );

        // Log audit trail
        auditLogService.log(
                "Customer Registration Submitted",
                newCustomer.getFullName(),
                "CUSTOMER",
                newCustomer.getId().toString()
        );

        return "Registration successful. Awaiting approval from Manager: " + parentManager.getFullName();
    }

    public String initiateLogin(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BadCredentialsException("Invalid Login ID or Password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.log("Login Failed - Incorrect Password", request.getLoginId(), user.getRole(), null);
            throw new BadCredentialsException("Invalid Login ID or Password.");
        }

        // Check account status
        validateAccountStatus(user);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(user.getLoginId(), otp);

        System.out.println("=========================================");
        System.out.println("SBT CRYPTO OTP FOR USER [" + user.getLoginId() + "]: " + otp);
        System.out.println("=========================================");

        auditLogService.log("OTP Generated", user.getLoginId(), user.getRole(), null);

        return otp;
    }

    public LoginResponse verifyOtpAndLogin(String loginId, String otpCode) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadCredentialsException("User not found."));

        validateAccountStatus(user);

        String cachedOtp = otpCache.get(loginId);
        if (cachedOtp == null || !cachedOtp.equals(otpCode)) {
            auditLogService.log("Login Failed - Invalid OTP", loginId, user.getRole(), null);
            throw new BadCredentialsException("Invalid or expired OTP.");
        }

        otpCache.remove(loginId);

        // Create authentication context
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                Collections.singletonList(authority)
        );

        String jwt = tokenProvider.createToken(authentication);

        auditLogService.log("Login Successful", loginId, user.getRole(), user.getId().toString());

        return new LoginResponse(
                jwt,
                user.getId(),
                user.getLoginId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getFirstLogin()
        );
    }

    public LoginResponse firstLoginSetup(String currentLoginId, FirstLoginSetupRequest request) {
        User user = userRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!user.getFirstLogin()) {
            throw new IllegalStateException("First login setup already completed for this account.");
        }

        // 1. Mandatory password change
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("New password is mandatory on first login.");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 2. Optional Login ID change (with uniqueness check)
        if (request.getLoginId() != null && !request.getLoginId().trim().isEmpty() 
            && !request.getLoginId().equals(user.getLoginId())) {
            
            if (userRepository.existsByLoginId(request.getLoginId())) {
                throw new IllegalArgumentException("Requested Login ID already exists.");
            }
            user.setLoginId(request.getLoginId());
        }

        // 3. Update profile fields
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        // 4. Configure platform settings (if role is SYSTEM_OWNER)
        if ("SYSTEM_OWNER".equalsIgnoreCase(user.getRole())) {
            PlatformSettings settings = platformSettingsRepository.findById(1L)
                    .orElse(new PlatformSettings());
            
            if (request.getPlatformBalance() != null) {
                settings.setPlatformBalance(request.getPlatformBalance());
            }
            if (request.getCompanyName() != null && !request.getCompanyName().isEmpty()) {
                settings.setCompanyName(request.getCompanyName());
            }
            if (request.getApplicationName() != null && !request.getApplicationName().isEmpty()) {
                settings.setApplicationName(request.getApplicationName());
            }
            if (request.getMotto() != null && !request.getMotto().isEmpty()) {
                settings.setMotto(request.getMotto());
            }
            platformSettingsRepository.save(settings);
            
            auditLogService.log("Global Settings Configured", user.getLoginId(), user.getRole(), "1");
        }

        // Mark setup complete
        user.setFirstLogin(false);
        userRepository.save(user);

        auditLogService.log("First Login Setup Completed", user.getLoginId(), user.getRole(), user.getId().toString());

        // Return a fresh token based on new/updated Login ID credentials
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getLoginId(),
                null,
                Collections.singletonList(authority)
        );
        String jwt = tokenProvider.createToken(authentication);

        return new LoginResponse(
                jwt,
                user.getId(),
                user.getLoginId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getFirstLogin()
        );
    }

    private void validateAccountStatus(User user) {
        String status = user.getStatus();
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) {
            throw new DisabledException("Account is pending approval. You cannot log in yet.");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            throw new DisabledException("Account registration has been rejected.");
        } else if ("BLOCKED".equalsIgnoreCase(status)) {
            throw new LockedException("Account has been blocked. Access denied.");
        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
            throw new LockedException("Account is suspended.");
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            throw new DisabledException("Account is inactive.");
        }
    }

    // --- HELPER GENERATORS ---
    public String generateUniqueLoginId() {
        Random random = new Random();
        String loginId;
        do {
            loginId = String.format("%09d", random.nextInt(1000000000));
        } while (userRepository.existsByLoginId(loginId));
        return loginId;
    }

    public String generateTempPassword() {
        Random random = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
