package com.platform.hierarchy.dto;

public class FirstLoginSetupRequest {
    private String password; // Mandatory new password
    private String loginId;  // Optional new login ID (mostly System Owner)
    private String fullName;
    private String phoneNumber;
    private String region;

    // Platform settings configuration (only for System Owner role)
    private Double platformBalance;
    private String companyName;
    private String applicationName;
    private String motto;

    public FirstLoginSetupRequest() {}

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Double getPlatformBalance() { return platformBalance; }
    public void setPlatformBalance(Double platformBalance) { this.platformBalance = platformBalance; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getMotto() { return motto; }
    public void setMotto(String motto) { this.motto = motto; }
}
