package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.isBlank;
import static com.splitsmart.app.business.Preconditions.require;
import static com.splitsmart.app.business.Preconditions.requireFound;
import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;

import java.util.List;
import java.util.Map;

public class ExpenseManager {
    private static volatile ExpenseManager instance;

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final AccountManager accountManager;

    public ExpenseManager() {
        this(ExpenseRepository.getInstance(), GroupRepository.getInstance(), AccountManager.getInstance());
    }

    public ExpenseManager(ExpenseRepository expenseRepository, GroupRepository groupRepository,
                          AccountManager accountManager) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
        this.accountManager = accountManager;
    }

    public static ExpenseManager getInstance() {
        if (instance == null) {
            synchronized (ExpenseManager.class) {
                if (instance == null) instance = new ExpenseManager();
            }
        }
        return instance;
    }

    public void validateExpenseInput(double amount, String payerId, List<String> participantIds,
                                     List<String> groupMemberIds) {
        require(amount > 0, "Amount must be greater than 0");
        require(MoneyUtils.hasAtMostTwoDecimals(amount), "Amount may have at most 2 decimal places");
        require(participantIds.size() >= 2, "At least 2 participants are required");
        require(participantIds.contains(payerId), "Payer must be one of the participants");
        require(groupMemberIds.containsAll(participantIds), "All participants must be members of the group");
        require(groupMemberIds.contains(payerId), "Payer must be a member of the group");
    }

    /** Picks the split strategy for {@code splitMethod} and returns each participant's share. */
    public List<ExpenseShare> computeShares(double amount, List<String> participantIds, SplitMethod splitMethod,
                                            Map<String, Double> exactAmounts, Map<String, Double> percentages) {
        SplitCalculator calculator;
        switch (splitMethod) {
            case EXACT:
                calculator = new ExactSplitCalculator();
                break;
            case PERCENTAGE:
                calculator = new PercentageSplitCalculator();
                break;
            case EQUAL:
            default:
                calculator = new EqualSplitCalculator();
                break;
        }
        return calculator.calculate(amount, participantIds, exactAmounts, percentages);
    }

    public Expense createExpense(String groupId, String title, double amount, String date,
                                 ExpenseCategory category, String payerId, List<String> participantIds,
                                 SplitMethod splitMethod, String note,
                                 Map<String, Double> exactAmounts, Map<String, Double> percentages) {
        require(!isBlank(title), "Title is required");
        MoneyUtils.requireIsoDate(date);

        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String creatorId = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getMemberIds().contains(creatorId), "You must be a group member to add expenses");

        validateExpenseInput(amount, payerId, participantIds, group.getMemberIds());
        List<ExpenseShare> shares = computeShares(amount, participantIds, splitMethod, exactAmounts, percentages);

        Expense expense = new Expense();
        expense.setGroupId(groupId);
        expense.setTitle(title.trim());
        expense.setAmount(MoneyUtils.roundToCents(amount));
        expense.setDate(date.trim());
        expense.setCategory(category.name());
        expense.setPayerId(payerId);
        expense.setParticipantIds(participantIds);
        expense.setSplitMethod(splitMethod.name());
        expense.setNote(note.trim());
        expense.setCreatorId(creatorId);
        expense.setShares(shares);
        return expenseRepository.save(expense);
    }

    public Expense editExpense(String expenseId, String title, double amount, String date,
                               ExpenseCategory category, String payerId, List<String> participantIds,
                               SplitMethod splitMethod, String note,
                               Map<String, Double> exactAmounts, Map<String, Double> percentages) {
        Expense existing = requireFound(expenseRepository.getById(expenseId), "Expense not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(existing.getCreatorId().equals(currentUid), "Only the creator can edit this expense");

        Group group = requireFound(groupRepository.getById(existing.getGroupId()), "Group not found");

        require(!isBlank(title), "Title is required");
        MoneyUtils.requireIsoDate(date);
        validateExpenseInput(amount, payerId, participantIds, group.getMemberIds());
        List<ExpenseShare> shares = computeShares(amount, participantIds, splitMethod, exactAmounts, percentages);

        existing.setTitle(title.trim());
        existing.setAmount(MoneyUtils.roundToCents(amount));
        existing.setDate(date.trim());
        existing.setCategory(category.name());
        existing.setPayerId(payerId);
        existing.setParticipantIds(participantIds);
        existing.setSplitMethod(splitMethod.name());
        existing.setNote(note.trim());
        existing.setShares(shares);
        return expenseRepository.save(existing);
    }

    public void deleteExpense(String expenseId) {
        Expense existing = requireFound(expenseRepository.getById(expenseId), "Expense not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(existing.getCreatorId().equals(currentUid), "Only the creator can delete this expense");
        expenseRepository.delete(expenseId);
    }

    public Expense getExpense(String expenseId) {
        return requireFound(expenseRepository.getById(expenseId), "Expense not found");
    }

    public List<Expense> listExpenses(String groupId) {
        return expenseRepository.getByGroup(groupId);
    }
}
