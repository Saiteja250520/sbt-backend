package com.platform.hierarchy.dto;

public class TransferRequest {
    private Long userId;
    private Double amount;
    private String description;

    public TransferRequest() {}

    public TransferRequest(Long userId, Double amount, String description) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
