package com.splitsmart.app.business;

import static com.splitsmart.app.business.F1UserAndGroupManagementTest.assertRejected;
import static org.junit.Assert.assertEquals;

import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Demo checklist, Feature 3: expense splitting. */
public class F3ExpenseSplittingTest {
    private final ExpenseManager splitter = new ExpenseManager(null, null, null);
    private final List<String> abc = Arrays.asList("alice", "bob", "carol");

    private static Map<String, Double> values(double a, double b, double c) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("alice", a);
        map.put("bob", b);
        map.put("carol", c);
        return map;
    }

    private static double[] amounts(List<ExpenseShare> shares) {
        double[] result = new double[shares.size()];
        for (int i = 0; i < shares.size(); i++) result[i] = shares.get(i).getAmount();
        return result;
    }

    private static long totalCents(List<ExpenseShare> shares) {
        long cents = 0;
        for (ExpenseShare share : shares) cents += MoneyUtils.toCents(share.getAmount());
        return cents;
    }

    private List<ExpenseShare> split(double total, SplitMethod method, Map<String, Double> exact, Map<String, Double> pct) {
        return splitter.computeShares(total, abc, method, exact, pct);
    }

    @Test
    public void equalSplitOfThirtyAmongThreeIsTenEach() {
        List<ExpenseShare> shares = split(30.0, SplitMethod.EQUAL, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(Arrays.toString(new double[]{10.0, 10.0, 10.0}), Arrays.toString(amounts(shares)));
    }

    @Test
    public void exactSplitAssignsEachAmount() {
        List<ExpenseShare> shares = split(30.0, SplitMethod.EXACT, values(5, 10, 15), Collections.emptyMap());
        assertEquals(Arrays.toString(new double[]{5.0, 10.0, 15.0}), Arrays.toString(amounts(shares)));
    }

    @Test
    public void exactSplitAllowsAZeroShare() {
        List<ExpenseShare> shares = split(30.0, SplitMethod.EXACT, values(0, 15, 15), Collections.emptyMap());
        assertEquals(0.0, shares.get(0).getAmount(), 0.0);
    }

    /** WB-15 — exact shares must sum to the expense (including a 1¢ miss). */
    @Test
    public void exactSplitRejectsMismatchedTotal() {
        assertRejected("must add up to the total",
                () -> split(30.0, SplitMethod.EXACT, values(5, 10, 14), Collections.emptyMap()));
        assertRejected("must add up to the total",
                () -> split(30.0, SplitMethod.EXACT, values(5, 10, 14.99), Collections.emptyMap()));
    }

    @Test
    public void percentageSplitOfTwentyThirtyFifty() {
        List<ExpenseShare> shares = split(30.0, SplitMethod.PERCENTAGE, Collections.emptyMap(), values(20, 30, 50));
        assertEquals(Arrays.toString(new double[]{6.0, 9.0, 15.0}), Arrays.toString(amounts(shares)));
    }

    @Test
    public void percentageSplitAllowsZeroPercent() {
        List<ExpenseShare> shares = split(30.0, SplitMethod.PERCENTAGE, Collections.emptyMap(), values(40, 60, 0));
        assertEquals(0.0, shares.get(2).getAmount(), 0.0);
        assertEquals(3000, totalCents(shares));
    }

    /** WB-16 */
    @Test
    public void percentageSplitRejectsTotalOtherThanHundred() {
        assertRejected("must sum to 100%",
                () -> split(30.0, SplitMethod.PERCENTAGE, Collections.emptyMap(), values(40, 40, 0)));
        assertRejected("must sum to 100%",
                () -> split(30.0, SplitMethod.PERCENTAGE, Collections.emptyMap(), values(50, 50, 10)));
    }

    /** WB-14 */
    @Test
    public void equalRoundingGivesRemainderCentToOneParticipant() {
        List<ExpenseShare> shares = split(10.0, SplitMethod.EQUAL, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(Arrays.toString(new double[]{3.34, 3.33, 3.33}), Arrays.toString(amounts(shares)));
        assertEquals(1000, totalCents(shares));
    }

    /** WB-17 */
    @Test
    public void percentageRoundingStillSumsToFullAmount() {
        List<ExpenseShare> shares = split(10.0, SplitMethod.PERCENTAGE, Collections.emptyMap(),
                values(33.33, 33.33, 33.34));
        assertEquals(1000, totalCents(shares));
        List<ExpenseShare> odd = split(0.10, SplitMethod.PERCENTAGE, Collections.emptyMap(), values(33.33, 33.33, 33.34));
        assertEquals(10, totalCents(odd));
    }

    @Test
    public void eachMethodPicksItsOwnCalculator() {
        // Same inputs, three methods: each produces its own shares.
        assertEquals(1000, totalCents(split(10.0, SplitMethod.EQUAL, values(2, 3, 5), values(10, 20, 70))));
        assertEquals(Arrays.toString(new double[]{2.0, 3.0, 5.0}),
                Arrays.toString(amounts(split(10.0, SplitMethod.EXACT, values(2, 3, 5), values(10, 20, 70)))));
        assertEquals(Arrays.toString(new double[]{1.0, 2.0, 7.0}),
                Arrays.toString(amounts(split(10.0, SplitMethod.PERCENTAGE, values(2, 3, 5), values(10, 20, 70)))));
    }

    @Test
    public void alicePayingThirtyEquallyMeansBobAndCarolEachOweHerTen() {
        TestBackend app = new TestBackend();
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        Group demo = app.group("Demo", alice, bob, carol);
        app.equalExpense(demo, alice, "Dinner", 30.0, alice, alice, bob, carol);

        assertEquals(10.0, app.owed(demo, bob, alice), 0.0);
        assertEquals(10.0, app.owed(demo, carol, alice), 0.0);
        assertEquals("Alice owes nobody", 0.0,
                app.balances.getUserSummary(demo.getId(), alice.getUid()).getTotalToPay(), 0.0);
    }
}
