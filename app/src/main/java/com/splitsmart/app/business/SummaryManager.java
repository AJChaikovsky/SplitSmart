package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.MonthlySummary;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SummaryManager {
    private static volatile SummaryManager instance;

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public SummaryManager() {
        this(ExpenseRepository.getInstance(), UserRepository.getInstance(), GroupRepository.getInstance());
    }

    public SummaryManager(ExpenseRepository expenseRepository, UserRepository userRepository,
                          GroupRepository groupRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public static SummaryManager getInstance() {
        if (instance == null) {
            synchronized (SummaryManager.class) {
                if (instance == null) instance = new SummaryManager();
            }
        }
        return instance;
    }

    public MonthlySummary getMonthlySummary(String groupId, int year, int month) {
        require(month >= 1 && month <= 12, "Month must be 1-12");
        String prefix = String.format(Locale.US, "%d-%02d", year, month);

        List<Expense> expenses = new ArrayList<>();
        for (Expense expense : expenseRepository.getByGroup(groupId)) {
            if (expense.getDate().startsWith(prefix)) expenses.add(expense);
        }
        if (expenses.isEmpty()) {
            return new MonthlySummary(year, month);
        }

        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Double> payerIdTotals = new LinkedHashMap<>();
        double total = 0.0;
        for (Expense expense : expenses) {
            total += expense.getAmount();
            categoryTotals.put(expense.getCategory(), MoneyUtils.roundToCents(
                    categoryTotals.getOrDefault(expense.getCategory(), 0.0) + expense.getAmount()));
            payerIdTotals.put(expense.getPayerId(), MoneyUtils.roundToCents(
                    payerIdTotals.getOrDefault(expense.getPayerId(), 0.0) + expense.getAmount()));
        }

        Group group = groupRepository.getById(groupId);
        List<String> memberIds = group == null ? Collections.emptyList() : group.getMemberIds();
        if (memberIds.isEmpty()) memberIds = new ArrayList<>(payerIdTotals.keySet());

        // Include members who paid $0 so differences are visible.
        Map<String, Double> payerTotals = new LinkedHashMap<>();
        for (String uid : new LinkedHashSet<>(memberIds)) {
            payerTotals.put(nameOf(uid), payerIdTotals.getOrDefault(uid, 0.0));
        }
        // Anyone who paid but is no longer a member
        for (Map.Entry<String, Double> entry : payerIdTotals.entrySet()) {
            if (!memberIds.contains(entry.getKey())) {
                payerTotals.put(nameOf(entry.getKey()), entry.getValue());
            }
        }

        double roundedTotal = MoneyUtils.roundToCents(total);
        return new MonthlySummary(
                year,
                month,
                expenses.size(),
                roundedTotal,
                categoryTotals,
                payerTotals,
                buildPayerInsights(payerTotals, roundedTotal)
        );
    }

    private String nameOf(String uid) {
        User user = userRepository.getById(uid);
        return user != null ? user.getName() : shortId(uid);
    }

    private List<String> buildPayerInsights(Map<String, Double> payerTotals, double total) {
        if (payerTotals.isEmpty() || total <= 0) return Collections.emptyList();

        List<Map.Entry<String, Double>> ranked = new ArrayList<>(payerTotals.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        LinkedHashSet<String> insights = new LinkedHashSet<>();
        Map.Entry<String, Double> top = ranked.get(0);
        Map.Entry<String, Double> bottom = ranked.get(ranked.size() - 1);
        double fairShare = MoneyUtils.roundToCents(total / ranked.size());

        insights.add(top.getKey() + " paid the most (" + format(top.getValue()) + " · "
                + pct(top.getValue(), total) + "% of group spend)");

        if (ranked.size() >= 2 && bottom.getValue() < top.getValue()) {
            double gap = MoneyUtils.roundToCents(top.getValue() - bottom.getValue());
            insights.add(top.getKey() + " paid " + format(gap) + " more than " + bottom.getKey());
        }

        if (ranked.size() >= 2) {
            List<String> leastPayers = new ArrayList<>();
            for (Map.Entry<String, Double> entry : ranked) {
                if (entry.getValue().equals(bottom.getValue())) leastPayers.add(entry.getKey());
            }
            if (bottom.getValue() == 0.0) {
                insights.add(String.join(", ", leastPayers) + " paid $0 this month");
            } else if (leastPayers.size() == 1) {
                insights.add(bottom.getKey() + " paid the least (" + format(bottom.getValue()) + " · "
                        + pct(bottom.getValue(), total) + "%)");
            }
        }

        for (Map.Entry<String, Double> entry : ranked) {
            double diff = MoneyUtils.roundToCents(entry.getValue() - fairShare);
            if (diff > 0.009) {
                insights.add(entry.getKey() + " is " + format(diff) + " above an even split ("
                        + format(fairShare) + " each)");
            } else if (diff < -0.009) {
                insights.add(entry.getKey() + " is " + format(-diff) + " below an even split ("
                        + format(fairShare) + " each)");
            }
        }

        List<String> result = new ArrayList<>(insights);
        return result.size() > 6 ? result.subList(0, 6) : result;
    }

    private static String format(double amount) {
        return String.format("$%.2f", amount);
    }

    private static int pct(double part, double total) {
        return total <= 0 ? 0 : (int) Math.round(part / total * 100.0);
    }

    static String shortId(String uid) {
        return uid.length() > 8 ? uid.substring(0, 8) : uid;
    }
}
