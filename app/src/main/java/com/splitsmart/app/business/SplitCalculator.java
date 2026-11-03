package com.splitsmart.app.business;

import com.splitsmart.app.model.ExpenseShare;

import java.util.List;
import java.util.Map;

public interface SplitCalculator {
    /**
     * @param totalAmount    expense total (dollars with up to 2 decimals)
     * @param participantIds ordered list of participants
     * @param exactAmounts   map of userId -> exact dollar amount (used by EXACT; may be empty)
     * @param percentages    map of userId -> percentage (used by PERCENTAGE; may be empty)
     * @return list of ExpenseShare whose amounts sum to totalAmount
     */
    List<ExpenseShare> calculate(
            double totalAmount,
            List<String> participantIds,
            Map<String, Double> exactAmounts,
            Map<String, Double> percentages
    );
}
