package com.splitsmart.app.business;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Settlement;
import com.splitsmart.app.model.Transaction;
import com.splitsmart.app.model.TransactionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HistoryManager {
    private static volatile HistoryManager instance;

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    public HistoryManager() {
        this(ExpenseRepository.getInstance(), SettlementRepository.getInstance());
    }

    public HistoryManager(ExpenseRepository expenseRepository, SettlementRepository settlementRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    public static HistoryManager getInstance() {
        if (instance == null) {
            synchronized (HistoryManager.class) {
                if (instance == null) instance = new HistoryManager();
            }
        }
        return instance;
    }

    public enum SortOrder { NEWEST, OLDEST }

    /** Search and filter options; any null field means "no filter". */
    public static class HistoryFilter {
        private final String keyword;
        private final TransactionType type;
        private final ExpenseCategory category;
        private final String memberId;
        private final SortOrder sortOrder;

        public HistoryFilter() {
            this(null, null, null, null, SortOrder.NEWEST);
        }

        public HistoryFilter(String keyword, TransactionType type, ExpenseCategory category,
                             String memberId, SortOrder sortOrder) {
            this.keyword = keyword;
            this.type = type;
            this.category = category;
            this.memberId = memberId;
            this.sortOrder = sortOrder;
        }

        public String getKeyword() { return keyword; }
        public TransactionType getType() { return type; }
        public ExpenseCategory getCategory() { return category; }
        public String getMemberId() { return memberId; }
        public SortOrder getSortOrder() { return sortOrder; }

        public HistoryFilter withMemberId(String newMemberId) {
            return new HistoryFilter(keyword, type, category, newMemberId, sortOrder);
        }
    }

    public List<Transaction> getHistory(String groupId, HistoryFilter filter) {
        List<Transaction> merged = new ArrayList<>();
        for (Expense expense : expenseRepository.getByGroup(groupId)) merged.add(Transaction.fromExpense(expense));
        for (Settlement settlement : settlementRepository.getByGroup(groupId)) {
            merged.add(Transaction.fromSettlement(settlement));
        }

        String keyword = filter.getKeyword() == null ? "" : filter.getKeyword().trim().toLowerCase(Locale.ROOT);
        List<Transaction> matches = new ArrayList<>();
        for (Transaction tx : merged) {
            if (!keyword.isEmpty()
                    && !tx.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
                    && !tx.getNote().toLowerCase(Locale.ROOT).contains(keyword)) {
                continue;
            }
            if (filter.getType() != null && tx.getType() != filter.getType()) continue;
            if (filter.getCategory() != null
                    && (tx.getType() != TransactionType.EXPENSE
                    || !tx.getCategory().equalsIgnoreCase(filter.getCategory().name()))) {
                continue;
            }
            if (filter.getMemberId() != null && !tx.getMemberIds().contains(filter.getMemberId())) continue;
            matches.add(tx);
        }

        Comparator<Transaction> oldestFirst = Comparator.comparing(Transaction::getDate)
                .thenComparing(Transaction::getId);
        matches.sort(filter.getSortOrder() == SortOrder.OLDEST ? oldestFirst : oldestFirst.reversed());
        return matches;
    }
}
