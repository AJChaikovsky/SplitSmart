package com.splitsmart.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.BalanceManager;
import com.splitsmart.app.business.SettlementManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityCreateSettlementBinding;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CreateSettlementActivity extends AppCompatActivity {
    private ActivityCreateSettlementBinding binding;
    private final SettlementManager settlementManager = SettlementManager.getInstance();
    private final BalanceManager balanceManager = BalanceManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private static final String STATE_PAY_TO = "payTo";
    private static final String STATE_AMOUNT = "amount";

    private String groupId;
    /** Who was selected and what amount was typed before a rotation or theme change. */
    private String restoredPayTo;
    private String restoredAmount;
    /** Debts the current user owes, in spinner order. */
    private List<BalanceManager.PairBalance> creditors = Collections.emptyList();

    private static class CreditorData {
        final List<BalanceManager.PairBalance> creditors;
        final List<String> labels;

        CreditorData(List<BalanceManager.PairBalance> creditors, List<String> labels) {
            this.creditors = creditors;
            this.labels = labels;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateSettlementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.create_settlement));
        }

        String extra = getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID);
        groupId = extra == null ? "" : extra;
        if (groupId.trim().isEmpty()) {
            UiUtils.toast(this, "Missing group");
            finish();
            return;
        }
        binding.etDate.setText(UiUtils.todayIso());
        binding.etDate.setFocusable(false);
        binding.etDate.setClickable(true);
        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> save());
        if (savedInstanceState != null) {
            restoredPayTo = savedInstanceState.getString(STATE_PAY_TO);
            restoredAmount = savedInstanceState.getString(STATE_AMOUNT);
        }
        loadCreditors();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        String current = UiUtils.text(binding.etDate);
        if (current.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = current.split("-");
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        }
        new DatePickerDialog(
                this,
                (picker, year, month, day) ->
                        binding.etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadCreditors() {
        String uid = accountManager.currentUid();
        if (uid == null) {
            UiUtils.toast(this, "Not logged in");
            finish();
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> {
            List<BalanceManager.PairBalance> owed = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (BalanceManager.PairBalance balance : balanceManager.getBalancesForGroup(groupId)) {
                if (!balance.getFromUserId().equals(uid)) continue;
                owed.add(balance);
                User user = userRepository.getById(balance.getToUserId());
                String name = user != null ? user.getName() : balance.getToUserId();
                labels.add(name + " — owed " + UiUtils.formatMoney(balance.getAmount()));
            }
            return new CreditorData(owed, labels);
        }, data -> {
            binding.progress.setVisibility(View.GONE);
            creditors = data.creditors;
            if (data.labels.isEmpty()) {
                UiUtils.toast(this, "You don't owe anyone in this group");
                finish();
                return;
            }
            binding.spPayTo.setAdapter(UiUtils.choiceAdapter(this, data.labels));
            UiUtils.onItemSelected(binding.spPayTo, position -> {
                double owed = creditors.get(position).getAmount();
                binding.tvOwed.setText("You owe " + UiUtils.formatMoney(owed));
                if (restoredAmount != null) {
                    binding.etAmount.setText(restoredAmount);
                    restoredAmount = null;
                } else {
                    binding.etAmount.setText(String.format("%.2f", owed));
                }
            });
            for (int i = 0; i < creditors.size(); i++) {
                if (creditors.get(i).getToUserId().equals(restoredPayTo)) binding.spPayTo.setSelection(i);
            }
        }, this::showError);
    }

    private void save() {
        if (creditors.isEmpty()) return;
        String toUserId = creditors.get(binding.spPayTo.getSelectedItemPosition()).getToUserId();
        Double amount = UiUtils.parseDouble(UiUtils.text(binding.etAmount));
        if (amount == null) {
            UiUtils.toast(this, "Enter a valid amount");
            return;
        }
        String date = UiUtils.text(binding.etDate);
        String note = UiUtils.text(binding.etNote);
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> settlementManager.createSettlement(groupId, toUserId, amount, date, note),
                settlement -> {
                    binding.progress.setVisibility(View.GONE);
                    UiUtils.toast(this, "Settlement recorded");
                    finish();
                }, this::showError);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int position = binding.spPayTo.getSelectedItemPosition();
        boolean loaded = position >= 0 && position < creditors.size();
        outState.putString(STATE_PAY_TO, loaded ? creditors.get(position).getToUserId() : restoredPayTo);
        outState.putString(STATE_AMOUNT, loaded ? UiUtils.text(binding.etAmount) : restoredAmount);
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
