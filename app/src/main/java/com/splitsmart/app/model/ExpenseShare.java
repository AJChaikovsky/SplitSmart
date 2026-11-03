package com.splitsmart.app.model;

import java.util.HashMap;
import java.util.Map;

public class ExpenseShare {
    private String userId = "";
    private double amount = 0.0;
    private Double percentage = null;

    public ExpenseShare() {
    }

    public ExpenseShare(String userId, double amount) {
        this(userId, amount, null);
    }

    public ExpenseShare(String userId, double amount, Double percentage) {
        this.userId = userId;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("amount", amount);
        map.put("percentage", percentage);
        return map;
    }

    public static ExpenseShare fromMap(Map<String, Object> map) {
        return new ExpenseShare(
                MapReader.str(map, "userId", ""),
                MapReader.dbl(map, "amount", 0.0),
                MapReader.nullableDbl(map, "percentage")
        );
    }
}
