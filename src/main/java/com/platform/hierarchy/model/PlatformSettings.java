package com.platform.hierarchy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_settings")
public class PlatformSettings {

    @Id
    private Long id = 1L; // Singleton settings record ID

    @Column(name = "platform_balance", nullable = false)
    private Double platformBalance = 1000000.00;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName = "SBT Crypto";

    @Column(name = "application_name", nullable = false, length = 100)
    private String applicationName = "Customer Satisfaction Portal";

    @Column(nullable = false, length = 255)
    private String motto = "Building Trust Through Transparency, Growth Through Satisfaction";

    public PlatformSettings() {}

    public PlatformSettings(Long id, Double platformBalance, String companyName, String applicationName, String motto) {
        this.id = id;
        this.platformBalance = platformBalance;
        this.companyName = companyName;
        this.applicationName = applicationName;
        this.motto = motto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPlatformBalance() { return platformBalance; }
    public void setPlatformBalance(Double platformBalance) { this.platformBalance = platformBalance; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getMotto() { return motto; }
    public void setMotto(String motto) { this.motto = motto; }
}
