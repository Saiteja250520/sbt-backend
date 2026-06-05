package com.platform.hierarchy.service;

import com.platform.hierarchy.model.*;
import com.platform.hierarchy.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AdminService(UserRepository userRepository, TransferRepository transferRepository,
                        DepositRepository depositRepository, WithdrawalRepository withdrawalRepository,
                        TransactionRepository transactionRepository, InvestmentRepository investmentRepository,
                        PlatformSettingsRepository platformSettingsRepository, NotificationService notificationService,
                        AuditLogService auditLogService, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.depositRepository = depositRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.transactionRepository = transactionRepository;
        this.investmentRepository = investmentRepository;
        this.platformSettingsRepository = platformSettingsRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    private User getManager(String managerLoginId) {
        User manager = userRepository.findByLoginId(managerLoginId)
                .orElseThrow(() -> new IllegalArgumentException("Management Account not found: " + managerLoginId));
        if (!"MANAGER".equalsIgnoreCase(manager.getRole()) && !"SYSTEM_OWNER".equalsIgnoreCase(manager.getRole())) {
            throw new SecurityException("Unauthorized. Must be a Manager or System Owner.");
        }
        return manager;
    }

    @Transactional(readOnly = true)
    public List<User> getAssignedUsers(String managerLoginId) {
        User manager = getManager(managerLoginId);
        
        // System Owner sees all Customers, Manager sees only assigned Customers
        if ("SYSTEM_OWNER".equalsIgnoreCase(manager.getRole())) {
            return userRepository.findByRole("CUSTOMER");
        }
        return userRepository.findByParentAndRole(manager, "CUSTOMER");
    }

    public void updateUserStatus(String managerLoginId, Long userId, String action) {
        User manager = getManager(managerLoginId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer account not found with ID: " + userId));

        // Enforce hierarchy visibility
        if (!"SYSTEM_OWNER".equalsIgnoreCase(manager.getRole()) && 
            (user.getParent() == null || !user.getParent().getId().equals(manager.getId()))) {
            throw new SecurityException("Access Denied. You can only manage your assigned customers.");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            // Step 5: System generates Login ID and Temporary Password
            String generatedLoginId = authService.generateUniqueLoginId();
            String tempPassword = authService.generateTempPassword();

            user.setLoginId(generatedLoginId);
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setStatus("ACTIVE");
            user.setFirstLogin(true); // Enforce password change on first login
            userRepository.save(user);

            // Notify Customer (issuing the login ID and temp password)
            notificationService.sendNotification(
                    user,
                    "Your Customer account has been approved by Manager '" + manager.getFullName() + 
                    "'. Your Login ID is: " + generatedLoginId + " and Temporary Password is: " + tempPassword + 
                    ". Please change your password immediately upon logging in."
            );
            
            auditLogService.log("Customer Approved", manager.getLoginId(), manager.getRole(), user.getId().toString());
            System.out.println("=========================================");
            System.out.println("GENERATED CREDENTIALS FOR APPROVED CLIENT [" + user.getFullName() + "]:");
            System.out.println("Login ID: " + generatedLoginId);
            System.out.println("Password: " + tempPassword);
            System.out.println("=========================================");
        } else if ("REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action)) {
            user.setStatus("REJECTED");
            userRepository.save(user);
            notificationService.sendNotification(
                    user,
                    "Your Customer registration request was rejected by Manager '" + manager.getFullName() + "'."
            );
            auditLogService.log("Customer Rejected", manager.getLoginId(), manager.getRole(), user.getId().toString());
        } else {
            user.setStatus(action.toUpperCase());
            userRepository.save(user);
            auditLogService.log("Customer Status Updated (" + action + ")", manager.getLoginId(), manager.getRole(), user.getId().toString());
        }
    }

    public Transfer createTransfer(String managerLoginId, Long userId, Double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Allocation amount must be greater than zero.");
        }
        User manager = getManager(managerLoginId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + userId));

        // Visibility check
        if (!"SYSTEM_OWNER".equalsIgnoreCase(manager.getRole()) && 
            (user.getParent() == null || !user.getParent().getId().equals(manager.getId()))) {
            throw new SecurityException("Access Denied. You can only allocate funds to your assigned customers.");
        }

        // Generate Transfer Record
        Transfer transfer = new Transfer(manager, user, amount, description);
        transferRepository.save(transfer);

        // Update User Balance
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        // Ledger entry
        Transaction transaction = new Transaction(
                user,
                amount,
                "TRANSFER_IN",
                "Allocated ₹" + amount + " from Manager '" + manager.getFullName() + "'. Note: " + description
        );
        transactionRepository.save(transaction);

        // Notify Customer
        notificationService.sendNotification(
                user,
                "You received a fund allocation of ₹" + amount + " from Manager '" + manager.getFullName() + 
                "'. Your current balance is ₹" + user.getBalance()
        );

        auditLogService.log(
                "Funds Allocated",
                manager.getLoginId(),
                manager.getRole(),
                transfer.getId().toString()
        );

        return transfer;
    }

    public void handleDepositApproval(String managerLoginId, Long depositId, String action) {
        User manager = getManager(managerLoginId);
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit record not found."));

        User user = deposit.getUser();
        if (!"SYSTEM_OWNER".equalsIgnoreCase(manager.getRole()) && 
            (user.getParent() == null || !user.getParent().getId().equals(manager.getId()))) {
            throw new SecurityException("Access Denied. You can only approve deposits for your assigned customers.");
        }

        if (!"PENDING".equalsIgnoreCase(deposit.getStatus())) {
            throw new IllegalStateException("Deposit has already been processed.");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            deposit.setStatus("APPROVED");
            deposit.setApprovedBy(manager);
            depositRepository.save(deposit);

            user.setBalance(user.getBalance() + deposit.getAmount());
            userRepository.save(user);

            Transaction transaction = new Transaction(
                    user,
                    deposit.getAmount(),
                    "DEPOSIT",
                    "Deposit approved by Manager '" + manager.getFullName() + "'"
            );
            transactionRepository.save(transaction);

            notificationService.sendNotification(user, "Your deposit of ₹" + deposit.getAmount() + " has been approved.");
            auditLogService.log("Deposit Approved", manager.getLoginId(), manager.getRole(), deposit.getId().toString());
        } else {
            deposit.setStatus("REJECTED");
            deposit.setApprovedBy(manager);
            depositRepository.save(deposit);

            notificationService.sendNotification(user, "Your deposit of ₹" + deposit.getAmount() + " was rejected.");
            auditLogService.log("Deposit Rejected", manager.getLoginId(), manager.getRole(), deposit.getId().toString());
        }
    }

    public void handleWithdrawalApproval(String managerLoginId, Long withdrawalId, String action) {
        User manager = getManager(managerLoginId);
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal record not found."));

        User user = withdrawal.getUser();
        if (!"SYSTEM_OWNER".equalsIgnoreCase(manager.getRole()) && 
            (user.getParent() == null || !user.getParent().getId().equals(manager.getId()))) {
            throw new SecurityException("Access Denied. You can only approve withdrawals for your assigned customers.");
        }

        if (!"PENDING".equalsIgnoreCase(withdrawal.getStatus())) {
            throw new IllegalStateException("Withdrawal has already been processed.");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            if (user.getBalance() < withdrawal.getAmount()) {
                throw new IllegalStateException("User has insufficient balance to complete this withdrawal.");
            }

            withdrawal.setStatus("APPROVED");
            withdrawal.setApprovedBy(manager);
            withdrawalRepository.save(withdrawal);

            user.setBalance(user.getBalance() - withdrawal.getAmount());
            userRepository.save(user);

            Transaction transaction = new Transaction(
                    user,
                    -withdrawal.getAmount(),
                    "WITHDRAWAL",
                    "Withdrawal approved by Manager '" + manager.getFullName() + "'"
            );
            transactionRepository.save(transaction);

            notificationService.sendNotification(user, "Your withdrawal of ₹" + withdrawal.getAmount() + " has been approved.");
            auditLogService.log("Withdrawal Approved", manager.getLoginId(), manager.getRole(), withdrawal.getId().toString());
        } else {
            withdrawal.setStatus("REJECTED");
            withdrawal.setApprovedBy(manager);
            withdrawalRepository.save(withdrawal);

            notificationService.sendNotification(user, "Your withdrawal of ₹" + withdrawal.getAmount() + " was rejected.");
            auditLogService.log("Withdrawal Rejected", manager.getLoginId(), manager.getRole(), withdrawal.getId().toString());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminReports(String managerLoginId) {
        User manager = getManager(managerLoginId);
        List<User> users = getAssignedUsers(managerLoginId);

        // Summaries
        int totalUsers = users.size();
        double totalBalance = users.stream().mapToDouble(User::getBalance).sum();

        // Deposits
        List<Deposit> deposits = depositRepository.findByUserInOrderByCreatedAtDesc(users);
        double approvedDeposits = deposits.stream()
                .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                .mapToDouble(Deposit::getAmount).sum();

        // Withdrawals
        List<Withdrawal> withdrawals = withdrawalRepository.findByUserInOrderByCreatedAtDesc(users);
        double approvedWithdrawals = withdrawals.stream()
                .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                .mapToDouble(Withdrawal::getAmount).sum();

        // Transfers (allocations)
        List<Transfer> transfers = transferRepository.findByAdminInOrderByCreatedAtDesc(List.of(manager));
        double totalTransferred = transfers.stream().mapToDouble(Transfer::getAmount).sum();

        // Investments
        List<Investment> investments = investmentRepository.findByUserInOrderByCreatedAtDesc(users);
        double totalInvestments = investments.stream().mapToDouble(Investment::getAmount).sum();

        // Global Platform settings (for platform balance lookup)
        PlatformSettings settings = platformSettingsRepository.findById(1L)
                .orElse(new PlatformSettings());

        // Build Report Map
        Map<String, Object> reports = new HashMap<>();
        reports.put("totalUsers", totalUsers);
        reports.put("totalUserBalance", totalBalance);
        reports.put("approvedDeposits", approvedDeposits);
        reports.put("approvedWithdrawals", approvedWithdrawals);
        reports.put("totalTransferred", totalTransferred);
        reports.put("totalInvestments", totalInvestments);
        reports.put("assignedUsers", users);
        reports.put("recentDeposits", deposits.stream().limit(10).collect(Collectors.toList()));
        reports.put("recentWithdrawals", withdrawals.stream().limit(10).collect(Collectors.toList()));
        reports.put("recentTransfers", transfers.stream().limit(10).collect(Collectors.toList()));
        reports.put("platformBalance", settings.getPlatformBalance()); // Shared visibility

        return reports;
    }

    @Transactional(readOnly = true)
    public List<Deposit> getPendingDeposits(String managerLoginId) {
        List<User> users = getAssignedUsers(managerLoginId);
        return depositRepository.findByUserInOrderByCreatedAtDesc(users).stream()
                .filter(d -> "PENDING".equalsIgnoreCase(d.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Withdrawal> getPendingWithdrawals(String managerLoginId) {
        List<User> users = getAssignedUsers(managerLoginId);
        return withdrawalRepository.findByUserInOrderByCreatedAtDesc(users).stream()
                .filter(w -> "PENDING".equalsIgnoreCase(w.getStatus()))
                .collect(Collectors.toList());
    }
}
