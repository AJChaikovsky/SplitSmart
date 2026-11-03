package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;
import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.FriendRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.FriendBalance;
import com.splitsmart.app.model.Friendship;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FriendManager {
    private static volatile FriendManager instance;

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final AccountManager accountManager;
    private final BalanceManager balanceManager;
    private final RequestManager requestManager;

    public FriendManager() {
        this(FriendRepository.getInstance(), UserRepository.getInstance(), GroupRepository.getInstance(),
                ExpenseRepository.getInstance(), SettlementRepository.getInstance(), AccountManager.getInstance(),
                BalanceManager.getInstance(), RequestManager.getInstance());
    }

    public FriendManager(FriendRepository friendRepository, UserRepository userRepository,
                         GroupRepository groupRepository, ExpenseRepository expenseRepository,
                         SettlementRepository settlementRepository, AccountManager accountManager,
                         BalanceManager balanceManager, RequestManager requestManager) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.accountManager = accountManager;
        this.balanceManager = balanceManager;
        this.requestManager = requestManager;
    }

    public static FriendManager getInstance() {
        if (instance == null) {
            synchronized (FriendManager.class) {
                if (instance == null) instance = new FriendManager();
            }
        }
        return instance;
    }

    /** Sends a friend request (recipient must accept on the Requests tab). */
    public void addFriendByEmail(String email) {
        requestManager.sendFriendRequestByEmail(email);
    }

    public List<User> listFriends() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        List<User> friends = new ArrayList<>();
        for (Friendship friendship : friendRepository.listForUser(uid)) {
            for (String otherId : friendship.getUserIds()) {
                if (otherId.equals(uid)) continue;
                User friend = userRepository.getById(otherId);
                if (friend != null) friends.add(friend);
                break;
            }
        }
        friends.sort(Comparator.comparing(user -> user.getName().toLowerCase(Locale.ROOT)));
        return friends;
    }

    /**
     * Friends with net balance across all shared groups.
     * Positive netAmount => friend owes you; negative => you owe them.
     */
    public List<FriendBalance> listFriendsWithBalances() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        List<User> friends = listFriends();
        if (friends.isEmpty()) return Collections.emptyList();

        Map<String, Long> netByFriend = new HashMap<>();
        for (Group group : groupRepository.getGroupsForUser(uid)) {
            List<BalanceManager.PairBalance> balances = balanceManager.computePairwiseBalances(
                    expenseRepository.getByGroup(group.getId()),
                    settlementRepository.getByGroup(group.getId())
            );
            for (BalanceManager.PairBalance pair : balances) {
                long cents = MoneyUtils.toCents(pair.getAmount());
                if (pair.getToUserId().equals(uid)) {
                    netByFriend.merge(pair.getFromUserId(), cents, Long::sum);
                } else if (pair.getFromUserId().equals(uid)) {
                    netByFriend.merge(pair.getToUserId(), -cents, Long::sum);
                }
            }
        }

        List<FriendBalance> result = new ArrayList<>();
        for (User friend : friends) {
            result.add(new FriendBalance(friend, MoneyUtils.fromCents(netByFriend.getOrDefault(friend.getUid(), 0L))));
        }
        result.sort(Comparator
                .comparingDouble((FriendBalance balance) -> Math.abs(balance.getNetAmount())).reversed()
                .thenComparing(balance -> balance.getFriend().getName().toLowerCase(Locale.ROOT)));
        return result;
    }

    public void removeFriend(String friendUid) {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        require(!friendUid.equals(uid), "Invalid friend");
        friendRepository.delete(Friendship.docIdFor(uid, friendUid));
    }
}
