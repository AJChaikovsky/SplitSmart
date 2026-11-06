package com.splitsmart.app.business;

import static com.splitsmart.app.business.F1UserAndGroupManagementTest.assertRejected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.Settlement;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Demo checklist, Feature 4: balance calculation, display and settlement updates. */
public class F4BalanceAndSettlementTest {
    private final TestBackend app = new TestBackend();
    private final User alice = app.user("Alice", "1000000001");
    private final User bob = app.user("Bob", "1000000002");
    private final User carol = app.user("Carol", "1000000003");
    private final User david = app.user("David", "1000000004");

    /** Offset group: Alice owes Bob $10 + $5, Bob owes Alice $4, so Alice owes Bob $11. */
    private Group offsetGroup() {
        Group offset = app.group("Offset", bob, alice);
        app.equalExpense(offset, bob, "Groceries", 20.0, bob, alice, bob);
        app.equalExpense(offset, bob, "Gas", 10.0, bob, alice, bob);
        app.equalExpense(offset, alice, "Coffee", 8.0, alice, alice, bob);
        return offset;
    }

    private Settlement settle(Group group, User recorder, User from, User to, double amount) {
        app.signIn(recorder);
        return app.settlementManager.createSettlement(group.getId(), from.getUid(), to.getUid(), amount,
                TestBackend.TODAY, "");
    }

    // --- Show individual balances / accumulate and offset ---

    @Test
    public void debtsAccumulateAndOffsetWithinAPair() {
        Group offset = offsetGroup();
        assertEquals(11.0, app.owed(offset, alice, bob), 0.0);
        assertEquals(0.0, app.owed(offset, bob, alice), 0.0);
    }

    // --- Payable and receivable totals stay separate ---

    @Test
    public void payableAndReceivableTotalsAreSeparate() {
        Group totals = app.group("Totals", alice, bob, carol, david);
        Map<String, Double> exact = new LinkedHashMap<>();
        exact.put(alice.getUid(), 10.0);
        exact.put(bob.getUid(), 10.0);
        app.exactExpense(totals, alice, "Concert tickets", 20.0, alice, exact);
        app.equalExpense(totals, alice, "Lunch", 10.0, alice, alice, carol);
        app.equalExpense(totals, david, "Parking", 8.0, david, david, alice);

        BalanceManager.UserBalanceSummary summary = app.balances.getUserSummary(totals.getId(), alice.getUid());
        assertEquals(15.0, summary.getTotalToReceive(), 0.0);
        assertEquals(4.0, summary.getTotalToPay(), 0.0);
    }

    // --- Group and user-pair boundaries ---

    @Test
    public void debtsAreNotSimplifiedAcrossPairs() {
        Group chain = app.group("Chain", carol, alice, bob);
        app.equalExpense(chain, bob, "Textbook", 20.0, bob, alice, bob);
        app.equalExpense(chain, carol, "Printing", 20.0, carol, bob, carol);

        assertEquals(10.0, app.owed(chain, alice, bob), 0.0);
        assertEquals(10.0, app.owed(chain, bob, carol), 0.0);
        assertEquals("No invented Alice-to-Carol debt", 0.0, app.owed(chain, alice, carol), 0.0);
    }

    @Test
    public void otherGroupsDoNotAffectThisGroup() {
        Group offset = offsetGroup();
        Group other = app.group("Other", alice, bob);
        app.equalExpense(other, alice, "Concert", 100.0, alice, alice, bob);

        assertEquals(11.0, app.owed(offset, alice, bob), 0.0);
        assertEquals(50.0, app.owed(other, bob, alice), 0.0);
    }

    // --- Update after settlement changes ---

