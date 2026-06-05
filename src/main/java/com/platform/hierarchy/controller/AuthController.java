package com.platform.hierarchy.controller;

import com.platform.hierarchy.dto.FirstLoginSetupRequest;
import com.platform.hierarchy.dto.LoginRequest;
import com.platform.hierarchy.dto.LoginResponse;
import com.platform.hierarchy.dto.RegisterRequest;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.repository.UserRepository;
import com.platform.hierarchy.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        try {
            String message = authService.registerCustomer(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            String otpCode = authService.initiateLogin(request);
            Map<String, Object> response = new HashMap<>();
            response.put("otpRequired", true);
            response.put("loginId", request.getLoginId());
            response.put("message", "Simulated OTP code generated.");
            response.put("simulatedOtp", otpCode); // Prefill helper
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String loginId, @RequestParam String otpCode) {
        try {
            LoginResponse response = authService.verifyOtpAndLogin(loginId, otpCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/first-login-setup")
    public ResponseEntity<?> configureFirstLogin(@RequestBody FirstLoginSetupRequest request, Principal principal) {
        try {
            LoginResponse response = authService.firstLoginSetup(principal.getName(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Public list of active Managers to populate assigned dropdown
    @GetMapping("/managers")
    public ResponseEntity<List<Map<String, Object>>> getActiveManagers() {
        List<User> activeManagers = userRepository.findByRoleAndStatus("MANAGER", "ACTIVE");
        List<Map<String, Object>> managerList = activeManagers.stream().map(mgr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", mgr.getId());
            map.put("fullName", mgr.getFullName());
            map.put("loginId", mgr.getLoginId());
            map.put("email", mgr.getEmail());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(managerList);
    }
}
