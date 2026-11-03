package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;

import com.splitsmart.app.model.ExpenseShare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates percentages sum to 100%, converts to amounts with cent rounding so sum equals total.
 * Leftover cents from rounding are assigned to the first participants.
 */
public class PercentageSplitCalculator implements SplitCalculator {
    @Override
    public List<ExpenseShare> calculate(
            double totalAmount,
            List<String> participantIds,
            Map<String, Double> exactAmounts,
            Map<String, Double> percentages
    ) {
        require(!participantIds.isEmpty(), "At least one participant is required");
        require(totalAmount > 0, "Amount must be greater than 0");

        double pctSum = 0.0;
        for (String id : participantIds) {
            require(percentages.containsKey(id), "Missing percentage for participant " + id);
            double pct = percentages.get(id);
            require(pct >= 0, "Percentages must be non-negative");
            pctSum += pct;
        }
        require(Math.abs(pctSum - 100.0) < 0.01, "Percentages must sum to 100% (got " + pctSum + ")");

        long totalCents = MoneyUtils.toCents(totalAmount);
        long[] rawCents = new long[participantIds.size()];
        long assigned = 0;
        for (int i = 0; i < rawCents.length; i++) {
            rawCents[i] = (long) Math.floor(totalCents * percentages.get(participantIds.get(i)) / 100.0);
            assigned += rawCents[i];
        }

        long leftover = totalCents - assigned;
        for (int i = 0; leftover > 0 && i < rawCents.length; i++) {
            rawCents[i]++;
            leftover--;
        }

        List<ExpenseShare> shares = new ArrayList<>();
        for (int i = 0; i < rawCents.length; i++) {
            String id = participantIds.get(i);
            shares.add(new ExpenseShare(id, MoneyUtils.fromCents(rawCents[i]), percentages.get(id)));
        }
        return shares;
    }
}
