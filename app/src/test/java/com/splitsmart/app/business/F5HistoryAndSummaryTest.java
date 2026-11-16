package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.MonthlySummary;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.Transaction;
import com.splitsmart.app.model.TransactionType;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Demo checklist, Feature 5: history, search, filters and monthly summaries. */
public class F5HistoryAndSummaryTest {
    private final TestBackend app = new TestBackend();
    private final User alice = app.user("Alice", "1000000001");
    private final User bob = app.user("Bob", "1000000002");
    private final User carol = app.user("Carol", "1000000003");
    private final Group monthly = app.group("Monthly", alice, bob, carol);
    private final Expense chipotle = app.expense(monthly, alice, "Chipotle dinner", 30.0, "2026-11-02",
            ExpenseCategory.FOOD, "late night", alice, alice, bob);
    private final Expense uber = app.expense(monthly, alice, "Uber to LAX", 20.0, "2026-11-04",
            ExpenseCategory.TRANSPORTATION, "airport", bob, alice, bob);
    private final Expense rent = app.expense(monthly, alice, "October rent", 100.0, "2026-10-15",
            ExpenseCategory.HOUSING, "rent", alice, alice, carol);

    private void venmoSettlement() {
        app.signIn(bob);
        app.settlementManager.createSettlement(monthly.getId(), bob.getUid(), alice.getUid(), 5.0,
                "2026-11-06", "venmo for dinner");
    }

    private List<Transaction> history(HistoryManager.HistoryFilter filter) {
        return app.history.getHistory(monthly.getId(), filter);
    }

    private static List<String> titles(List<Transaction> transactions) {
        List<String> titles = new ArrayList<>();
        for (Transaction tx : transactions) titles.add(tx.getTitle());
        return titles;
    }

    private static HistoryManager.HistoryFilter keyword(String text) {
        return new HistoryManager.HistoryFilter(text, null, null, null, HistoryManager.SortOrder.NEWEST);
    }

    // --- Complete group history ---

    @Test
    public void historyMixesExpensesAndSettlementsNewestFirst() {
        venmoSettlement();
        List<Transaction> all = history(new HistoryManager.HistoryFilter());
        assertEquals(Arrays.asList("Settlement", "Uber to LAX", "Chipotle dinner", "October rent"), titles(all));

        Transaction settlement = all.get(0);
        assertEquals(TransactionType.SETTLEMENT, settlement.getType());
        assertEquals(5.0, settlement.getAmount(), 0.0);
        assertEquals("2026-11-06", settlement.getDate());
        assertEquals(TestBackend.ids(bob, alice), settlement.getMemberIds());
        assertEquals(TestBackend.ids(alice, bob), all.get(2).getMemberIds());
    }

    @Test
    public void memberWhoDidNotParticipateCanStillViewTheExpense() {
        app.signIn(carol);
        assertEquals("Chipotle dinner", app.expenseManager.getExpense(chipotle.getId()).getTitle());
        assertTrue(titles(history(new HistoryManager.HistoryFilter())).contains("Chipotle dinner"));
    }

    // --- Search, filter, sort ---

    @Test
    public void searchMatchesTitlesExpenseNotesAndSettlementNotes() {
        venmoSettlement();
        assertEquals(Collections.singletonList("Chipotle dinner"), titles(history(keyword("Chipotle"))));
        assertEquals(Collections.singletonList("Uber to LAX"), titles(history(keyword("airport"))));
        assertEquals(Collections.singletonList("Settlement"), titles(history(keyword("venmo"))));
        assertTrue(history(keyword("sushi")).isEmpty());
    }

    @Test
    public void searchIgnoresCapitalization() {
        // Current design: keyword matching is case-insensitive. Confirm with the TA before the demo.
        assertEquals(Collections.singletonList("Chipotle dinner"), titles(history(keyword("CHIPOTLE"))));
    }

