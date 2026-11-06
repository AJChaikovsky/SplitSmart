package com.splitsmart.app.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.splitsmart.app.R;
import com.splitsmart.app.business.ExpenseManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ActivityCreateExpenseBinding;
import com.splitsmart.app.model.Expense;
import com.splitsmart.app.model.ExpenseCategory;
import com.splitsmart.app.model.ExpenseShare;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateExpenseActivity extends AppCompatActivity {
    public static final String EXTRA_EXPENSE_ID = "expenseId";
    private static final String STATE_PARTICIPANTS = "participants";
    private static final String STATE_PAYER = "payerId";
    private static final String STATE_SPLIT_VALUES = "splitValues";

    private ActivityCreateExpenseBinding binding;
    private final ExpenseManager expenseManager = ExpenseManager.getInstance();
    private final GroupManager groupManager = GroupManager.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private String groupId;
    private List<User> members = Collections.emptyList();
    private final Map<String, CheckBox> participantChecks = new LinkedHashMap<>();
    private final Map<String, EditText> valueInputs = new LinkedHashMap<>();
    private String editingExpenseId;
    /** Saved form state after a rotation or theme change; null on a fresh open. */
    private Bundle restoredState;
    /** Per-person split values to put back once the inputs are rebuilt. */
    private final Map<String, String> pendingSplitValues = new HashMap<>();
    private SplitMethod valuesMethod;

    /** Group members plus the expense being edited (null when creating). */
    private static class FormData {
        final List<User> members;
        final Expense existing;

        FormData(List<User> members, Expense existing) {
            this.members = members;
            this.existing = existing;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String extra = getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID);
        groupId = extra == null ? "" : extra;
        editingExpenseId = getIntent().getStringExtra(EXTRA_EXPENSE_ID);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(editingExpenseId == null ? getString(R.string.add_expense) : "Edit Expense");
        }

        // The creation date is generated automatically and can't be changed; edits keep the original date.
        binding.etDate.setText(UiUtils.todayIso());
        binding.etDate.setFocusable(false);
        binding.etDate.setLongClickable(false);

        binding.spCategory.setAdapter(UiUtils.choiceAdapter(this, enumNames(ExpenseCategory.values())));
        binding.spCategory.setSelection(ExpenseCategory.OTHER.ordinal());

        binding.spSplitMethod.setAdapter(UiUtils.choiceAdapter(this, enumNames(SplitMethod.values())));

        UiUtils.onItemSelected(binding.spSplitMethod, position -> {
            rebuildSplitValueInputs();
            updateSharePreview();
        });
        UiUtils.onItemSelected(binding.spPayer, position -> {
            lockPayerAsParticipant();
            rebuildSplitValueInputs();
            updateSharePreview();
        });

        binding.btnSave.setOnClickListener(v -> save());
        restoredState = savedInstanceState;
        if (restoredState != null) {
            Bundle values = restoredState.getBundle(STATE_SPLIT_VALUES);
            if (values != null) {
                for (String uid : values.keySet()) pendingSplitValues.put(uid, values.getString(uid));
            }
        }
        loadMembers();
    }

    private static List<String> enumNames(Enum<?>[] values) {
        List<String> names = new ArrayList<>();
        for (Enum<?> value : values) names.add(value.name());
        return names;
    }

    private static String label(User user) {
        return user.getName() + " (" + user.getEmail() + ")";
    }

    private void loadMembers() {
        if (groupId.trim().isEmpty()) {
            UiUtils.toast(this, "Missing group");
            finish();
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        String expenseId = editingExpenseId;
        UiUtils.launchIo(this, () -> {
            Group group = groupManager.getGroup(groupId);
            List<User> users = new ArrayList<>();
            for (String uid : group.getMemberIds()) {
                User user = userRepository.getById(uid);
                if (user != null) users.add(user);
            }
            Expense existing = expenseId == null ? null : expenseManager.getExpense(expenseId);
            return new FormData(users, existing);
        }, data -> {
            binding.progress.setVisibility(View.GONE);
            members = data.members;
            Expense existing = data.existing;
            List<String> labels = new ArrayList<>();
            for (User user : members) labels.add(label(user));
            binding.spPayer.setAdapter(UiUtils.choiceAdapter(this, labels));
            binding.llParticipants.removeAllViews();
            participantChecks.clear();
            List<String> restoredParticipants = restoredState != null
                    ? restoredState.getStringArrayList(STATE_PARTICIPANTS) : null;
            for (User user : members) {
                MaterialCheckBox checkBox = new MaterialCheckBox(this);
                checkBox.setText(label(user));
                if (restoredParticipants != null) {
                    checkBox.setChecked(restoredParticipants.contains(user.getUid()));
                } else {
                    checkBox.setChecked(existing == null || existing.getParticipantIds().contains(user.getUid()));
                }
                checkBox.setOnCheckedChangeListener((button, checked) -> {
                    String payerId = currentPayerId();
                    if (payerId != null && user.getUid().equals(payerId) && !checked) {
                        // Payer cannot be deselected.
                        button.setChecked(true);
                        UiUtils.toast(this, "Payer must remain a participant");
                        return;
                    }
                    rebuildSplitValueInputs();
                    updateSharePreview();
                });
                participantChecks.put(user.getUid(), checkBox);
                binding.llParticipants.addView(checkBox);
            }
            // Text fields and the category/split spinners restore themselves after a rotation;
            // only fill them from the saved expense on a fresh open.
            if (existing != null && restoredState == null) {
                binding.etTitle.setText(existing.getTitle());
                binding.etAmount.setText(String.valueOf(existing.getAmount()));
                binding.etDate.setText(existing.getDate());
                binding.etNote.setText(existing.getNote());
                binding.spCategory.setSelection(existing.categoryEnum().ordinal());
                binding.spSplitMethod.setSelection(existing.splitMethodEnum().ordinal());
            }
            String payerId = restoredState != null ? restoredState.getString(STATE_PAYER)
                    : existing != null ? existing.getPayerId() : null;
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getUid().equals(payerId)) {
                    binding.spPayer.setSelection(i);
                    break;
                }
            }
            lockPayerAsParticipant();
            rebuildSplitValueInputs();
            updateSharePreview();
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, message);
        });
    }


    private String currentPayerId() {
        int position = binding.spPayer.getSelectedItemPosition();
        if (members.isEmpty() || position < 0) return null;
        return members.get(position).getUid();
    }

    private void lockPayerAsParticipant() {
        String payerId = currentPayerId();
        if (payerId == null) return;
        CheckBox payerCheck = participantChecks.get(payerId);
        if (payerCheck != null) {
            payerCheck.setChecked(true);
            payerCheck.setEnabled(false);
        }
        for (Map.Entry<String, CheckBox> entry : participantChecks.entrySet()) {
            if (!entry.getKey().equals(payerId)) entry.getValue().setEnabled(true);
        }
    }

    private List<String> selectedParticipants() {
        String payerId = currentPayerId();
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : participantChecks.entrySet()) {
            if (entry.getValue().isChecked()) selected.add(entry.getKey());
        }
        if (payerId != null && !selected.contains(payerId)) selected.add(0, payerId);
        return selected;
    }

    private SplitMethod selectedMethod() {
        int position = binding.spSplitMethod.getSelectedItemPosition();
        return position < 0 ? null : SplitMethod.values()[position];
    }

    private User memberById(String uid) {
        for (User user : members) {
            if (user.getUid().equals(uid)) return user;
        }
        return null;
    }

    private String nameOf(String uid) {
        User user = memberById(uid);
        return user != null ? user.getName() : uid;
    }

    private void rebuildSplitValueInputs() {
        SplitMethod method = selectedMethod();
        // Keep what was typed when only the participants change (not when the split method changes).
        Map<String, String> keep = new HashMap<>();
        if (method == valuesMethod) {
            for (Map.Entry<String, EditText> entry : valueInputs.entrySet()) {
                keep.put(entry.getKey(), UiUtils.text(entry.getValue()));
            }
        }
        keep.putAll(pendingSplitValues);
        binding.llSplitValues.removeAllViews();
        valueInputs.clear();
        valuesMethod = method;
        if (method == null) return;
        if (method == SplitMethod.EQUAL) {
            updateSharePreview();
            return;
        }

        TextView label = new TextView(this);
        label.setText(method == SplitMethod.EXACT ? "Exact amounts" : "Percentages");
        label.setTextAppearance(R.style.TextAppearance_SplitSmart_Section);
        label.setPadding(0, UiUtils.dp(this, 16), 0, UiUtils.dp(this, 8));
        binding.llSplitValues.addView(label);

        for (String uid : selectedParticipants()) {
            User user = memberById(uid);
            if (user == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiUtils.dp(this, 4), 0, UiUtils.dp(this, 4));

            TextView name = new TextView(this);
            name.setText(user.getName());
            name.setTextAppearance(R.style.TextAppearance_SplitSmart_Body);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            EditText input = new EditText(this);
            input.setBackgroundResource(R.drawable.background_card);
            int padding = UiUtils.dp(this, 12);
            input.setPadding(padding, padding, padding, padding);
            input.setMinHeight(UiUtils.dp(this, 48));
            input.setTextSize(14f);
            input.setHint(method == SplitMethod.EXACT ? "0.00" : "%");
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (keep.containsKey(uid)) input.setText(keep.get(uid));
            input.setOnFocusChangeListener((v, hasFocus) -> updateSharePreview());

            valueInputs.put(uid, input);
            row.addView(name);
            row.addView(input);
            binding.llSplitValues.addView(row);
        }
        if (!valueInputs.isEmpty()) pendingSplitValues.clear();
        updateSharePreview();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (participantChecks.isEmpty()) {
            // Members haven't loaded yet: carry the previous saved state forward.
            if (restoredState != null) {
                outState.putStringArrayList(STATE_PARTICIPANTS, restoredState.getStringArrayList(STATE_PARTICIPANTS));
                outState.putString(STATE_PAYER, restoredState.getString(STATE_PAYER));
            }
        } else {
            ArrayList<String> participants = new ArrayList<>();
            for (Map.Entry<String, CheckBox> entry : participantChecks.entrySet()) {
                if (entry.getValue().isChecked()) participants.add(entry.getKey());
            }
            outState.putStringArrayList(STATE_PARTICIPANTS, participants);
            outState.putString(STATE_PAYER, currentPayerId());
        }
        Bundle values = new Bundle();
        for (Map.Entry<String, String> entry : pendingSplitValues.entrySet()) values.putString(entry.getKey(), entry.getValue());
        for (Map.Entry<String, EditText> entry : valueInputs.entrySet()) {
            values.putString(entry.getKey(), UiUtils.text(entry.getValue()));
        }
        outState.putBundle(STATE_SPLIT_VALUES, values);
    }

    /** Reads the per-person inputs as a userId -> value map (blank or invalid counts as 0). */
    private Map<String, Double> inputValues() {
        Map<String, Double> values = new HashMap<>();
        for (Map.Entry<String, EditText> entry : valueInputs.entrySet()) {
            Double value = UiUtils.parseDouble(UiUtils.text(entry.getValue()));
            values.put(entry.getKey(), value != null ? value : 0.0);
        }
        return values;
    }

    private Map<String, Double> exactAmounts(SplitMethod method) {
        return method == SplitMethod.EXACT ? inputValues() : Collections.emptyMap();
    }

    private Map<String, Double> percentages(SplitMethod method) {
        return method == SplitMethod.PERCENTAGE ? inputValues() : Collections.emptyMap();
    }

    private List<ExpenseShare> computePreviewShares() {
        Double amount = UiUtils.parseDouble(UiUtils.text(binding.etAmount));
        SplitMethod method = selectedMethod();
        if (amount == null || method == null) return null;
        List<String> participants = selectedParticipants();
        if (participants.size() < 2) return null;
        try {
            return expenseManager.computeShares(amount, participants, method,
                    exactAmounts(method), percentages(method));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void updateSharePreview() {
        List<ExpenseShare> shares = computePreviewShares();
        View existing = binding.getRoot().findViewWithTag("sharePreview");
        if (existing != null) binding.llSplitValues.removeView(existing);
        if (shares == null) return;

        TextView preview = new TextView(this);
        preview.setTag("sharePreview");
        preview.setTextAppearance(R.style.TextAppearance_SplitSmart_Body);
        preview.setBackgroundResource(R.drawable.background_preview);
        int padding = UiUtils.dp(this, 16);
        preview.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = UiUtils.dp(this, 16);
        preview.setLayoutParams(params);

        StringBuilder text = new StringBuilder("Assigned shares:\n");
        for (ExpenseShare share : shares) {
            text.append("• ").append(nameOf(share.getUserId())).append(": ")
                    .append(UiUtils.formatMoney(share.getAmount())).append('\n');
        }
        preview.setText(text.toString());
        binding.llSplitValues.addView(preview);
    }

    private void save() {
        String title = UiUtils.text(binding.etTitle);
        Double amount = UiUtils.parseDouble(UiUtils.text(binding.etAmount));
        if (amount == null) {
            UiUtils.toast(this, "Enter a valid amount");
            return;
        }
        // New expenses get today's date; an edited expense keeps its original creation date.
        String date = editingExpenseId == null ? UiUtils.todayIso() : UiUtils.text(binding.etDate);
        String note = UiUtils.text(binding.etNote);
        ExpenseCategory category = ExpenseCategory.values()[binding.spCategory.getSelectedItemPosition()];
        SplitMethod method = SplitMethod.values()[binding.spSplitMethod.getSelectedItemPosition()];
        List<String> participants = selectedParticipants();
        if (members.isEmpty() || binding.spPayer.getSelectedItemPosition() < 0) {
            UiUtils.toast(this, "No members loaded");
            return;
        }
        String payerId = members.get(binding.spPayer.getSelectedItemPosition()).getUid();
        Map<String, Double> exact = exactAmounts(method);
        Map<String, Double> pct = percentages(method);

        List<ExpenseShare> shares;
        try {
            List<String> memberIds = new ArrayList<>();
            for (User user : members) memberIds.add(user.getUid());
            expenseManager.validateExpenseInput(amount, payerId, participants, memberIds);
            shares = expenseManager.computeShares(amount, participants, method, exact, pct);
        } catch (RuntimeException e) {
            UiUtils.toast(this, e.getMessage() != null ? e.getMessage() : "Invalid split");
            return;
        }

        StringBuilder message = new StringBuilder("Confirm shares (must sum to ")
                .append(UiUtils.formatMoney(amount)).append("):\n\n");
        for (ExpenseShare share : shares) {
            message.append(nameOf(share.getUserId())).append(": ")
                    .append(UiUtils.formatMoney(share.getAmount())).append('\n');
        }

        new AlertDialog.Builder(this)
                .setTitle("Review shares")
                .setMessage(message.toString())
                .setPositiveButton("Save", (dialog, which) ->
                        persist(title, amount, date, category, payerId, participants, method, note, exact, pct))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void persist(String title, double amount, String date, ExpenseCategory category, String payerId,
                         List<String> participants, SplitMethod method, String note,
                         Map<String, Double> exact, Map<String, Double> pct) {
        binding.progress.setVisibility(View.VISIBLE);
        String id = editingExpenseId;
        UiUtils.launchIo(this, () -> id == null
                ? expenseManager.createExpense(groupId, title, amount, date, category, payerId, participants,
                method, note, exact, pct)
                : expenseManager.editExpense(id, title, amount, category, payerId, participants,
                method, note, exact, pct), expense -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, "Saved");
            finish();
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, message);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
