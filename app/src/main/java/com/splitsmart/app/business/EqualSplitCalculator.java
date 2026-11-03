package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;

import com.splitsmart.app.model.ExpenseShare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Divides evenly to cents; leftover cents go to the first participants so the sum equals total.
 */
public class EqualSplitCalculator implements SplitCalculator {
    @Override
    public List<ExpenseShare> calculate(
            double totalAmount,
            List<String> participantIds,
            Map<String, Double> exactAmounts,
            Map<String, Double> percentages
    ) {
        require(!participantIds.isEmpty(), "At least one participant is required");
        require(totalAmount > 0, "Amount must be greater than 0");

        long totalCents = MoneyUtils.toCents(totalAmount);
        int n = participantIds.size();
        long base = totalCents / n;
        long remainder = totalCents % n;

        List<ExpenseShare> shares = new ArrayList<>();
        for (int index = 0; index < n; index++) {
            long cents = base + (index < remainder ? 1 : 0);
            shares.add(new ExpenseShare(participantIds.get(index), MoneyUtils.fromCents(cents)));
        }
        return shares;
    }
}