    @Test
    public void filtersByTypeCategoryAndInvolvedMember() {
        venmoSettlement();
        assertEquals(Collections.singletonList("Settlement"), titles(history(new HistoryManager.HistoryFilter(
                null, TransactionType.SETTLEMENT, null, null, HistoryManager.SortOrder.NEWEST))));
        assertEquals(Collections.singletonList("Chipotle dinner"), titles(history(new HistoryManager.HistoryFilter(
                null, null, ExpenseCategory.FOOD, null, HistoryManager.SortOrder.NEWEST))));
        assertEquals(Collections.singletonList("October rent"), titles(history(new HistoryManager.HistoryFilter(
                null, null, null, carol.getUid(), HistoryManager.SortOrder.NEWEST))));
        assertEquals("Settlement payer/recipient count as involved",
                Arrays.asList("Settlement", "Uber to LAX", "Chipotle dinner"),
                titles(history(new HistoryManager.HistoryFilter(null, null, null, bob.getUid(),
                        HistoryManager.SortOrder.NEWEST))));
        // Filters compose with AND: a matching member cannot override the requested category.
        assertEquals(Collections.singletonList("Chipotle dinner"), titles(history(new HistoryManager.HistoryFilter(
                null, TransactionType.EXPENSE, ExpenseCategory.FOOD, bob.getUid(), HistoryManager.SortOrder.NEWEST))));
        assertTrue(history(new HistoryManager.HistoryFilter(
                null, TransactionType.EXPENSE, ExpenseCategory.FOOD, carol.getUid(), HistoryManager.SortOrder.NEWEST)).isEmpty());
    }

    @Test
    public void sortsOldestFirstWhenAsked() {
        venmoSettlement();
        assertEquals(Arrays.asList("October rent", "Chipotle dinner", "Uber to LAX", "Settlement"),
                titles(history(new HistoryManager.HistoryFilter(null, null, null, null, HistoryManager.SortOrder.OLDEST))));
    }

    // --- Monthly summaries ---

    @Test
    public void monthlySummaryCountsExpensesAndExcludesSettlements() {
        venmoSettlement();
        MonthlySummary november = app.summary.getMonthlySummary(monthly.getId(), 2026, 11);
        assertEquals(2, november.getExpenseCount());
        assertEquals(50.0, november.getTotalSpending(), 0.0);
        assertEquals(30.0, november.getCategoryTotals().get("FOOD"), 0.0);
        assertEquals(20.0, november.getCategoryTotals().get("TRANSPORTATION"), 0.0);
        double categorySum = 0;
        for (double total : november.getCategoryTotals().values()) categorySum += total;
        assertEquals(50.0, categorySum, 0.0);
    }

    @Test
    public void changingTheMonthShowsThatMonthsExpenses() {
        MonthlySummary october = app.summary.getMonthlySummary(monthly.getId(), 2026, 10);
        assertEquals(1, october.getExpenseCount());
        assertEquals(100.0, october.getCategoryTotals().get("HOUSING"), 0.0);
        assertTrue(app.summary.getMonthlySummary(monthly.getId(), 2026, 9).isEmpty());
    }

    @Test
    public void editedExpenseStaysInItsOriginalMonth() {
        app.signIn(alice);
        app.expenseManager.editExpense(uber.getId(), "Uber to LAX", 25.0, ExpenseCategory.TRANSPORTATION,
                bob.getUid(), TestBackend.ids(alice, bob), SplitMethod.EQUAL, "airport",
                Collections.emptyMap(), Collections.emptyMap());

        assertEquals(55.0, app.summary.getMonthlySummary(monthly.getId(), 2026, 11).getTotalSpending(), 0.0);
        assertEquals(100.0, app.summary.getMonthlySummary(monthly.getId(), 2026, 10).getTotalSpending(), 0.0);
    }

    @Test
    public void editingLastMonthsExpenseKeepsItInLastMonth() {
        app.signIn(alice);
        app.expenseManager.editExpense(rent.getId(), "October rent", 120.0, ExpenseCategory.HOUSING,
                alice.getUid(), TestBackend.ids(alice, carol), SplitMethod.EQUAL, "rent",
                Collections.emptyMap(), Collections.emptyMap());

        MonthlySummary october = app.summary.getMonthlySummary(monthly.getId(), 2026, 10);
        assertEquals(1, october.getExpenseCount());
        assertEquals(120.0, october.getTotalSpending(), 0.0);
        assertEquals("November is unchanged", 50.0,
                app.summary.getMonthlySummary(monthly.getId(), 2026, 11).getTotalSpending(), 0.0);
    }

    @Test
    public void deletingAnExpenseUpdatesTheSummary() {
        app.signIn(alice);
        app.expenseManager.deleteExpense(chipotle.getId());
        MonthlySummary november = app.summary.getMonthlySummary(monthly.getId(), 2026, 11);
        assertEquals(1, november.getExpenseCount());
        assertEquals(20.0, november.getTotalSpending(), 0.0);
        assertEquals(rent.getDate(), "2026-10-15");
    }
}
