package com.platform.hierarchy.controller;

import com.platform.hierarchy.dto.ManagerCreateRequest;
import com.platform.hierarchy.dto.StatusUpdateRequest;
import com.platform.hierarchy.model.PlatformSettings;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.service.SuperAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super")
@PreAuthorize("hasRole('SYSTEM_OWNER')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/managers")
    public ResponseEntity<List<User>> getAllManagers() {
        return ResponseEntity.ok(superAdminService.getAllAdmins());
    }

    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(superAdminService.getAllUsers());
    }

    @PostMapping("/managers")
    public ResponseEntity<?> createManager(
            @RequestBody ManagerCreateRequest request,
            Principal principal) {
        try {
            User manager = superAdminService.createManager(principal.getName(), request);
            return ResponseEntity.ok(manager);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/managers/{id}/status")
    public ResponseEntity<Map<String, String>> updateManagerStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Principal principal) {
        try {
            superAdminService.updateAdminStatus(principal.getName(), id, request.getStatus());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Manager account status updated to: " + request.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/settings/balance")
    public ResponseEntity<?> updatePlatformBalance(
            @RequestBody Map<String, Double> payload,
            Principal principal) {
        try {
            Double newBalance = payload.get("platformBalance");
            if (newBalance == null || newBalance < 0) {
                throw new IllegalArgumentException("Invalid platform balance amount.");
            }
            PlatformSettings settings = superAdminService.updateGlobalSettings(principal.getName(), newBalance);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getPlatformReports(Principal principal) {
        return ResponseEntity.ok(superAdminService.getSuperAdminReports(principal.getName()));
    }
}
