package com.splitsmart.app.ui;

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
import com.splitsmart.app.model.Settlement;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records a payment made outside the app. Either side can record it: "I paid" lists people you owe,
 * "I was paid" lists people who owe you. Opened with EXTRA_SETTLEMENT_ID it edits that settlement.
 */
public class CreateSettlementActivity extends AppCompatActivity {
    public static final String EXTRA_SETTLEMENT_ID = "settlementId";
    private static final String STATE_PAY_TO = "payTo";
    private static final String STATE_AMOUNT = "amount";
    private static final String STATE_RECEIVED = "received";

    private ActivityCreateSettlementBinding binding;
    private final SettlementManager settlementManager = SettlementManager.getInstance();
    private final BalanceManager balanceManager = BalanceManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private String groupId;
    private String uid;
    /** Set when editing an existing settlement. */
    private String editingSettlementId;
    /** Open debts from the current user's point of view, in spinner order. */
    private List<Counterparty> owedByMe = Collections.emptyList();
    private List<Counterparty> owedToMe = Collections.emptyList();
    /** Who was selected and what amount was typed before a rotation or theme change. */
    private String restoredPayTo;
    private String restoredAmount;

    /** The other member in a possible settlement and the most that can be recorded. */
    private static class Counterparty {
        final String userId;
        final String name;
        final double max;

        Counterparty(String userId, String name, double max) {
            this.userId = userId;
            this.name = name;
            this.max = max;
        }
    }

    private static class Debts {
        final List<Counterparty> owedByMe;
        final List<Counterparty> owedToMe;

        Debts(List<Counterparty> owedByMe, List<Counterparty> owedToMe) {
            this.owedByMe = owedByMe;
            this.owedToMe = owedToMe;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateSettlementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editingSettlementId = getIntent().getStringExtra(EXTRA_SETTLEMENT_ID);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(
                    editingSettlementId == null ? R.string.create_settlement : R.string.edit_settlement));
        }

