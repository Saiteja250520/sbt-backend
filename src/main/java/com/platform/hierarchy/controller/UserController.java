package com.platform.hierarchy.controller;

import com.platform.hierarchy.dto.InvestmentRequest;
import com.platform.hierarchy.dto.TransactionRequest;
import com.platform.hierarchy.model.Deposit;
import com.platform.hierarchy.model.Investment;
import com.platform.hierarchy.model.Transaction;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.model.Withdrawal;
import com.platform.hierarchy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('CUSTOMER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @PostMapping("/deposits")
    public ResponseEntity<?> createDeposit(@RequestBody TransactionRequest request, Principal principal) {
        try {
            Deposit deposit = userService.createDepositRequest(principal.getName(), request.getAmount());
            return ResponseEntity.ok(deposit);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<?> createWithdrawal(@RequestBody TransactionRequest request, Principal principal) {
        try {
            Withdrawal withdrawal = userService.createWithdrawalRequest(principal.getName(), request.getAmount());
            return ResponseEntity.ok(withdrawal);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/investments")
    public ResponseEntity<?> subscribeToInvestment(@RequestBody InvestmentRequest request, Principal principal) {
        try {
            Investment investment = userService.subscribeToInvestment(
                    principal.getName(),
                    request.getAmount(),
                    request.getPlanName()
            );
            return ResponseEntity.ok(investment);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(Principal principal) {
        return ResponseEntity.ok(userService.getTransactionHistory(principal.getName()));
    }

    @GetMapping("/deposits")
    public ResponseEntity<List<Deposit>> getDepositRequests(Principal principal) {
        return ResponseEntity.ok(userService.getDepositRequests(principal.getName()));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<List<Withdrawal>> getWithdrawalRequests(Principal principal) {
        return ResponseEntity.ok(userService.getWithdrawalRequests(principal.getName()));
    }

    @GetMapping("/investments")
    public ResponseEntity<List<Investment>> getInvestments(Principal principal) {
        return ResponseEntity.ok(userService.getInvestments(principal.getName()));
    }
}
