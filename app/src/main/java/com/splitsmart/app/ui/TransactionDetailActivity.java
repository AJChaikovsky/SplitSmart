package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.ExpenseManager;
import com.splitsmart.app.business.SettlementManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityTransactionDetailBinding;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.Settlement;
import com.splitsmart.app.model.TransactionType;
import com.splitsmart.app.model.User;

public class TransactionDetailActivity extends AppCompatActivity {
    private ActivityTransactionDetailBinding binding;
    private final ExpenseManager expenseManager = ExpenseManager.getInstance();
    private final SettlementManager settlementManager = SettlementManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Transaction");
        }

        String id = getIntent().getStringExtra(HistoryActivity.EXTRA_TX_ID);
        String type = getIntent().getStringExtra(HistoryActivity.EXTRA_TX_TYPE);
        if (id == null || id.trim().isEmpty() || type == null || type.trim().isEmpty()) {
            UiUtils.toast(this, "Missing transaction");
            finish();
            return;
        }

        if (TransactionType.EXPENSE.name().equals(type)) {
            loadExpense(id);
        } else if (TransactionType.SETTLEMENT.name().equals(type)) {
            loadSettlement(id);
        } else {
            UiUtils.toast(this, "Unknown transaction type");
            finish();
        }
    }

    private String nameOf(String uid) {
        User user = userRepository.getById(uid);
        return user != null ? user.getName() : uid;
    }

    private void closeWithError(String message) {
        UiUtils.toast(this, message);
        finish();
    }

    private void loadExpense(String id) {
        String[] payer = new String[1];
        UiUtils.launchIo(this, () -> {
            Expense expense = expenseManager.getExpense(id);
            payer[0] = nameOf(expense.getPayerId());
            return expense;
        }, expense -> {
            binding.tvTitle.setText(expense.getTitle());
            StringBuilder details = new StringBuilder()
                    .append("Type: Expense\n")
                    .append("Amount: ").append(UiUtils.formatMoney(expense.getAmount())).append('\n')
                    .append("Date: ").append(expense.getDate()).append('\n')
                    .append("Category: ").append(expense.getCategory()).append('\n')
                    .append("Payer: ").append(payer[0]).append('\n')
                    .append("Split: ").append(expense.getSplitMethod()).append('\n');
            if (!expense.getNote().trim().isEmpty()) details.append("Note: ").append(expense.getNote()).append('\n');
            binding.tvDetails.setText(details.toString());
            binding.btnOpenExpense.setVisibility(View.VISIBLE);
            binding.btnOpenExpense.setOnClickListener(v ->
                    startActivity(new Intent(this, ExpenseDetailActivity.class)
                            .putExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID, expense.getId())));
        }, this::closeWithError);
    }

    private void loadSettlement(String id) {
        String[] names = new String[2];
        UiUtils.launchIo(this, () -> {
            Settlement settlement = settlementManager.getSettlement(id);
            names[0] = nameOf(settlement.getFromUserId());
            names[1] = nameOf(settlement.getToUserId());
            return settlement;
        }, settlement -> {
            boolean isCreator = settlement.getCreatorId().equals(accountManager.currentUid());
            binding.tvTitle.setText("Settlement");
            StringBuilder details = new StringBuilder()
                    .append("Type: Settlement\n")
                    .append("From: ").append(names[0]).append('\n')
                    .append("To: ").append(names[1]).append('\n')
                    .append("Amount: ").append(UiUtils.formatMoney(settlement.getAmount())).append('\n')
                    .append("Date: ").append(settlement.getDate()).append('\n');
            if (!settlement.getNote().trim().isEmpty()) {
                details.append("Note: ").append(settlement.getNote()).append('\n');
            }
            binding.tvDetails.setText(details.toString());
            if (isCreator) {
                binding.btnDeleteSettlement.setVisibility(View.VISIBLE);
                binding.btnDeleteSettlement.setOnClickListener(v ->
                        UiUtils.confirm(this, "Delete settlement", "Delete this settlement?", () ->
                                UiUtils.launchIo(this, () -> {
                                    settlementManager.deleteSettlement(settlement.getId());
                                    return null;
                                }, ignored -> {
                                    UiUtils.toast(this, "Deleted");
                                    finish();
                                })));
            } else {
                binding.btnDeleteSettlement.setVisibility(View.GONE);
            }
        }, this::closeWithError);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
