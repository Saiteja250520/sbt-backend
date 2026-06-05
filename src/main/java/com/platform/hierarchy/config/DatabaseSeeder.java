package com.platform.hierarchy.config;

import com.platform.hierarchy.model.*;
import com.platform.hierarchy.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, DepositRepository depositRepository,
                          WithdrawalRepository withdrawalRepository, TransactionRepository transactionRepository,
                          PlatformSettingsRepository platformSettingsRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.transactionRepository = transactionRepository;
        this.platformSettingsRepository = platformSettingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed Platform Settings first
        if (!platformSettingsRepository.existsById(1L)) {
            System.out.println("Seeding global Platform Settings...");
            PlatformSettings settings = new PlatformSettings();
            settings.setId(1L);
            settings.setPlatformBalance(1000000.00); // ₹1,000,000
            settings.setCompanyName("SBT Crypto");
            settings.setApplicationName("Customer Satisfaction Portal");
            settings.setMotto("Building Trust Through Transparency, Growth Through Satisfaction");
            platformSettingsRepository.save(settings);
        }

        if (userRepository.existsByLoginId("976543210")) {
            System.out.println("Database already seeded with SBT Crypto users.");
            return;
        }

        System.out.println("Seeding database with SBT Crypto Portal default data...");

        // 1. Seed Default System Owner Account
        User owner = new User();
        owner.setLoginId("976543210");
        owner.setPassword(passwordEncoder.encode("123456"));
        owner.setEmail("owner@sbtcrypto.com");
        owner.setFullName("System Owner");
        owner.setRole("SYSTEM_OWNER");
        owner.setStatus("ACTIVE");
        owner.setFirstLogin(true); // Mandatory setup on first login
        userRepository.save(owner);

        // 2. Seed an Active Manager Account
        User manager1 = new User();
        manager1.setLoginId("876543210");
        manager1.setPassword(passwordEncoder.encode("123456"));
        manager1.setEmail("manager1@sbtcrypto.com");
        manager1.setFullName("Manager One");
        manager1.setPhoneNumber("+919876543210");
        manager1.setRegion("East Region");
        manager1.setRole("MANAGER");
        manager1.setStatus("ACTIVE");
        manager1.setParent(owner);
        manager1.setFirstLogin(true);
        userRepository.save(manager1);

        // 3. Seed an Active Customer Account assigned to manager1
        User customer1 = new User();
        customer1.setLoginId("776543210");
        customer1.setPassword(passwordEncoder.encode("123456"));
        customer1.setEmail("customer1@gmail.com");
        customer1.setFullName("Customer One");
        customer1.setPhoneNumber("+919876543211");
        customer1.setRegion("East Region");
        customer1.setRole("CUSTOMER");
        customer1.setStatus("ACTIVE");
        customer1.setParent(manager1);
        customer1.setBalance(5000.00);
        customer1.setFirstLogin(true);
        userRepository.save(customer1);

        // 4. Seed a Pending Customer Account
        User pendingCustomer = new User();
        pendingCustomer.setLoginId("pendingcustomer@gmail.com"); // Placeholder loginId until approved
        pendingCustomer.setPassword(passwordEncoder.encode("TemporaryPass@123"));
        pendingCustomer.setEmail("pendingcustomer@gmail.com");
        pendingCustomer.setFullName("Pending Client");
        pendingCustomer.setPhoneNumber("+919876543212");
        pendingCustomer.setRegion("East Region");
        pendingCustomer.setNotes("Requesting priority manager group assignment.");
        pendingCustomer.setRole("CUSTOMER");
        pendingCustomer.setStatus("PENDING_APPROVAL");
        pendingCustomer.setParent(manager1);
        pendingCustomer.setFirstLogin(true);
        userRepository.save(pendingCustomer);

        // 5. Seed Initial Ledger Transactions for customer1
        Transaction initialDeposit = new Transaction(
                customer1,
                5000.00,
                "DEPOSIT",
                "Initial customer account activation funding"
        );
        transactionRepository.save(initialDeposit);

        // 6. Seed Pending Approvals for manager1
        Deposit pendingDeposit = new Deposit(customer1, 2500.00);
        depositRepository.save(pendingDeposit);

        Withdrawal pendingWithdrawal = new Withdrawal(customer1, 500.00);
        withdrawalRepository.save(pendingWithdrawal);

        System.out.println("SBT Crypto Database Seeding completed successfully.");
    }
}
