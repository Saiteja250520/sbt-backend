package com.platform.hierarchy.dto;

public class ManagerCreateRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String region;
    private String notes;

    public ManagerCreateRequest() {}

    public ManagerCreateRequest(String fullName, String email, String phoneNumber, String region, String notes) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.region = region;
        this.notes = notes;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
