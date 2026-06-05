package com.platform.hierarchy.dto;

public class RegisterRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String notes;
    private Long parentId; // Selected Manager ID

    public RegisterRequest() {}

    public RegisterRequest(String fullName, String email, String phoneNumber, String address, String notes, Long parentId) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.notes = notes;
        this.parentId = parentId;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
