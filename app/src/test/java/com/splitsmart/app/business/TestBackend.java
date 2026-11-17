package com.splitsmart.app.business;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.FriendRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.RequestRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Every manager wired the same way the app wires them, but on in-memory storage and login. */
class TestBackend {
    final FakeFirestore db = new FakeFirestore();
    final FakeAuth auth = new FakeAuth();

    final UserRepository users = new UserRepository(db);
    final GroupRepository groups = new GroupRepository(db);
    final ExpenseRepository expenses = new ExpenseRepository(db);
    final SettlementRepository settlements = new SettlementRepository(db);
    final FriendRepository friends = new FriendRepository(db);
    final RequestRepository requests = new RequestRepository(db);

    final AccountManager accounts = new AccountManager(auth, users);
    final BalanceManager balances = new BalanceManager(expenses, settlements);
    final RequestManager requestManager = new RequestManager(requests, users, friends, groups, accounts);
    final GroupManager groupManager = new GroupManager(groups, users, expenses, settlements, accounts, requestManager);
    final ExpenseManager expenseManager = new ExpenseManager(expenses, groups, accounts);
    final SettlementManager settlementManager = new SettlementManager(settlements, balances, accounts);
    final HistoryManager history = new HistoryManager(expenses, settlements);
    final SummaryManager summary = new SummaryManager(expenses, users, groups);
    final FriendManager friendManager =
            new FriendManager(friends, users, groups, expenses, settlements, accounts, balances, requestManager);
    final ActivityManager activity = new ActivityManager(groups, expenses, settlements, users, accounts);

    static final String TODAY = "2026-11-06";

    /** Creates a registered user (account + profile) without signing in. */
    User user(String name, String uscId) {
        String email = name.toLowerCase(Locale.ROOT) + "@usc.edu";
        User user = new User("uid-" + email, name, email, uscId);
        auth.addAccount(user.getUid(), email, "password1");
        users.save(user);
        return user;
    }

    void signIn(User user) {
        auth.signedInUid = user.getUid();
    }

    /** Owner creates the group, invites each member by email, and each member accepts. */
    Group group(String name, User owner, User... members) {
        signIn(owner);
        Group group = groupManager.createGroup(name, "", Collections.emptyList());
        for (User member : members) {
            signIn(owner);
            AppRequest invite = requestManager.sendGroupInviteByEmail(group.getId(), member.getEmail());
            signIn(member);
            requestManager.accept(invite.getId());
        }
        return groupManager.getGroup(group.getId());
    }

    /** {@code from} sends a friend request and {@code to} accepts it. */
    void befriend(User from, User to) {
        signIn(from);
        AppRequest request = requestManager.sendFriendRequestByEmail(to.getEmail());
        signIn(to);
        requestManager.accept(request.getId());
    }

    Expense equalExpense(Group group, User creator, String title, double amount, User payer, User... participants) {
        signIn(creator);
        return expenseManager.createExpense(group.getId(), title, amount, TODAY, ExpenseCategory.OTHER,
                payer.getUid(), ids(participants), SplitMethod.EQUAL, "",
                Collections.emptyMap(), Collections.emptyMap());
    }

    Expense expense(Group group, User creator, String title, double amount, String date, ExpenseCategory category,
                    String note, User payer, User... participants) {
        signIn(creator);
        return expenseManager.createExpense(group.getId(), title, amount, date, category,
                payer.getUid(), ids(participants), SplitMethod.EQUAL, note,
                Collections.emptyMap(), Collections.emptyMap());
    }

    Expense exactExpense(Group group, User creator, String title, double amount, User payer,
                         Map<String, Double> exact) {
        signIn(creator);
        return expenseManager.createExpense(group.getId(), title, amount, TODAY, ExpenseCategory.OTHER,
                payer.getUid(), new java.util.ArrayList<>(exact.keySet()), SplitMethod.EXACT, "",
                exact, Collections.emptyMap());
    }

    /** What {@code from} owes {@code to} in this group right now. */
    double owed(Group group, User from, User to) {
        return balances.amountOwed(from.getUid(), to.getUid(), balances.getBalancesForGroup(group.getId()));
    }

    static List<String> ids(User... people) {
        String[] result = new String[people.length];
        for (int i = 0; i < people.length; i++) result[i] = people[i].getUid();
        return Arrays.asList(result);
    }
}
