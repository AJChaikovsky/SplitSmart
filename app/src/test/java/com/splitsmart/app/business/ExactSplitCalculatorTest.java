package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;

import com.splitsmart.app.model.ExpenseShare;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExactSplitCalculatorTest {
    private final ExactSplitCalculator calculator = new ExactSplitCalculator();

    private List<ExpenseShare> split(double total, List<String> participants, Map<String, Double> exact) {
        return calculator.calculate(total, participants, exact, Collections.emptyMap());
    }

    private static Map<String, Double> amounts(Object... pairs) {
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (Double) pairs[i + 1]);
        return map;
    }

    @Test
    public void acceptsAmountsThatSumToTotal() {
        List<ExpenseShare> shares = split(50.0, Arrays.asList("a", "b"), amounts("a", 20.0, "b", 30.0));
        assertEquals(20.0, shares.get(0).getAmount(), 0.0);
        assertEquals(30.0, shares.get(1).getAmount(), 0.0);
    }

    @Test
    public void acceptsWithinOneCentTolerance() {
        List<ExpenseShare> shares = split(10.00, Arrays.asList("a", "b"), amounts("a", 5.00, "b", 5.00));
        assertEquals(10.0, shares.get(0).getAmount() + shares.get(1).getAmount(), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWhenSumDoesNotMatch() {
        split(50.0, Arrays.asList("a", "b"), amounts("a", 10.0, "b", 10.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingParticipantAmount() {
        split(10.0, Arrays.asList("a", "b"), amounts("a", 10.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTotalOffByOneCent() {
        split(30.0, Arrays.asList("a", "b", "c"), amounts("a", 5.0, "b", 10.0, "c", 14.99));
    }

    @Test
    public void allowsZeroShare() {
        List<ExpenseShare> shares = split(30.0, Arrays.asList("a", "b", "c"), amounts("a", 0.0, "b", 15.0, "c", 15.0));
        assertEquals(0.0, shares.get(0).getAmount(), 0.0);
    }
}
