package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;

import com.splitsmart.app.model.ExpenseShare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates that exact amounts sum to exactly the total and returns those shares.
 */
public class ExactSplitCalculator implements SplitCalculator {
    @Override
    public List<ExpenseShare> calculate(
            double totalAmount,
            List<String> participantIds,
            Map<String, Double> exactAmounts,
            Map<String, Double> percentages
    ) {
        require(!participantIds.isEmpty(), "At least one participant is required");
        require(totalAmount > 0, "Amount must be greater than 0");

        long sum = 0;
        for (String id : participantIds) {
            require(exactAmounts.containsKey(id), "Missing exact amount for participant " + id);
            double amount = exactAmounts.get(id);
            require(amount >= 0, "Exact amounts must be non-negative");
            require(MoneyUtils.hasAtMostTwoDecimals(amount), "Exact amounts must have at most 2 decimals");
            sum += MoneyUtils.toCents(amount);
        }

        long totalCents = MoneyUtils.toCents(totalAmount);
        require(sum == totalCents,
                "Exact amounts must add up to the total. They add up to "
                        + String.format("$%.2f", MoneyUtils.fromCents(sum))
                        + " but the total is " + String.format("$%.2f", totalAmount));

        List<ExpenseShare> shares = new ArrayList<>();
        for (String id : participantIds) {
            shares.add(new ExpenseShare(id, MoneyUtils.roundToCents(exactAmounts.get(id))));
        }
        return shares;
    }
}
