package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.BalanceManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityBalanceBinding;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupBalanceFragment extends GroupTabFragment {
    private ActivityBalanceBinding binding;
    private UiTask loading;
    private final BalanceManager balanceManager = BalanceManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final Map<String, String> names = new HashMap<>();
    private final List<BalanceManager.PairBalance> items = new ArrayList<>();
    private final RecyclerView.Adapter<BalanceHolder> adapter = new RecyclerView.Adapter<BalanceHolder>() {
        @NonNull
        @Override
        public BalanceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BalanceHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_balance, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull BalanceHolder holder, int position) {
            BalanceManager.PairBalance balance = items.get(position);
            String from = nameOf(balance.getFromUserId());
            String to = nameOf(balance.getToUserId());
            holder.description.setText(getString(R.string.balance_direction, from, to));
            holder.amount.setText(UiUtils.formatMoney(balance.getAmount()));
            holder.avatar.setText(UiUtils.initials(from));
        }
    };

    /** Everything one balance load produces, handed to the main thread together. */
    private static class BalanceData {
        final List<BalanceManager.PairBalance> balances;
        final BalanceManager.UserBalanceSummary summary;
        final Map<String, String> labels;

        BalanceData(List<BalanceManager.PairBalance> balances, BalanceManager.UserBalanceSummary summary,
                    Map<String, String> labels) {
            this.balances = balances;
            this.summary = summary;
            this.labels = labels;
        }
    }

    public GroupBalanceFragment() {
        super(R.layout.activity_balance);
    }

    private String nameOf(String uid) {
        String name = names.get(uid);
        return name != null ? name : uid;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = ActivityBalanceBinding.bind(view);
        binding.rvBalances.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBalances.setAdapter(adapter);
        binding.btnCreateSettlement.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateSettlementActivity.class)
                        .putExtra(HomeActivity.EXTRA_GROUP_ID, getGroupId())));
    }

    @Override
    public void refresh() {
        String uid = accountManager.currentUid();
        if (uid == null) return;
        String groupId = getGroupId();
        if (loading != null) loading.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        loading = UiUtils.loadData(this, () -> {
            List<BalanceManager.PairBalance> balances = balanceManager.getBalancesForGroup(groupId);
            Set<String> userIds = new LinkedHashSet<>();
            for (BalanceManager.PairBalance balance : balances) {
                userIds.add(balance.getFromUserId());
                userIds.add(balance.getToUserId());
            }
            Map<String, String> labels = new HashMap<>();
            for (String id : userIds) {
                User user = userRepository.getById(id);
                labels.put(id, user != null ? user.getName() : id);
            }
            return new BalanceData(balances, balanceManager.summarizeForUser(uid, balances), labels);
        }, data -> {
            names.clear();
            names.putAll(data.labels);
            items.clear();
            items.addAll(data.balances);
            adapter.notifyDataSetChanged();
            binding.tvToPay.setText(UiUtils.formatMoney(data.summary.getTotalToPay()));
            binding.tvToReceive.setText(UiUtils.formatMoney(data.summary.getTotalToReceive()));
            binding.tvEmpty.setVisibility(data.balances.isEmpty() ? View.VISIBLE : View.GONE);
            binding.progress.setVisibility(View.GONE);
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(requireContext(), message);
        });
    }

    @Override
    public void onDestroyView() {
        binding.rvBalances.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private static class BalanceHolder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView description;
        final TextView amount;

        BalanceHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.tvAvatar);
            description = view.findViewById(R.id.tvBalance);
            amount = view.findViewById(R.id.tvBalanceAmount);
        }
    }
}
