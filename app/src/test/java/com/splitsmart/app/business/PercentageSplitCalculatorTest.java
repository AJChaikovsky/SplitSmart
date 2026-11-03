package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;

import com.splitsmart.app.model.ExpenseShare;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentageSplitCalculatorTest {
    private final PercentageSplitCalculator calculator = new PercentageSplitCalculator();

    private List<ExpenseShare> split(double total, List<String> participants, Map<String, Double> percentages) {
        return calculator.calculate(total, participants, Collections.emptyMap(), percentages);
    }

    private static Map<String, Double> percents(Object... pairs) {
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (Double) pairs[i + 1]);
        return map;
    }

    private static double sum(List<ExpenseShare> shares) {
        double total = 0.0;
        for (ExpenseShare share : shares) total += share.getAmount();
        return total;
    }

    @Test
    public void convertsPercentagesWithCentRounding() {
        List<ExpenseShare> shares = split(100.0, Arrays.asList("a", "b"), percents("a", 50.0, "b", 50.0));
        assertEquals(50.0, shares.get(0).getAmount(), 0.0);
        assertEquals(50.0, shares.get(1).getAmount(), 0.0);
        assertEquals(100.0, sum(shares), 0.0);
    }

    @Test
    public void leftoverCentsGoToFirstParticipants() {
        // 33.33% + 33.33% + 33.34% of $10.00
        List<ExpenseShare> shares = split(10.0, Arrays.asList("a", "b", "c"),
                percents("a", 33.33, "b", 33.33, "c", 33.34));
        assertEquals(10.0, sum(shares), 0.0);
        assertEquals(3, shares.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPercentagesNot100() {
        split(10.0, Arrays.asList("a", "b"), percents("a", 40.0, "b", 40.0));
    }
}
