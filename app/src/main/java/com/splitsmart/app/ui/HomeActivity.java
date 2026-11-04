package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.ActivityManager;
import com.splitsmart.app.business.FriendManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.business.RequestManager;
import com.splitsmart.app.databinding.ActivityHomeBinding;
import com.splitsmart.app.model.ActivityItem;
import com.splitsmart.app.model.ActivityKind;
import com.splitsmart.app.model.FriendBalance;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.GroupBalance;
import com.splitsmart.app.model.IncomingRequest;
import com.splitsmart.app.model.RequestType;
import com.splitsmart.app.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "groupId";
    private static final String STATE_TAB = "homeTab";

    private ActivityHomeBinding binding;
    private final GroupManager groupManager = GroupManager.getInstance();
    private final FriendManager friendManager = FriendManager.getInstance();
    private final ActivityManager activityManager = ActivityManager.getInstance();
    private final RequestManager requestManager = RequestManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();

    private final GroupAdapter groupsAdapter = new GroupAdapter(group ->
            startActivity(new Intent(this, GroupDetailActivity.class).putExtra(EXTRA_GROUP_ID, group.getId())));
    private final FriendAdapter friendsAdapter = new FriendAdapter();
    private final RequestAdapter requestsAdapter = new RequestAdapter(this::acceptRequest, this::denyRequest);
    private final ActivityAdapter activityAdapter = new ActivityAdapter(this::openActivityItem);

    private int currentTab = R.id.nav_groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!accountManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.rvGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGroups.setAdapter(groupsAdapter);
        binding.rvFriends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFriends.setAdapter(friendsAdapter);
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRequests.setAdapter(requestsAdapter);
        binding.rvActivity.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActivity.setAdapter(activityAdapter);

        binding.btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.fabCreate.setOnClickListener(v -> onFabClick());
        // Come back to the same tab after a rotation or theme change.
        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_TAB, R.id.nav_groups)
                : R.id.nav_groups;
        binding.bottomNav.setSelectedItemId(tab);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        showTab(tab);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_TAB, currentTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) refreshCurrentTab();
    }

    private void onFabClick() {
        if (currentTab == R.id.nav_groups) {
            startActivity(new Intent(this, CreateGroupActivity.class));
        } else if (currentTab == R.id.nav_friends) {
            showAddFriendDialog();
        }
    }

    private void showTab(int tabId) {
        currentTab = tabId;
        binding.panelGroups.setVisibility(tabId == R.id.nav_groups ? View.VISIBLE : View.GONE);
        binding.panelFriends.setVisibility(tabId == R.id.nav_friends ? View.VISIBLE : View.GONE);
        binding.panelRequests.setVisibility(tabId == R.id.nav_requests ? View.VISIBLE : View.GONE);
        binding.panelActivity.setVisibility(tabId == R.id.nav_activity ? View.VISIBLE : View.GONE);

        if (tabId == R.id.nav_groups) {
            binding.tvHeaderTitle.setText(R.string.your_groups);
            binding.tvHeaderSubtitle.setText(R.string.groups_subtitle);
            binding.fabCreate.show();
            binding.fabCreate.setContentDescription(getString(R.string.create_group));
        } else if (tabId == R.id.nav_friends) {
            binding.tvHeaderTitle.setText(R.string.your_friends);
            binding.tvHeaderSubtitle.setText(R.string.friends_subtitle);
            binding.fabCreate.show();
            binding.fabCreate.setContentDescription(getString(R.string.add_friend));
        } else if (tabId == R.id.nav_requests) {
            binding.tvHeaderTitle.setText(R.string.your_requests);
            binding.tvHeaderSubtitle.setText(R.string.requests_subtitle);
            binding.fabCreate.hide();
        } else if (tabId == R.id.nav_activity) {
            binding.tvHeaderTitle.setText(R.string.your_activity);
            binding.tvHeaderSubtitle.setText(R.string.activity_subtitle);
            binding.fabCreate.hide();
        }
        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        if (currentTab == R.id.nav_groups) loadGroups();
        else if (currentTab == R.id.nav_friends) loadFriends();
        else if (currentTab == R.id.nav_requests) loadRequests();
        else if (currentTab == R.id.nav_activity) loadActivity();
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        UiUtils.toast(this, message);
    }

    private void loadGroups() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, groupManager::listGroupsForCurrentUser, groups -> {
            binding.progress.setVisibility(View.GONE);
            groupsAdapter.submit(groups);
            binding.tvEmptyGroups.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
        }, this::showError);
    }

    private void loadFriends() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, friendManager::listFriendsWithBalances, friends -> {
            double owedToYou = 0.0;
            double youOwe = 0.0;
            for (FriendBalance friend : friends) {
                if (friend.youAreOwed()) owedToYou += friend.getNetAmount();
                if (friend.youOwe()) youOwe -= friend.getNetAmount();
            }
            binding.progress.setVisibility(View.GONE);
            friendsAdapter.submit(friends);
            binding.tvEmptyFriends.setVisibility(friends.isEmpty() ? View.VISIBLE : View.GONE);
            if (friends.isEmpty()) {
                binding.tvFriendsSummary.setVisibility(View.GONE);
            } else {
                binding.tvFriendsSummary.setVisibility(View.VISIBLE);
                binding.tvFriendsSummary.setText(getString(R.string.you_are_owed) + ": "
                        + UiUtils.formatMoney(owedToYou) + "  ·  "
                        + getString(R.string.you_owe) + ": " + UiUtils.formatMoney(youOwe));
            }
        }, this::showError);
    }

    private void loadRequests() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, requestManager::listIncomingPending, requests -> {
            binding.progress.setVisibility(View.GONE);
            requestsAdapter.submit(requests);
            binding.tvEmptyRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        }, this::showError);
    }

    /** Activity rows plus per-group balances, loaded together for the Activity tab. */
    private static class ActivityData {
        final List<ActivityItem> items;
        final List<GroupBalance> balances;

        ActivityData(List<ActivityItem> items, List<GroupBalance> balances) {
            this.items = items;
            this.balances = balances;
        }
    }

    private void loadActivity() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> new ActivityData(activityManager.getMyActivity(),
                activityManager.getGroupBalances()), data -> {
            binding.progress.setVisibility(View.GONE);
            activityAdapter.submit(data.items);
            binding.tvEmptyActivity.setVisibility(data.items.isEmpty() ? View.VISIBLE : View.GONE);
            showGroupBalances(data.balances);
        }, this::showError);
    }

    private void showGroupBalances(List<GroupBalance> balances) {
        binding.cardGroupBalances.setVisibility(balances.isEmpty() ? View.GONE : View.VISIBLE);
        double toPay = 0.0;
        double toReceive = 0.0;
        for (GroupBalance balance : balances) {
            toPay += balance.getToPay();
            toReceive += balance.getToReceive();
        }
        binding.tvGroupTotals.setText(getString(R.string.group_totals,
                UiUtils.formatMoney(toPay), UiUtils.formatMoney(toReceive)));
        binding.groupBalanceChart.setData(balances);
        boolean settled = binding.groupBalanceChart.isEmpty();
        binding.groupBalanceChart.setVisibility(settled ? View.GONE : View.VISIBLE);
        binding.tvGroupsSettled.setVisibility(settled ? View.VISIBLE : View.GONE);
    }

    private void showAddFriendDialog() {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.friend_email));
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(UiUtils.dp(this, 20), UiUtils.dp(this, 16), UiUtils.dp(this, 20), UiUtils.dp(this, 16));
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_friend)
                .setView(input)
                .setPositiveButton(R.string.add_short, (dialog, which) -> {
                    String email = UiUtils.text(input);
                    binding.progress.setVisibility(View.VISIBLE);
                    UiUtils.launchIo(this, () -> {
                        friendManager.addFriendByEmail(email);
                        return null;
                    }, ignored -> {
                        binding.progress.setVisibility(View.GONE);
                        UiUtils.toast(this, getString(R.string.friend_request_sent));
                    }, this::showError);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void acceptRequest(IncomingRequest item) {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> {
            requestManager.accept(item.getRequest().getId());
            return null;
        }, ignored -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, "Accepted");
            loadRequests();
        }, this::showError);
    }

    private void denyRequest(IncomingRequest item) {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> {
            requestManager.deny(item.getRequest().getId());
            return null;
        }, ignored -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, "Denied");
            loadRequests();
        }, this::showError);
    }

    private void openActivityItem(ActivityItem item) {
        TransactionType type = item.getKind() == ActivityKind.SETTLEMENT
                ? TransactionType.SETTLEMENT : TransactionType.EXPENSE;
        startActivity(new Intent(this, TransactionDetailActivity.class)
                .putExtra(HistoryActivity.EXTRA_TX_ID, item.getId())
                .putExtra(HistoryActivity.EXTRA_TX_TYPE, type.name()));
    }

    private static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {
        private final UiUtils.Callback<Group> onClick;
        private final List<Group> items = new ArrayList<>();

        GroupAdapter(UiUtils.Callback<Group> onClick) {
            this.onClick = onClick;
        }

        void submit(List<Group> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Group group = items.get(position);
            int count = group.getMemberIds().size();
            holder.name.setText(group.getName());
            holder.description.setText(group.getDescription().trim().isEmpty()
                    ? holder.itemView.getContext().getString(R.string.group_fallback)
                    : group.getDescription());
            holder.members.setText(holder.itemView.getResources()
                    .getQuantityString(R.plurals.group_members, count, count));
            holder.avatar.setText(UiUtils.initials(group.getName()));
            holder.itemView.setOnClickListener(v -> onClick.accept(group));
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView avatar;
            final TextView name;
            final TextView description;
            final TextView members;

            VH(View view) {
                super(view);
                avatar = view.findViewById(R.id.tvAvatar);
                name = view.findViewById(R.id.tvName);
                description = view.findViewById(R.id.tvDescription);
                members = view.findViewById(R.id.tvMembers);
            }
        }
    }

    private static class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {
        private final List<FriendBalance> items = new ArrayList<>();

        void submit(List<FriendBalance> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FriendBalance item = items.get(position);
            android.content.Context ctx = holder.itemView.getContext();
            holder.avatar.setText(UiUtils.initials(item.getFriend().getName()));
            holder.name.setText(item.getFriend().getName());
            holder.email.setText(item.getFriend().getEmail());
            if (item.youAreOwed()) {
                holder.balance.setText(ctx.getString(R.string.owes_you, UiUtils.formatMoney(item.getNetAmount())));
                holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.primary));
            } else if (item.youOwe()) {
                holder.balance.setText(ctx.getString(R.string.you_owe_amount, UiUtils.formatMoney(-item.getNetAmount())));
                holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.error));
            } else {
                holder.balance.setText(ctx.getString(R.string.settled_up));
                holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView avatar;
            final TextView name;
            final TextView email;
            final TextView balance;

            VH(View view) {
                super(view);
                avatar = view.findViewById(R.id.tvAvatar);
                name = view.findViewById(R.id.tvName);
                email = view.findViewById(R.id.tvEmail);
                balance = view.findViewById(R.id.tvBalance);
            }
        }
    }

    private static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {
        private final UiUtils.Callback<IncomingRequest> onAccept;
        private final UiUtils.Callback<IncomingRequest> onDeny;
        private final List<IncomingRequest> items = new ArrayList<>();

        RequestAdapter(UiUtils.Callback<IncomingRequest> onAccept, UiUtils.Callback<IncomingRequest> onDeny) {
            this.onAccept = onAccept;
            this.onDeny = onDeny;
        }

        void submit(List<IncomingRequest> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            IncomingRequest item = items.get(position);
            android.content.Context ctx = holder.itemView.getContext();
            holder.kind.setText(item.getRequest().typeEnum() == RequestType.GROUP_INVITE
                    ? ctx.getString(R.string.request_group)
                    : ctx.getString(R.string.request_friend));
            holder.headline.setText(item.headline());
            holder.email.setText(item.getFromEmail());
            holder.btnAccept.setOnClickListener(v -> onAccept.accept(item));
            holder.btnDeny.setOnClickListener(v -> onDeny.accept(item));
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView kind;
            final TextView headline;
            final TextView email;
            final MaterialButton btnAccept;
            final MaterialButton btnDeny;

            VH(View view) {
                super(view);
                kind = view.findViewById(R.id.tvKind);
                headline = view.findViewById(R.id.tvHeadline);
                email = view.findViewById(R.id.tvEmail);
                btnAccept = view.findViewById(R.id.btnAccept);
                btnDeny = view.findViewById(R.id.btnDeny);
            }
        }
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.VH> {
        private final UiUtils.Callback<ActivityItem> onClick;
        private final List<ActivityItem> items = new ArrayList<>();

        ActivityAdapter(UiUtils.Callback<ActivityItem> onClick) {
            this.onClick = onClick;
        }

        void submit(List<ActivityItem> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ActivityItem item = items.get(position);
            holder.kind.setText(item.getKind() == ActivityKind.SETTLEMENT ? "Settlement" : "Expense");
            holder.group.setText(item.getGroupName());
            holder.title.setText(item.getTitle());
            holder.subtitle.setText(item.getSubtitle());
            holder.amount.setText(UiUtils.formatMoney(item.getAmount()));
            holder.date.setText(item.getDate());
            holder.itemView.setOnClickListener(v -> onClick.accept(item));
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView kind;
            final TextView group;
            final TextView title;
            final TextView subtitle;
            final TextView amount;
            final TextView date;

            VH(View view) {
                super(view);
                kind = view.findViewById(R.id.tvKind);
                group = view.findViewById(R.id.tvGroup);
                title = view.findViewById(R.id.tvTitle);
                subtitle = view.findViewById(R.id.tvSubtitle);
                amount = view.findViewById(R.id.tvAmount);
                date = view.findViewById(R.id.tvDate);
            }
        }
    }
}
