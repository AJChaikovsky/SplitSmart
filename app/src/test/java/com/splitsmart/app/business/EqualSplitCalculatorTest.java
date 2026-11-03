package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;

import com.splitsmart.app.model.ExpenseShare;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EqualSplitCalculatorTest {
    private final EqualSplitCalculator calculator = new EqualSplitCalculator();

    private List<ExpenseShare> split(double total, List<String> participants) {
        return calculator.calculate(total, participants, Collections.emptyMap(), Collections.emptyMap());
    }

    private static double sum(List<ExpenseShare> shares) {
        double total = 0.0;
        for (ExpenseShare share : shares) total += share.getAmount();
        return total;
    }

    @Test
    public void dividesEvenlyWhenCentsDivideCleanly() {
        List<ExpenseShare> shares = split(30.0, Arrays.asList("a", "b", "c"));
        assertEquals(3, shares.size());
        for (ExpenseShare share : shares) assertEquals(10.0, share.getAmount(), 0.0);
        assertEquals(30.0, sum(shares), 0.0);
    }

    @Test
    public void assignsLeftoverCentsToFirstParticipants() {
        // $10.00 / 3 = 333, 333, 334 cents -> first gets leftover
        List<ExpenseShare> shares = split(10.0, Arrays.asList("a", "b", "c"));
        assertEquals(3.34, shares.get(0).getAmount(), 0.0);
        assertEquals(3.33, shares.get(1).getAmount(), 0.0);
        assertEquals(3.33, shares.get(2).getAmount(), 0.0);
        assertEquals(10.0, sum(shares), 0.0);
    }

    @Test
    public void twoParticipantsOddCent() {
        List<ExpenseShare> shares = split(0.01, Arrays.asList("a", "b"));
        assertEquals(0.01, sum(shares), 0.0);
        assertEquals(0.01, shares.get(0).getAmount(), 0.0);
        assertEquals(0.0, shares.get(1).getAmount(), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyParticipants() {
        split(10.0, Collections.emptyList());
    }
}
