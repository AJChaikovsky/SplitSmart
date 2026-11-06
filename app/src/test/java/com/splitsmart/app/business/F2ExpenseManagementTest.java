package com.splitsmart.app.business;

import static com.splitsmart.app.business.F1UserAndGroupManagementTest.assertRejected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Demo checklist, Feature 2: expense management. */
public class F2ExpenseManagementTest {
    private final TestBackend app = new TestBackend();
    private final User alice = app.user("Alice", "1000000001");
    private final User bob = app.user("Bob", "1000000002");
    private final User carol = app.user("Carol", "1000000003");
    private final User david = app.user("David", "1000000004");
    private final Group demo = app.group("Demo", alice, bob, carol);

    private Expense create(double amount, String payerId, List<String> participants) {
        app.signIn(alice);
        return app.expenseManager.createExpense(demo.getId(), "Dinner", amount, TestBackend.TODAY,
                ExpenseCategory.FOOD, payerId, participants, SplitMethod.EQUAL, "demo",
                Collections.emptyMap(), Collections.emptyMap());
    }

    // --- Create a complete expense ---

    @Test
    public void savesEveryFieldOfACompleteExpense() {
        Expense created = create(30.0, alice.getUid(), TestBackend.ids(alice, bob, carol));
        Expense saved = app.expenseManager.getExpense(created.getId());

        assertEquals("Dinner", saved.getTitle());
        assertEquals(30.0, saved.getAmount(), 0.0);
        assertEquals(ExpenseCategory.FOOD, saved.categoryEnum());
        assertEquals(alice.getUid(), saved.getPayerId());
        assertEquals(TestBackend.ids(alice, bob, carol), saved.getParticipantIds());
        assertEquals(SplitMethod.EQUAL, saved.splitMethodEnum());
        assertEquals("demo", saved.getNote());
        assertTrue("YYYY-MM-DD date", saved.getDate().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(alice.getUid(), saved.getCreatorId());
    }

    @Test
    public void memberCanRecordAnotherPayerWithoutParticipating() {
        Expense movie = create(24.0, bob.getUid(), TestBackend.ids(bob, carol));
        assertEquals(alice.getUid(), movie.getCreatorId());
        assertFalse(movie.getParticipantIds().contains(alice.getUid()));
        assertEquals(bob.getUid(), movie.getPayerId());
    }

    // --- Required categories ---

    @Test
    public void hasTheFiveRequiredCategoriesAndDefaultsToOther() {
        List<String> names = new ArrayList<>();
        for (ExpenseCategory category : ExpenseCategory.values()) names.add(category.name());
        assertEquals(Arrays.asList("FOOD", "HOUSING", "TRANSPORTATION", "ENTERTAINMENT", "OTHER"), names);
        assertEquals(ExpenseCategory.OTHER, new Expense().categoryEnum());
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromString(null));
    }

    // --- Validate the amount ---

    @Test
    public void rejectsZeroNegativeAndThreeDecimalAmounts() {
        List<String> everyone = TestBackend.ids(alice, bob, carol);
        assertRejected("Amount must be greater than 0", () -> create(0.0, alice.getUid(), everyone));
        assertRejected("Amount must be greater than 0", () -> create(-5.0, alice.getUid(), everyone));
        assertRejected("at most 2 decimal places", () -> create(12.345, alice.getUid(), everyone));
        assertEquals(30.0, create(30.0, alice.getUid(), everyone).getAmount(), 0.0);
    }

    // --- Validate payer and participants ---

    @Test
    public void payerAndParticipantsMustBelongToTheGroup() {
        assertRejected("All participants must be members of the group",
                () -> create(30.0, alice.getUid(), TestBackend.ids(alice, david)));
        assertRejected("Payer must be one of the participants",
                () -> create(30.0, david.getUid(), TestBackend.ids(alice, bob)));
    }

    @Test
    public void needsTwoParticipantsIncludingThePayer() {
        assertRejected("At least 2 participants are required",
                () -> create(30.0, alice.getUid(), TestBackend.ids(alice)));
        assertRejected("Payer must be one of the participants",
                () -> create(30.0, alice.getUid(), TestBackend.ids(bob, carol)));
    }

    // --- Edit/delete with correct permissions ---

    @Test
    public void creatorEditsPayerParticipantsAndAmountButKeepsCreationDate() {
        app.signIn(alice);
        Expense original = app.expenseManager.createExpense(demo.getId(), "Dinner", 30.0, "2026-10-15",
                ExpenseCategory.FOOD, alice.getUid(), TestBackend.ids(alice, bob, carol), SplitMethod.EQUAL, "",
                Collections.emptyMap(), Collections.emptyMap());

        app.expenseManager.editExpense(original.getId(), "Dinner", 40.0, ExpenseCategory.FOOD, bob.getUid(),
                TestBackend.ids(alice, bob), SplitMethod.EQUAL, "", Collections.emptyMap(), Collections.emptyMap());

        Expense saved = app.expenseManager.getExpense(original.getId());
        assertEquals(40.0, saved.getAmount(), 0.0);
        assertEquals(bob.getUid(), saved.getPayerId());
        assertEquals(TestBackend.ids(alice, bob), saved.getParticipantIds());
        assertEquals("Creation date unchanged", "2026-10-15", saved.getDate());
    }

    @Test
    public void otherMemberCanViewButNotEditOrDelete() {
        Expense dinner = create(30.0, alice.getUid(), TestBackend.ids(alice, bob, carol));
        app.signIn(bob);
        assertEquals("Dinner", app.expenseManager.getExpense(dinner.getId()).getTitle());
        assertRejected("Only the creator can edit this expense",
                () -> app.expenseManager.editExpense(dinner.getId(), "Dinner", 1.0, ExpenseCategory.FOOD,
                        bob.getUid(), TestBackend.ids(alice, bob), SplitMethod.EQUAL, "",
                        Collections.emptyMap(), Collections.emptyMap()));
        assertRejected("Only the creator can delete this expense",
                () -> app.expenseManager.deleteExpense(dinner.getId()));
    }

    @Test
    public void creatorCanDelete() {
        Expense dinner = create(30.0, alice.getUid(), TestBackend.ids(alice, bob, carol));
        app.expenseManager.deleteExpense(dinner.getId());
        assertTrue(app.expenseManager.listExpenses(demo.getId()).isEmpty());
    }

    // --- Preserve and share changes ---

    @Test
    public void savedChangesAreVisibleToAnotherMember() {
        Expense dinner = create(30.0, alice.getUid(), TestBackend.ids(alice, bob, carol));
        app.expenseManager.editExpense(dinner.getId(), "Dinner", 45.0, ExpenseCategory.FOOD, alice.getUid(),
                TestBackend.ids(alice, bob, carol), SplitMethod.EQUAL, "", Collections.emptyMap(), Collections.emptyMap());

        app.signIn(bob);
        List<Expense> seenByBob = app.expenseManager.listExpenses(demo.getId());
        assertEquals(1, seenByBob.size());
        assertEquals(45.0, seenByBob.get(0).getAmount(), 0.0);
    }
}
