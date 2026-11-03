package com.splitsmart.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.splitsmart.app.R;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.business.HistoryManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityHistoryBinding;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.Transaction;
import com.splitsmart.app.model.TransactionType;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupHistoryFragment extends GroupTabFragment {
    private ActivityHistoryBinding binding;
    private UiTask loading;
    private final HistoryManager historyManager = HistoryManager.getInstance();
    private final GroupManager groupManager = GroupManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private List<User> members = Collections.emptyList();
    private String restoredMemberId = null;
    private final List<Transaction> items = new ArrayList<>();
    private final RecyclerView.Adapter<TransactionHolder> adapter = new RecyclerView.Adapter<TransactionHolder>() {
        @NonNull
        @Override
        public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new TransactionHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
            Transaction transaction = items.get(position);
            boolean settlement = transaction.getType() == TransactionType.SETTLEMENT;
            holder.title.setText(settlement ? getString(R.string.settlement_label) : transaction.getTitle());
            holder.date.setText(transaction.getDate());
            holder.amount.setText(UiUtils.formatMoney(transaction.getAmount()));
            String category = transaction.getCategory().trim().isEmpty() ? "OTHER" : transaction.getCategory();
            holder.category.setText(settlement ? getString(R.string.settlement_label) : UiUtils.displayChoice(category));
            holder.icon.setImageResource(settlement ? R.drawable.ic_swap : categoryIcon(transaction.getCategory()));
            holder.itemView.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), TransactionDetailActivity.class)
                            .putExtra(HistoryActivity.EXTRA_TX_ID, transaction.getId())
                            .putExtra(HistoryActivity.EXTRA_TX_TYPE, transaction.getType().name())
                            .putExtra(HomeActivity.EXTRA_GROUP_ID, getGroupId())));
        }
    };

    /** Group members plus the matching transactions, loaded together. */
    private static class HistoryData {
        final List<User> users;
        final List<Transaction> transactions;

        HistoryData(List<User> users, List<Transaction> transactions) {
            this.users = users;
            this.transactions = transactions;
        }
    }

    public GroupHistoryFragment() {
        super(R.layout.activity_history);
    }

    private static int categoryIcon(String category) {
        switch (ExpenseCategory.fromString(category)) {
            case FOOD: return R.drawable.ic_food;
            case HOUSING: return R.drawable.ic_house;
            case TRANSPORTATION: return R.drawable.ic_car;
            case ENTERTAINMENT: return R.drawable.ic_play;
            case OTHER:
            default: return R.drawable.ic_receipt;
        }
    }

    private static List<String> withAll(Enum<?>[] values) {
        List<String> names = new ArrayList<>();
        names.add("ALL");
        for (Enum<?> value : values) names.add(value.name());
        return names;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = ActivityHistoryBinding.bind(view);
        Context context = requireContext();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(context));
        binding.rvHistory.setAdapter(adapter);
        binding.spType.setAdapter(UiUtils.choiceAdapter(context, withAll(TransactionType.values())));
        binding.spCategory.setAdapter(UiUtils.choiceAdapter(context, withAll(ExpenseCategory.values())));
        binding.spSort.setAdapter(UiUtils.choiceAdapter(context, Arrays.asList("NEWEST", "OLDEST")));
        binding.spMember.setAdapter(UiUtils.choiceAdapter(context,
                Collections.singletonList(getString(R.string.all_members))));
        binding.btnToggleFilters.setOnClickListener(v -> {
            boolean expanded = binding.layoutFilters.getVisibility() != View.VISIBLE;
            binding.layoutFilters.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.btnToggleFilters.setSelected(expanded);
        });
        binding.btnApply.setOnClickListener(v -> {
            binding.layoutFilters.setVisibility(View.GONE);
            binding.btnToggleFilters.setSelected(false);
            refresh();
        });
        binding.etSearch.setOnEditorActionListener((textView, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            refresh();
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
            return true;
        });
        if (savedInstanceState != null) {
            restoredMemberId = savedInstanceState.getString("memberId");
            binding.etSearch.setText(savedInstanceState.getString("keyword", ""));
            binding.spType.setSelection(savedInstanceState.getInt("type", 0));
            binding.spCategory.setSelection(savedInstanceState.getInt("category", 0));
            binding.spSort.setSelection(savedInstanceState.getInt("sort", 0));
            boolean expanded = savedInstanceState.getBoolean("filtersExpanded", false);
            binding.layoutFilters.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.btnToggleFilters.setSelected(expanded);
        }
    }

    private String selectedMemberId() {
        int memberIndex = binding.spMember.getSelectedItemPosition() - 1;
        if (memberIndex >= 0 && memberIndex < members.size()) return members.get(memberIndex).getUid();
        return restoredMemberId;
    }

    @Override
    public void refresh() {
        Object type = binding.spType.getSelectedItem();
        Object category = binding.spCategory.getSelectedItem();
        HistoryManager.HistoryFilter filter = new HistoryManager.HistoryFilter(
                UiUtils.text(binding.etSearch),
                type == null || "ALL".equals(type.toString()) ? null : TransactionType.valueOf(type.toString()),
                category == null || "ALL".equals(category.toString()) ? null : ExpenseCategory.valueOf(category.toString()),
                selectedMemberId(),
                binding.spSort.getSelectedItem() != null && "OLDEST".equals(binding.spSort.getSelectedItem().toString())
                        ? HistoryManager.SortOrder.OLDEST : HistoryManager.SortOrder.NEWEST
        );
        String groupId = getGroupId();
        if (loading != null) loading.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        loading = UiUtils.loadData(this, () -> {
            List<User> users = new ArrayList<>();
            for (String uid : groupManager.getGroup(groupId).getMemberIds()) {
                User user = userRepository.getById(uid);
                if (user != null) users.add(user);
            }
            // Ignore a remembered member filter for someone who has since left the group.
            String memberId = null;
            for (User user : users) {
                if (user.getUid().equals(filter.getMemberId())) memberId = user.getUid();
            }
            return new HistoryData(users, historyManager.getHistory(groupId, filter.withMemberId(memberId)));
        }, data -> {
            members = data.users;
            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.all_members));
            int selected = 0;
            for (int i = 0; i < data.users.size(); i++) {
                User user = data.users.get(i);
                labels.add(user.getName() + " (" + user.getEmail() + ")");
                if (user.getUid().equals(filter.getMemberId())) selected = i + 1;
            }
            binding.spMember.setAdapter(UiUtils.choiceAdapter(requireContext(), labels));
            binding.spMember.setSelection(selected);
            restoredMemberId = null;
            items.clear();
            items.addAll(data.transactions);
            adapter.notifyDataSetChanged();
            binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.progress.setVisibility(View.GONE);
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(requireContext(), message);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            outState.putString("keyword", UiUtils.text(binding.etSearch));
            outState.putInt("type", Math.max(0, binding.spType.getSelectedItemPosition()));
            outState.putInt("category", Math.max(0, binding.spCategory.getSelectedItemPosition()));
            outState.putInt("sort", Math.max(0, binding.spSort.getSelectedItemPosition()));
            outState.putString("memberId", selectedMemberId());
            outState.putBoolean("filtersExpanded", binding.layoutFilters.getVisibility() == View.VISIBLE);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        binding.rvHistory.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private static class TransactionHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView date;
        final TextView amount;
        final TextView category;

        TransactionHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.ivCategory);
            title = view.findViewById(R.id.tvTitle);
            date = view.findViewById(R.id.tvMeta);
            amount = view.findViewById(R.id.tvAmount);
            category = view.findViewById(R.id.tvCategory);
        }
    }
}
