package com.platform.hierarchy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false, length = 50)
    private String loginId; // Replaces username

    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String region;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 20)
    private String role; // 'SYSTEM_OWNER', 'MANAGER', 'CUSTOMER'

    @Column(nullable = false, length = 20)
    private String status; // 'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'REJECTED', 'BLOCKED', 'INACTIVE'

    @Column(name = "first_login", nullable = false)
    private Boolean firstLogin = true; // Forcing setup on first login

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private User parent; // Hierarchical parent (Manager -> Owner, Customer -> Manager)

    @Column(nullable = false)
    private Double balance = 0.00;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Default Constructor
    public User() {}

    // Parameterized Constructor
    public User(String loginId, String password, String email, String fullName, String role, String status, User parent) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.parent = parent;
        this.balance = 0.00;
        this.firstLogin = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }

    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
