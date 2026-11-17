package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.ActivityItem;
import com.splitsmart.app.model.ActivityKind;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.FriendBalance;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.List;

/** Extra white-box tests for friend balances and the Activity tab (WB-33 and WB-34). */
public class FriendBalancesAndActivityTest {
    private static final double CENT = 0.001;
    private final TestBackend app = new TestBackend();

    /**
     * WB-33: each friend's net balance adds up across every shared group, largest first.
     * Rationale: Bob appears in two groups with debts in opposite directions, so the merge has to
     * add both (+10 and -4 = +6). Carol is owed money (negative branch) and Dave shares no group
     * (getOrDefault branch). The order checks the sort by absolute amount, not signed amount.
     */
    @Test
    public void friendBalancesNetAcrossGroupsAndSortByAmount() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        User dave = app.user("Dave", "1000000004");
        app.befriend(alice, bob);
        app.befriend(alice, carol);
        app.befriend(alice, dave);

        Group roommates = app.group("Roommates", alice, bob);
        Group trip = app.group("Trip", alice, bob);
        Group lab = app.group("Lab", alice, carol);
        app.equalExpense(roommates, alice, "Groceries", 20.0, alice, alice, bob); // Bob owes Alice 10
        app.equalExpense(trip, bob, "Gas", 8.0, bob, alice, bob);                // Alice owes Bob 4
        app.equalExpense(lab, carol, "Supplies", 30.0, carol, alice, carol);     // Alice owes Carol 15

        app.signIn(alice);
        List<FriendBalance> balances = app.friendManager.listFriendsWithBalances();

        assertEquals(3, balances.size());
        assertEquals("Carol", balances.get(0).getFriend().getName());
        assertEquals(-15.0, balances.get(0).getNetAmount(), CENT);
        assertTrue(balances.get(0).youOwe());
        assertEquals("Bob", balances.get(1).getFriend().getName());
        assertEquals(6.0, balances.get(1).getNetAmount(), CENT);
        assertTrue(balances.get(1).youAreOwed());
        assertEquals("Dave", balances.get(2).getFriend().getName());
        assertTrue(balances.get(2).isSettled());
    }

    /**
     * WB-34: Activity shows only items that involve you, from your groups, newest first.
     * Rationale: covers the "involves me" check for expenses and settlements. Bob and Carol's
     * movie (Alice not involved) and a group Alice isn't in are both left out. Checks the
     * "You added" label for your own expense and the recorder's name for someone else's.
     */
    @Test
    public void activityShowsOnlyMyItemsNewestFirst() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        Group roommates = app.group("Roommates", alice, bob, carol);
        Group outside = app.group("Bob and Carol", bob, carol);

        app.expense(roommates, alice, "Groceries", 30.0, "2026-11-01", ExpenseCategory.FOOD, "", alice,
                alice, bob, carol);
        app.expense(roommates, bob, "Movie", 20.0, "2026-11-03", ExpenseCategory.ENTERTAINMENT, "", bob,
                bob, carol);
        app.signIn(bob);
        app.settlementManager.createSettlement(roommates.getId(), bob.getUid(), alice.getUid(), 10.0,
                "2026-11-05", "");
        app.expense(outside, bob, "Lunch", 12.0, "2026-11-06", ExpenseCategory.FOOD, "", bob, bob, carol);

        app.signIn(alice);
        List<ActivityItem> items = app.activity.getMyActivity();

        assertEquals(2, items.size());
        assertEquals(ActivityKind.SETTLEMENT, items.get(0).getKind());
        assertEquals("2026-11-05", items.get(0).getDate());
        assertTrue(items.get(0).getSubtitle().startsWith("Bob recorded · Bob paid Alice"));
        assertEquals("Groceries", items.get(1).getTitle());
        assertTrue(items.get(1).getSubtitle().startsWith("You added · paid by Alice"));
        assertEquals("Roommates", items.get(1).getGroupName());
    }
}
