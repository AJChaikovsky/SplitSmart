package com.splitsmart.app.business;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.GroupBalance;
import com.splitsmart.app.model.Settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pairwise balances from expenses + settlements.
 * Non-payer participants owe the payer their share.
 * totalToPay and totalToReceive are shown separately (not netted into one number for display totals).
 * No debt simplification across triples.
 */
public class BalanceManager {
    private static volatile BalanceManager instance;

    // Resolved lazily so the pure calculations can be unit tested without Firebase.
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    public BalanceManager() {
        this(null, null);
    }

    public BalanceManager(ExpenseRepository expenseRepository, SettlementRepository settlementRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    public static BalanceManager getInstance() {
        if (instance == null) {
            synchronized (BalanceManager.class) {
                if (instance == null) instance = new BalanceManager();
            }
        }
        return instance;
    }

    private ExpenseRepository expenses() {
        return expenseRepository != null ? expenseRepository : ExpenseRepository.getInstance();
    }

    private SettlementRepository settlements() {
        return settlementRepository != null ? settlementRepository : SettlementRepository.getInstance();
    }

    /** A debt where {@code fromUserId} owes {@code toUserId} a positive {@code amount}. */
    public static class PairBalance {
        private final String fromUserId;
        private final String toUserId;
        private final double amount;

        public PairBalance(String fromUserId, String toUserId, double amount) {
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
            this.amount = amount;
        }

        public String getFromUserId() { return fromUserId; }
        public String getToUserId() { return toUserId; }
        public double getAmount() { return amount; }
    }

    public static class UserBalanceSummary {
        private final String userId;
        private final double totalToPay;
        private final double totalToReceive;
        private final List<PairBalance> debts;

        public UserBalanceSummary(String userId, double totalToPay, double totalToReceive, List<PairBalance> debts) {
            this.userId = userId;
            this.totalToPay = totalToPay;
            this.totalToReceive = totalToReceive;
            this.debts = debts;
        }

        public String getUserId() { return userId; }
        public double getTotalToPay() { return totalToPay; }
        public double getTotalToReceive() { return totalToReceive; }
        public List<PairBalance> getDebts() { return debts; }
    }

    /**
     * Computes net pairwise balances. Each result means {@code fromUserId} owes {@code toUserId}.
     */
    public List<PairBalance> computePairwiseBalances(List<Expense> expenses, List<Settlement> settlements) {
        // Directed "from|to" -> cents owed, before netting each pair.
        Map<String, Long> directed = new HashMap<>();
        Set<String> users = new LinkedHashSet<>();

        for (Expense expense : expenses) {
            String payer = expense.getPayerId();
            for (ExpenseShare share : expense.getShares()) {
                if (!share.getUserId().equals(payer)) {
                    addOwes(directed, users, share.getUserId(), payer, MoneyUtils.toCents(share.getAmount()));
                }
            }
        }

        for (Settlement settlement : settlements) {
            // Settlement: fromUser pays toUser, reducing what from owes to
            addOwes(directed, users, settlement.getFromUserId(), settlement.getToUserId(),
                    -MoneyUtils.toCents(settlement.getAmount()));
        }

        // Net each unordered pair into a single directed debt
        List<PairBalance> results = new ArrayList<>();
        for (String a : users) {
            for (String b : users) {
                if (a.compareTo(b) >= 0) continue;
                long aOwesB = directed.getOrDefault(key(a, b), 0L);
                long bOwesA = directed.getOrDefault(key(b, a), 0L);
                long net = aOwesB - bOwesA;
                if (net > 0) {
                    results.add(new PairBalance(a, b, MoneyUtils.fromCents(net)));
                } else if (net < 0) {
                    results.add(new PairBalance(b, a, MoneyUtils.fromCents(-net)));
                }
            }
        }
        results.sort(Comparator.comparingDouble(PairBalance::getAmount).reversed());
        return results;
    }

    private static void addOwes(Map<String, Long> directed, Set<String> users, String from, String to, long cents) {
        if (from.equals(to) || cents == 0L) return;
        directed.merge(key(from, to), cents, Long::sum);
        users.add(from);
        users.add(to);
    }

    private static String key(String from, String to) {
        return from + "|" + to;
    }

    public UserBalanceSummary summarizeForUser(String userId, List<PairBalance> balances) {
        List<PairBalance> debts = new ArrayList<>();
        double totalToPay = 0.0;
        double totalToReceive = 0.0;
        for (PairBalance balance : balances) {
            if (balance.getFromUserId().equals(userId)) {
                debts.add(balance);
                totalToPay += balance.getAmount();
            } else if (balance.getToUserId().equals(userId)) {
                debts.add(balance);
                totalToReceive += balance.getAmount();
            }
        }
        return new UserBalanceSummary(
                userId,
                MoneyUtils.roundToCents(totalToPay),
                MoneyUtils.roundToCents(totalToReceive),
                debts
        );
    }

    /** The user's totals in one group, computed from that group's expenses and settlements. */
    public GroupBalance summarizeGroupForUser(Group group, String userId,
                                              List<Expense> groupExpenses, List<Settlement> groupSettlements) {
        UserBalanceSummary summary = summarizeForUser(userId, computePairwiseBalances(groupExpenses, groupSettlements));
        return new GroupBalance(group.getId(), group.getName(), summary.getTotalToPay(), summary.getTotalToReceive());
    }

    public List<PairBalance> getBalancesForGroup(String groupId) {
        List<Expense> groupExpenses = expenses().getByGroup(groupId);
        List<Settlement> groupSettlements = settlements().getByGroup(groupId);
        return computePairwiseBalances(groupExpenses, groupSettlements);
    }

    public UserBalanceSummary getUserSummary(String groupId, String userId) {
        return summarizeForUser(userId, getBalancesForGroup(groupId));
    }

    public double amountOwed(String fromUserId, String toUserId, List<PairBalance> balances) {
        for (PairBalance balance : balances) {
            if (balance.getFromUserId().equals(fromUserId) && balance.getToUserId().equals(toUserId)) {
                return balance.getAmount();
            }
        }
        return 0.0;
    }
}
