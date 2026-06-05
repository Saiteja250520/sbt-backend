package com.platform.hierarchy.dto;

public class InvestmentRequest {
    private Double amount;
    private String planName;

    public InvestmentRequest() {}

    public InvestmentRequest(Double amount, String planName) {
        this.amount = amount;
        this.planName = planName;
    }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
}
