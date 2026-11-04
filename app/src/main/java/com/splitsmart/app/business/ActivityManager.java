package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.ActivityItem;
import com.splitsmart.app.model.ActivityKind;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.GroupBalance;
import com.splitsmart.app.model.Settlement;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityManager {
    private static volatile ActivityManager instance;

    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final AccountManager accountManager;
    private final BalanceManager balanceManager;

    public ActivityManager() {
        this(GroupRepository.getInstance(), ExpenseRepository.getInstance(), SettlementRepository.getInstance(),
                UserRepository.getInstance(), AccountManager.getInstance(), BalanceManager.getInstance());
    }

    public ActivityManager(GroupRepository groupRepository, ExpenseRepository expenseRepository,
                           SettlementRepository settlementRepository, UserRepository userRepository,
                           AccountManager accountManager, BalanceManager balanceManager) {
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.userRepository = userRepository;
        this.accountManager = accountManager;
        this.balanceManager = balanceManager;
    }

    /** What the current user owes and is owed in each of their groups, largest amounts first. */
    public List<GroupBalance> getGroupBalances() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        List<GroupBalance> result = new ArrayList<>();
        for (Group group : groupRepository.getGroupsForUser(uid)) {
            result.add(balanceManager.summarizeGroupForUser(group, uid,
                    expenseRepository.getByGroup(group.getId()),
                    settlementRepository.getByGroup(group.getId())));
        }
        result.sort(Comparator.comparingDouble(
                (GroupBalance balance) -> balance.getToPay() + balance.getToReceive()).reversed());
        return result;
    }

    public static ActivityManager getInstance() {
        if (instance == null) {
            synchronized (ActivityManager.class) {
                if (instance == null) instance = new ActivityManager();
            }
        }
        return instance;
    }

    /**
     * Expenses and settlements across all groups that involve the current user
     * (as payer, participant, creator, or settlement party).
     */
    public List<ActivityItem> getMyActivity() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        List<ActivityItem> items = new ArrayList<>();
        Map<String, String> nameCache = new HashMap<>();

        for (Group group : groupRepository.getGroupsForUser(uid)) {
            for (Expense expense : expenseRepository.getByGroup(group.getId())) {
                boolean involvesMe = uid.equals(expense.getPayerId())
                        || uid.equals(expense.getCreatorId())
                        || expense.getParticipantIds().contains(uid);
                if (!involvesMe) continue;

                String payer = nameOf(expense.getPayerId(), nameCache);
                String actor = nameOf(expense.getCreatorId(), nameCache);
                String youLabel = uid.equals(expense.getCreatorId()) ? "You" : actor;
                items.add(new ActivityItem(
                        expense.getId(),
                        ActivityKind.EXPENSE,
                        group.getId(),
                        group.getName(),
                        expense.getTitle(),
                        youLabel + " added · paid by " + payer + " · " + expense.getCategory(),
                        expense.getAmount(),
                        expense.getDate(),
                        actor
                ));
            }

            for (Settlement settlement : settlementRepository.getByGroup(group.getId())) {
                boolean involvesMe = uid.equals(settlement.getFromUserId())
                        || uid.equals(settlement.getToUserId())
                        || uid.equals(settlement.getCreatorId());
                if (!involvesMe) continue;

                String from = nameOf(settlement.getFromUserId(), nameCache);
                String to = nameOf(settlement.getToUserId(), nameCache);
                String actor = nameOf(settlement.getCreatorId(), nameCache);
                String youLabel = uid.equals(settlement.getCreatorId()) ? "You" : actor;
                items.add(new ActivityItem(
                        settlement.getId(),
                        ActivityKind.SETTLEMENT,
                        group.getId(),
                        group.getName(),
                        "Settlement",
                        youLabel + " recorded · " + from + " paid " + to,
                        settlement.getAmount(),
                        settlement.getDate(),
                        actor
                ));
            }
        }

        items.sort(Comparator.comparing(ActivityItem::getDate)
                .thenComparing(ActivityItem::getId)
                .reversed());
        return items;
    }

    private String nameOf(String userId, Map<String, String> cache) {
        String cached = cache.get(userId);
        if (cached != null) return cached;
        User user = userRepository.getById(userId);
        String name = user != null ? user.getName() : SummaryManager.shortId(userId);
        cache.put(userId, name);
        return name;
    }
}
