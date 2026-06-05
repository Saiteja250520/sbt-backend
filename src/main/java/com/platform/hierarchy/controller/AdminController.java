package com.platform.hierarchy.controller;

import com.platform.hierarchy.dto.StatusUpdateRequest;
import com.platform.hierarchy.dto.TransferRequest;
import com.platform.hierarchy.model.Deposit;
import com.platform.hierarchy.model.Transfer;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.model.Withdrawal;
import com.platform.hierarchy.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'MANAGER')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAssignedCustomers(Principal principal) {
        return ResponseEntity.ok(adminService.getAssignedUsers(principal.getName()));
    }

    @PutMapping("/customers/{id}/status")
    public ResponseEntity<?> updateCustomerStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Principal principal) {
        try {
            java.util.Map<String, String> creds = adminService.updateUserStatus(principal.getName(), id, request.getStatus());
            java.util.Map<String, Object> response = new HashMap<>();
            response.put("message", "Customer account status updated successfully.");
            if (creds != null) {
                response.put("loginId", creds.get("loginId"));
                response.put("password", creds.get("password"));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            java.util.Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> createAllocation(@RequestBody TransferRequest request, Principal principal) {
        try {
            Transfer transfer = adminService.createTransfer(
                    principal.getName(),
                    request.getUserId(),
                    request.getAmount(),
                    request.getDescription()
            );
            return ResponseEntity.ok(transfer);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/deposits/pending")
    public ResponseEntity<List<Deposit>> getPendingDeposits(Principal principal) {
        return ResponseEntity.ok(adminService.getPendingDeposits(principal.getName()));
    }

    @GetMapping("/withdrawals/pending")
    public ResponseEntity<List<Withdrawal>> getPendingWithdrawals(Principal principal) {
        return ResponseEntity.ok(adminService.getPendingWithdrawals(principal.getName()));
    }

    @PostMapping("/deposits/{id}/approve")
    public ResponseEntity<Map<String, String>> handleDepositApproval(
            @PathVariable Long id,
            @RequestParam String action, // 'APPROVE' or 'REJECT'
            Principal principal) {
        try {
            adminService.handleDepositApproval(principal.getName(), id, action);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Deposit request has been " + action.toLowerCase() + "d.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<Map<String, String>> handleWithdrawalApproval(
            @PathVariable Long id,
            @RequestParam String action, // 'APPROVE' or 'REJECT'
            Principal principal) {
        try {
            adminService.handleWithdrawalApproval(principal.getName(), id, action);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Withdrawal request has been " + action.toLowerCase() + "d.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getManagerReports(Principal principal) {
        return ResponseEntity.ok(adminService.getAdminReports(principal.getName()));
    }
}
