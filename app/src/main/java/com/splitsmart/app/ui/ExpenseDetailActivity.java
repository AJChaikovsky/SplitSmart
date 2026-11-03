package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.ExpenseManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityExpenseDetailBinding;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.User;

public class ExpenseDetailActivity extends AppCompatActivity {
    private ActivityExpenseDetailBinding binding;
    private final ExpenseManager expenseManager = ExpenseManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();
    private String expenseId;
    private Expense expense;

    /** The expense plus names resolved off the main thread. */
    private static class ExpenseView {
        final Expense expense;
        final String payerName;
        final String shareLines;

        ExpenseView(Expense expense, String payerName, String shareLines) {
            this.expense = expense;
            this.payerName = payerName;
            this.shareLines = shareLines;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpenseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Expense");
        }

        String extra = getIntent().getStringExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID);
        expenseId = extra == null ? "" : extra;
        if (expenseId.trim().isEmpty()) {
            UiUtils.toast(this, "Missing expense");
            finish();
            return;
        }

        binding.btnEdit.setOnClickListener(v -> {
            if (expense == null) return;
            startActivity(new Intent(this, CreateExpenseActivity.class)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, expense.getGroupId())
                    .putExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID, expense.getId()));
        });
        binding.btnDelete.setOnClickListener(v ->
                UiUtils.confirm(this, "Delete expense", "Are you sure you want to delete this expense?", () -> {
                    binding.progress.setVisibility(View.VISIBLE);
                    UiUtils.launchIo(this, () -> {
                        expenseManager.deleteExpense(expenseId);
                        return null;
                    }, ignored -> {
                        binding.progress.setVisibility(View.GONE);
                        UiUtils.toast(this, "Deleted");
                        finish();
                    }, this::showError);
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && !expenseId.trim().isEmpty()) load();
    }

    private String nameOf(String uid) {
        User user = userRepository.getById(uid);
        return user != null ? user.getName() : uid;
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> {
            Expense loaded = expenseManager.getExpense(expenseId);
            StringBuilder shares = new StringBuilder();
            for (ExpenseShare share : loaded.getShares()) {
                if (shares.length() > 0) shares.append('\n');
                shares.append(nameOf(share.getUserId())).append(": ").append(UiUtils.formatMoney(share.getAmount()));
            }
            return new ExpenseView(loaded, nameOf(loaded.getPayerId()), shares.toString());
        }, view -> {
            expense = view.expense;
            Expense e = view.expense;
            boolean isCreator = e.getCreatorId().equals(accountManager.currentUid());
            binding.progress.setVisibility(View.GONE);
            binding.tvTitle.setText(e.getTitle());
            binding.tvAmount.setText(UiUtils.formatMoney(e.getAmount()));
            StringBuilder details = new StringBuilder()
                    .append("Date: ").append(e.getDate()).append('\n')
                    .append("Category: ").append(e.getCategory()).append('\n')
                    .append("Split: ").append(e.getSplitMethod()).append('\n')
                    .append("Payer: ").append(view.payerName).append('\n');
            if (!e.getNote().trim().isEmpty()) details.append("Note: ").append(e.getNote()).append('\n');
            binding.tvDetails.setText(details.toString());
            binding.tvShares.setText(view.shareLines);
            binding.btnEdit.setVisibility(isCreator ? View.VISIBLE : View.GONE);
            binding.btnDelete.setVisibility(isCreator ? View.VISIBLE : View.GONE);
        }, this::showError);
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        UiUtils.toast(this, message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
