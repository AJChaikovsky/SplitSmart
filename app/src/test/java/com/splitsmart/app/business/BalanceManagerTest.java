package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.GroupBalance;
import com.splitsmart.app.model.Settlement;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BalanceManagerTest {
    private final BalanceManager manager = new BalanceManager();

    private static Expense expense(String id, String payerId, double amount, ExpenseShare... shares) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setGroupId("g1");
        expense.setPayerId(payerId);
        expense.setAmount(amount);
        List<ExpenseShare> shareList = Arrays.asList(shares);
        expense.setShares(shareList);
        for (ExpenseShare share : shareList) expense.getParticipantIds().add(share.getUserId());
        return expense;
    }

    private static boolean hasDebt(List<BalanceManager.PairBalance> balances, String from, String to, Double amount) {
        for (BalanceManager.PairBalance balance : balances) {
            if (balance.getFromUserId().equals(from) && balance.getToUserId().equals(to)
                    && (amount == null || balance.getAmount() == amount)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void expenseCreatesPairwiseDebtFromNonPayersToPayer() {
        Expense dinner = expense("e1", "alice", 30.0,
                new ExpenseShare("alice", 10.0),
                new ExpenseShare("bob", 10.0),
                new ExpenseShare("carol", 10.0));
        List<BalanceManager.PairBalance> balances =
                manager.computePairwiseBalances(Collections.singletonList(dinner), Collections.emptyList());
        assertEquals(2, balances.size());
        assertTrue(hasDebt(balances, "bob", "alice", 10.0));
        assertTrue(hasDebt(balances, "carol", "alice", 10.0));
    }

    @Test
    public void settlementReducesOwedAmount() {
        Expense taxi = expense("e1", "alice", 20.0,
                new ExpenseShare("alice", 10.0),
                new ExpenseShare("bob", 10.0));
        Settlement settlement = new Settlement();
        settlement.setId("s1");
        settlement.setGroupId("g1");
        settlement.setFromUserId("bob");
        settlement.setToUserId("alice");
        settlement.setAmount(4.0);

        List<BalanceManager.PairBalance> balances = manager.computePairwiseBalances(
                Collections.singletonList(taxi), Collections.singletonList(settlement));
        assertEquals(1, balances.size());
        assertEquals("bob", balances.get(0).getFromUserId());
        assertEquals("alice", balances.get(0).getToUserId());
        assertEquals(6.0, balances.get(0).getAmount(), 0.0);
    }

    @Test
    public void netsPairwiseWithoutSimplifyingTriples() {
        // alice paid for bob ($5) and carol paid for bob ($5) — bob owes both, no triple simplification
        Expense e1 = expense("e1", "alice", 5.0, new ExpenseShare("alice", 0.0), new ExpenseShare("bob", 5.0));
        Expense e2 = expense("e2", "carol", 5.0, new ExpenseShare("carol", 0.0), new ExpenseShare("bob", 5.0));
        List<BalanceManager.PairBalance> balances =
                manager.computePairwiseBalances(Arrays.asList(e1, e2), Collections.emptyList());
        assertEquals(2, balances.size());
        assertTrue(hasDebt(balances, "bob", "alice", null));
        assertTrue(hasDebt(balances, "bob", "carol", null));
    }

    @Test
    public void totalToPayAndReceiveAreSeparate() {
        Expense e1 = expense("e1", "alice", 20.0, new ExpenseShare("alice", 10.0), new ExpenseShare("bob", 10.0));
        Expense e2 = expense("e2", "bob", 6.0, new ExpenseShare("alice", 3.0), new ExpenseShare("bob", 3.0));
        List<BalanceManager.PairBalance> balances =
                manager.computePairwiseBalances(Arrays.asList(e1, e2), Collections.emptyList());
        BalanceManager.UserBalanceSummary aliceSummary = manager.summarizeForUser("alice", balances);
        // bob owes alice 10, alice owes bob 3 => net bob owes alice 7
        // After pairwise netting there should be one debt
        assertEquals(1, balances.size());
        assertEquals(0.0, aliceSummary.getTotalToPay(), 0.0);
        assertEquals(7.0, aliceSummary.getTotalToReceive(), 0.0);

        BalanceManager.UserBalanceSummary bobSummary = manager.summarizeForUser("bob", balances);
        assertEquals(7.0, bobSummary.getTotalToPay(), 0.0);
        assertEquals(0.0, bobSummary.getTotalToReceive(), 0.0);
    }

    @Test
    public void summarizesOneGroupForUser() {
        Group group = new Group("g1", "Roommates", "", "alice",
                Arrays.asList("a@usc.edu", "b@usc.edu", "c@usc.edu"), Arrays.asList("alice", "bob", "carol"), 0L);
        // alice paid $30 for three people; bob paid $9 split with alice
        Expense rent = expense("e1", "alice", 30.0,
                new ExpenseShare("alice", 10.0), new ExpenseShare("bob", 10.0), new ExpenseShare("carol", 10.0));
        Expense snacks = expense("e2", "bob", 9.0, new ExpenseShare("alice", 4.5), new ExpenseShare("bob", 4.5));

        GroupBalance alice = manager.summarizeGroupForUser(group, "alice",
                Arrays.asList(rent, snacks), Collections.emptyList());
        assertEquals("Roommates", alice.getGroupName());
        assertEquals(0.0, alice.getToPay(), 0.0);
        assertEquals(15.5, alice.getToReceive(), 0.0); // bob nets 5.50, carol 10.00

        GroupBalance carol = manager.summarizeGroupForUser(group, "carol",
                Arrays.asList(rent, snacks), Collections.emptyList());
        assertEquals(10.0, carol.getToPay(), 0.0);
        assertEquals(0.0, carol.getToReceive(), 0.0);
        assertTrue(!carol.isSettled());
    }
}