        String extra = getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID);
        groupId = extra == null ? "" : extra;
        uid = accountManager.currentUid();
        if (uid == null) {
            UiUtils.toast(this, "Not logged in");
            finish();
            return;
        }

        // The date is recorded automatically (today for a new settlement, unchanged when editing).
        binding.etDate.setText(UiUtils.todayIso());
        binding.etDate.setFocusable(false);
        binding.etDate.setLongClickable(false);
        binding.btnSave.setOnClickListener(v -> save());
        if (savedInstanceState != null) {
            restoredPayTo = savedInstanceState.getString(STATE_PAY_TO);
            restoredAmount = savedInstanceState.getString(STATE_AMOUNT);
            if (savedInstanceState.getBoolean(STATE_RECEIVED)) binding.toggleDirection.check(R.id.btnIWasPaid);
        }

        if (editingSettlementId != null) {
            loadForEdit(savedInstanceState == null);
        } else if (groupId.trim().isEmpty()) {
            UiUtils.toast(this, "Missing group");
            finish();
        } else {
            binding.toggleDirection.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) showCounterparties();
            });
            loadDebts();
        }
    }

    private boolean recordingReceived() {
        return binding.toggleDirection.getCheckedButtonId() == R.id.btnIWasPaid;
    }

    private String nameOf(String userId) {
        User user = userRepository.getById(userId);
        return user != null ? user.getName() : userId;
    }

    private void loadDebts() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> {
            List<Counterparty> byMe = new ArrayList<>();
            List<Counterparty> toMe = new ArrayList<>();
            for (BalanceManager.PairBalance balance : balanceManager.getBalancesForGroup(groupId)) {
                if (balance.getFromUserId().equals(uid)) {
                    byMe.add(new Counterparty(balance.getToUserId(), nameOf(balance.getToUserId()), balance.getAmount()));
                } else if (balance.getToUserId().equals(uid)) {
                    toMe.add(new Counterparty(balance.getFromUserId(), nameOf(balance.getFromUserId()), balance.getAmount()));
                }
            }
            return new Debts(byMe, toMe);
        }, debts -> {
            binding.progress.setVisibility(View.GONE);
            owedByMe = debts.owedByMe;
            owedToMe = debts.owedToMe;
            if (owedByMe.isEmpty() && owedToMe.isEmpty()) {
                UiUtils.toast(this, "No one owes anyone in this group with you");
                finish();
                return;
            }
            // Open on whichever side actually has a debt.
            if (!recordingReceived() && owedByMe.isEmpty()) binding.toggleDirection.check(R.id.btnIWasPaid);
            else if (recordingReceived() && owedToMe.isEmpty()) binding.toggleDirection.check(R.id.btnIPaid);
            showCounterparties();
        }, this::showError);
    }

    private List<Counterparty> currentList() {
        return recordingReceived() ? owedToMe : owedByMe;
    }

    private void showCounterparties() {
        boolean received = recordingReceived();
        binding.tvPersonLabel.setText(received ? R.string.settlement_received_from : R.string.settlement_paid_to);
        List<Counterparty> list = currentList();
        List<String> labels = new ArrayList<>();
        for (Counterparty person : list) {
            labels.add(person.name + (received ? " — owes you " : " — you owe ") + UiUtils.formatMoney(person.max));
        }
        if (labels.isEmpty()) labels.add(received ? "No one owes you in this group" : "You don't owe anyone in this group");
        binding.spPayTo.setAdapter(UiUtils.choiceAdapter(this, labels));
        binding.spPayTo.setEnabled(!list.isEmpty());
        UiUtils.onItemSelected(binding.spPayTo, position -> {
            if (position >= currentList().size()) {
                binding.tvOwed.setText("");
                binding.etAmount.setText("");
                return;
            }
            Counterparty person = currentList().get(position);
            binding.tvOwed.setText((received ? person.name + " owes you " : "You owe " + person.name + " ")
                    + UiUtils.formatMoney(person.max));
            if (restoredAmount != null) {
                binding.etAmount.setText(restoredAmount);
                restoredAmount = null;
            } else {
                binding.etAmount.setText(String.format("%.2f", person.max));
            }
        });
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).userId.equals(restoredPayTo)) binding.spPayTo.setSelection(i);
        }
    }

    /** Edit mode: the people involved stay fixed; only the amount and note can change. */
    private void loadForEdit(boolean fillFields) {
        binding.toggleDirection.setVisibility(View.GONE);
        binding.progress.setVisibility(View.VISIBLE);
        Settlement[] loaded = new Settlement[1];
        UiUtils.launchIo(this, () -> {
            Settlement settlement = settlementManager.getSettlement(editingSettlementId);
            loaded[0] = settlement;
            double owedNow = balanceManager.amountOwed(settlement.getFromUserId(), settlement.getToUserId(),
                    balanceManager.getBalancesForGroup(settlement.getGroupId()));
            return new Counterparty(settlement.getToUserId(),
                    nameOf(settlement.getFromUserId()) + " paid " + nameOf(settlement.getToUserId()),
                    owedNow + settlement.getAmount());
        }, editable -> {
            binding.progress.setVisibility(View.GONE);
            Settlement settlement = loaded[0];
            binding.tvPersonLabel.setText(R.string.edit_settlement);
            binding.spPayTo.setAdapter(UiUtils.choiceAdapter(this, Collections.singletonList(editable.name)));
            binding.spPayTo.setEnabled(false);
            binding.tvOwed.setText("Up to " + UiUtils.formatMoney(editable.max) + " can be recorded");
            binding.etDate.setText(settlement.getDate());
            if (fillFields) {
                binding.etAmount.setText(String.format("%.2f", settlement.getAmount()));
                binding.etNote.setText(settlement.getNote());
            }
        }, message -> {
            UiUtils.toast(this, message);
            finish();
        });
    }

    private void save() {
        Double amount = UiUtils.parseDouble(UiUtils.text(binding.etAmount));
        if (amount == null) {
            UiUtils.toast(this, "Enter a valid amount");
            return;
        }
        String note = UiUtils.text(binding.etNote);
        if (editingSettlementId != null) {
            binding.progress.setVisibility(View.VISIBLE);
            UiUtils.launchIo(this, () -> settlementManager.editSettlement(editingSettlementId, amount, note),
                    settlement -> finishWith("Settlement updated"), this::showError);
            return;
        }

        int position = binding.spPayTo.getSelectedItemPosition();
        if (position < 0 || position >= currentList().size()) {
            UiUtils.toast(this, "Choose who was paid");
            return;
        }
        String other = currentList().get(position).userId;
        String from = recordingReceived() ? other : uid;
        String to = recordingReceived() ? uid : other;
        String date = UiUtils.todayIso();
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> settlementManager.createSettlement(groupId, from, to, amount, date, note),
                settlement -> finishWith("Settlement recorded"), this::showError);
    }

    private void finishWith(String message) {
        binding.progress.setVisibility(View.GONE);
        UiUtils.toast(this, message);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding == null) return;
        outState.putBoolean(STATE_RECEIVED, recordingReceived());
        int position = binding.spPayTo.getSelectedItemPosition();
        boolean loaded = editingSettlementId == null && position >= 0 && position < currentList().size();
        outState.putString(STATE_PAY_TO, loaded ? currentList().get(position).userId : restoredPayTo);
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
