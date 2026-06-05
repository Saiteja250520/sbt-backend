package com.platform.hierarchy.service;

import com.platform.hierarchy.model.*;
import com.platform.hierarchy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, DepositRepository depositRepository,
                       WithdrawalRepository withdrawalRepository, InvestmentRepository investmentRepository,
                       TransactionRepository transactionRepository, NotificationService notificationService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public User getProfile(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + loginId));
    }

    public Deposit createDepositRequest(String loginId, Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        User user = getProfile(loginId);
        
        Deposit deposit = new Deposit(user, amount);
        depositRepository.save(deposit);

        // Notify Manager
        if (user.getParent() != null) {
            notificationService.sendNotification(
                    user.getParent(),
                    "Customer '" + user.getFullName() + "' requested a deposit of ₹" + amount
            );
        }

        auditLogService.log("Deposit Requested", user.getLoginId(), user.getRole(), deposit.getId().toString());
        return deposit;
    }

    public Withdrawal createWithdrawalRequest(String loginId, Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        User user = getProfile(loginId);
        
        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance to request withdrawal.");
        }

        Withdrawal withdrawal = new Withdrawal(user, amount);
        withdrawalRepository.save(withdrawal);

        // Notify Manager
        if (user.getParent() != null) {
            notificationService.sendNotification(
                    user.getParent(),
                    "Customer '" + user.getFullName() + "' requested a withdrawal of ₹" + amount
            );
        }

        auditLogService.log("Withdrawal Requested", user.getLoginId(), user.getRole(), withdrawal.getId().toString());
        return withdrawal;
    }

    public Investment subscribeToInvestment(String loginId, Double amount, String planName) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Investment amount must be greater than zero.");
        }
        User user = getProfile(loginId);

        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance to subscribe to this investment plan.");
        }

        // Deduct balance immediately
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        // Save Investment record
        Investment investment = new Investment(user, amount, planName);
        investmentRepository.save(investment);

        // Transaction history log
        Transaction transaction = new Transaction(
                user,
                -amount,
                "INVESTMENT",
                "Subscribed to investment plan: " + planName
        );
        transactionRepository.save(transaction);

        // Notify Customer
        notificationService.sendNotification(
                user,
                "Successfully subscribed to plan: " + planName + " for ₹" + amount + ". Your new balance is ₹" + user.getBalance()
        );

        auditLogService.log(
                "Investment Created",
                user.getLoginId(),
                user.getRole(),
                investment.getId().toString()
        );

        return investment;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String loginId) {
        User user = getProfile(loginId);
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Deposit> getDepositRequests(String loginId) {
        User user = getProfile(loginId);
        return depositRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Withdrawal> getWithdrawalRequests(String loginId) {
        User user = getProfile(loginId);
        return withdrawalRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Investment> getInvestments(String loginId) {
        User user = getProfile(loginId);
        return investmentRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
