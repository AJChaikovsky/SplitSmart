package com.splitsmart.app.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MonthlySummary {
    private final int year;
    private final int month;
    private final int expenseCount;
    private final double totalSpending;
    private final Map<String, Double> categoryTotals;
    /** Display name -> amount paid this month. */
    private final Map<String, Double> payerTotals;
    private final List<String> payerInsights;

    /** An empty summary for a month with no expenses. */
    public MonthlySummary(int year, int month) {
        this(year, month, 0, 0.0, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
    }

    public MonthlySummary(int year, int month, int expenseCount, double totalSpending,
                          Map<String, Double> categoryTotals, Map<String, Double> payerTotals,
                          List<String> payerInsights) {
        this.year = year;
        this.month = month;
        this.expenseCount = expenseCount;
        this.totalSpending = totalSpending;
        this.categoryTotals = categoryTotals;
        this.payerTotals = payerTotals;
        this.payerInsights = payerInsights;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getExpenseCount() { return expenseCount; }
    public double getTotalSpending() { return totalSpending; }
    public Map<String, Double> getCategoryTotals() { return categoryTotals; }
    public Map<String, Double> getPayerTotals() { return payerTotals; }
    public List<String> getPayerInsights() { return payerInsights; }

    public boolean isEmpty() {
        return expenseCount == 0;
    }
}