    @Test
    public void settlementCreateEditAndDeleteUpdateBalances() {
        Group offset = offsetGroup();
        Settlement payment = settle(offset, alice, alice, bob, 5.0);
        assertEquals("$5 against $11 leaves $6", 6.0, app.owed(offset, alice, bob), 0.0);

        app.settlementManager.editSettlement(payment.getId(), 3.0, "edited");
        assertEquals(8.0, app.owed(offset, alice, bob), 0.0);
        assertEquals("Date stays fixed", TestBackend.TODAY, app.settlementManager.getSettlement(payment.getId()).getDate());

        app.settlementManager.deleteSettlement(payment.getId());
        assertEquals(11.0, app.owed(offset, alice, bob), 0.0);
    }

    @Test
    public void settlementRejectsZeroNegativeAndAboveDebtAmounts() {
        Group offset = offsetGroup();
        assertRejected("Amount must be greater than 0", () -> settle(offset, alice, alice, bob, 0.0));
        assertRejected("Amount must be greater than 0", () -> settle(offset, alice, alice, bob, -5.0));
        assertRejected("Amount cannot exceed what is owed", () -> settle(offset, alice, alice, bob, 20.0));
        assertRejected("at most 2 decimal places", () -> settle(offset, alice, alice, bob, 1.005));
    }

    @Test
    public void recipientCanRecordASettlement() {
        Group offset = offsetGroup();
        Settlement received = settle(offset, bob, alice, bob, 5.0);
        assertEquals(bob.getUid(), received.getCreatorId());
        assertEquals(alice.getUid(), received.getFromUserId());
        assertEquals(6.0, app.owed(offset, alice, bob), 0.0);
    }

    @Test
    public void onlyThePayerOrRecipientCanRecordAndOnlyTheCreatorCanChangeIt() {
        Group offset = offsetGroup();
        app.signIn(bob);
        app.groupManager.inviteMemberByEmail(offset.getId(), carol.getEmail());
        assertRejected("You can only record a payment you made or received",
                () -> settle(offset, carol, alice, bob, 5.0));

        Settlement payment = settle(offset, alice, alice, bob, 5.0);
        app.signIn(bob);
        assertRejected("Only the creator can edit this settlement",
                () -> app.settlementManager.editSettlement(payment.getId(), 1.0, ""));
        assertRejected("Only the creator can delete this settlement",
                () -> app.settlementManager.deleteSettlement(payment.getId()));
    }

    // --- Update after expense changes, and the settled-expense policy ---

    @Test
    public void expenseCreateEditAndDeleteUpdateBalances() {
        Group demo = app.group("Demo", alice, bob, carol);
        Expense dinner = app.equalExpense(demo, alice, "Dinner", 30.0, alice, alice, bob, carol);
        assertEquals(10.0, app.owed(demo, bob, alice), 0.0);

        app.expenseManager.editExpense(dinner.getId(), "Dinner", 60.0, ExpenseCategory.FOOD, alice.getUid(),
                TestBackend.ids(alice, bob, carol), SplitMethod.EQUAL, "", Collections.emptyMap(), Collections.emptyMap());
        assertEquals(20.0, app.owed(demo, bob, alice), 0.0);

        app.expenseManager.deleteExpense(dinner.getId());
        assertTrue(app.balances.getBalancesForGroup(demo.getId()).isEmpty());
    }

    @Test
    public void editingASettledExpenseFlipsTheBalanceInsteadOfLocking() {
        Group offset = offsetGroup();
        settle(offset, alice, alice, bob, 11.0);
        assertEquals(0.0, app.owed(offset, alice, bob), 0.0);

        // Bob lowers Groceries from $20 to $10 after Alice already paid: Alice has now overpaid by $5.
        Expense groceries = null;
        for (Expense e : app.expenses.getByGroup(offset.getId())) if (e.getTitle().equals("Groceries")) groceries = e;
        app.signIn(bob);
        app.expenseManager.editExpense(groceries.getId(), "Groceries", 10.0, ExpenseCategory.FOOD, bob.getUid(),
                TestBackend.ids(alice, bob), SplitMethod.EQUAL, "", Collections.emptyMap(), Collections.emptyMap());

        assertEquals(5.0, app.owed(offset, bob, alice), 0.0);
        assertEquals(0.0, app.owed(offset, alice, bob), 0.0);
    }
}
