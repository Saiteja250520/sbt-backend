package com.platform.hierarchy.dto;

public class LoginResponse {
    private String token;
    private Long id;
    private String loginId;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private Boolean firstLogin;

    public LoginResponse() {}

    public LoginResponse(String token, Long id, String loginId, String fullName, String email, String role, String status, Boolean firstLogin) {
        this.token = token;
        this.id = id;
        this.loginId = loginId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.firstLogin = firstLogin;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }
}
