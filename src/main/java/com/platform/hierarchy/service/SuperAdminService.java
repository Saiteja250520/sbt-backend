package com.platform.hierarchy.service;

import com.platform.hierarchy.dto.ManagerCreateRequest;
import com.platform.hierarchy.model.*;
import com.platform.hierarchy.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SuperAdminService {

    private final UserRepository userRepository;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransferRepository transferRepository;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public SuperAdminService(UserRepository userRepository, DepositRepository depositRepository,
                             WithdrawalRepository withdrawalRepository, TransferRepository transferRepository,
                             InvestmentRepository investmentRepository, TransactionRepository transactionRepository,
                             PlatformSettingsRepository platformSettingsRepository, NotificationService notificationService,
                             AuditLogService auditLogService, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.transferRepository = transferRepository;
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
        this.platformSettingsRepository = platformSettingsRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    private User getSystemOwner(String ownerLoginId) {
        User owner = userRepository.findByLoginId(ownerLoginId)
                .orElseThrow(() -> new IllegalArgumentException("System Owner not found: " + ownerLoginId));
        if (!"SYSTEM_OWNER".equalsIgnoreCase(owner.getRole())) {
            throw new SecurityException("Unauthorized. Must be a System Owner.");
        }
        return owner;
    }

    @Transactional(readOnly = true)
    public List<User> getAllAdmins() {
        return userRepository.findByRole("MANAGER"); // Admins mapped to Managers
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findByRole("CUSTOMER"); // Users mapped to Customers
    }

    public User createManager(String ownerLoginId, ManagerCreateRequest request) {
        User owner = getSystemOwner(ownerLoginId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Organization Email already in use.");
        }

        // Generate Login ID and Temp Password
        String loginId = authService.generateUniqueLoginId();
        String tempPassword = authService.generateTempPassword();

        User manager = new User();
        manager.setLoginId(loginId);
        manager.setPassword(passwordEncoder.encode(tempPassword));
        manager.setEmail(request.getEmail());
        manager.setFullName(request.getFullName());
        manager.setPhoneNumber(request.getPhoneNumber());
        manager.setAddress(request.getAddress());
        manager.setNotes(request.getNotes());
        manager.setRole("MANAGER");
        manager.setStatus("ACTIVE"); // Active by default on creation
        manager.setParent(owner);
        manager.setFirstLogin(true);

        userRepository.save(manager);

        // Notify Manager (logs simulated delivery)
        notificationService.sendNotification(
                manager,
                "Welcome to SBT Crypto. Your Manager Login ID is: " + loginId + 
                " and Temporary Password is: " + tempPassword + ". Please change it immediately upon logging in."
        );

        auditLogService.log("Manager Created", owner.getLoginId(), owner.getRole(), manager.getId().toString());

        // Return manager object with un-encoded temporary password for display to the owner
        User displayUser = new User();
        displayUser.setId(manager.getId());
        displayUser.setLoginId(manager.getLoginId());
        displayUser.setEmail(manager.getEmail());
        displayUser.setFullName(manager.getFullName());
        displayUser.setPassword(tempPassword); // Attach raw temp password for UI receipt modal
        return displayUser;
    }

    public void updateAdminStatus(String ownerLoginId, Long managerId, String newStatus) {
        User owner = getSystemOwner(ownerLoginId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found with ID: " + managerId));

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new IllegalArgumentException("User with ID: " + managerId + " is not a Manager.");
        }

        manager.setStatus(newStatus.toUpperCase());
        userRepository.save(manager);

        notificationService.sendNotification(
                manager,
                "Your Manager account status has been updated to: " + newStatus.toUpperCase() + " by System Owner."
        );

        auditLogService.log(
                "Manager Status Updated (" + newStatus + ")",
                owner.getLoginId(),
                owner.getRole(),
                manager.getId().toString()
        );
    }

    public PlatformSettings updateGlobalSettings(String ownerLoginId, Double newBalance) {
        getSystemOwner(ownerLoginId);
        
        PlatformSettings settings = platformSettingsRepository.findById(1L)
                .orElse(new PlatformSettings());
        
        double oldBalance = settings.getPlatformBalance();
        settings.setPlatformBalance(newBalance);
        platformSettingsRepository.save(settings);

        auditLogService.log(
                "Platform Balance Updated",
                ownerLoginId,
                "SYSTEM_OWNER",
                "Old: ₹" + oldBalance + " -> New: ₹" + newBalance
        );

        return settings;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSuperAdminReports(String ownerLoginId) {
        List<User> managers = userRepository.findByRole("MANAGER");
        List<User> customers = userRepository.findByRole("CUSTOMER");

        long totalAdmins = managers.size();
        long totalUsers = customers.size();
        double totalUserBalances = customers.stream().mapToDouble(User::getBalance).sum();

        // Financials
        List<Deposit> deposits = depositRepository.findAll();
        double totalApprovedDeposits = deposits.stream()
                .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                .mapToDouble(Deposit::getAmount).sum();

        List<Withdrawal> withdrawals = withdrawalRepository.findAll();
        double totalApprovedWithdrawals = withdrawals.stream()
                .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                .mapToDouble(Withdrawal::getAmount).sum();

        List<Transfer> transfers = transferRepository.findAll();
        double totalTransferred = transfers.stream().mapToDouble(Transfer::getAmount).sum();

        List<Investment> investments = investmentRepository.findAll();
        double totalInvestments = investments.stream().mapToDouble(Investment::getAmount).sum();

        // Platform Settings (for Platform Balance)
        PlatformSettings settings = platformSettingsRepository.findById(1L)
                .orElse(new PlatformSettings());

        // Manager Summaries
        List<Map<String, Object>> managerSummaries = new ArrayList<>();
        for (User manager : managers) {
            List<User> assignedCustomers = userRepository.findByParentAndRole(manager, "CUSTOMER");
            double sumOfBalances = assignedCustomers.stream().mapToDouble(User::getBalance).sum();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", manager.getId());
            summary.put("loginId", manager.getLoginId());
            summary.put("username", manager.getLoginId()); // Maintain key mapping for table compatibility
            summary.put("fullName", manager.getFullName());
            summary.put("email", manager.getEmail());
            summary.put("status", manager.getStatus());
            summary.put("assignedUserCount", assignedCustomers.size());
            summary.put("sumOfBalances", sumOfBalances);
            summary.put("createdAt", manager.getCreatedAt());
            managerSummaries.add(summary);
        }

        // Build Report Map
        Map<String, Object> report = new HashMap<>();
        report.put("totalAdmins", totalAdmins);
        report.put("totalUsers", totalUsers);
        report.put("totalUserBalances", totalUserBalances);
        report.put("totalApprovedDeposits", totalApprovedDeposits);
        report.put("totalApprovedWithdrawals", totalApprovedWithdrawals);
        report.put("totalTransferred", totalTransferred);
        report.put("totalInvestments", totalInvestments);
        report.put("platformBalance", settings.getPlatformBalance()); // Modifiable pool
        report.put("adminSummaries", managerSummaries);

        // Growth analytics trends
        List<Map<String, Object>> growthData = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] depositTrends = {12000, 19000, 32000, 50000, 75000, totalApprovedDeposits};
        double[] withdrawalTrends = {3000, 5000, 8000, 15000, 20000, totalApprovedWithdrawals};

        for (int i = 0; i < months.length; i++) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("month", months[i]);
            trend.put("deposits", depositTrends[i]);
            trend.put("withdrawals", withdrawalTrends[i]);
            growthData.add(trend);
        }
        report.put("growthAnalytics", growthData);

        return report;
    }
}
