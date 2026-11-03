package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.R;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.databinding.ActivityEditGroupBinding;

public class EditGroupActivity extends AppCompatActivity {
    private ActivityEditGroupBinding binding;
    private final GroupManager groupManager = GroupManager.getInstance();
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.edit_group));
        }

        String extra = getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID);
        groupId = extra == null ? "" : extra;
        if (groupId.trim().isEmpty()) {
            UiUtils.toast(this, "Missing group");
            finish();
            return;
        }

        UiUtils.launchIo(this, () -> groupManager.getGroup(groupId), group -> {
            binding.etName.setText(group.getName());
            binding.etDescription.setText(group.getDescription());
        });

        binding.btnSave.setOnClickListener(v -> {
            String name = UiUtils.text(binding.etName);
            String description = UiUtils.text(binding.etDescription);
            binding.progress.setVisibility(View.VISIBLE);
            UiUtils.launchIo(this, () -> groupManager.editGroup(groupId, name, description), group -> {
                binding.progress.setVisibility(View.GONE);
                UiUtils.toast(this, "Saved");
                finish();
            }, this::showError);
        });

        binding.btnDelete.setOnClickListener(v ->
                UiUtils.confirm(this, "Delete group", "Delete this group and all expenses/settlements?", () -> {
                    binding.progress.setVisibility(View.VISIBLE);
                    UiUtils.launchIo(this, () -> {
                        groupManager.deleteGroup(groupId);
                        return null;
                    }, ignored -> {
                        binding.progress.setVisibility(View.GONE);
                        Intent intent = new Intent(this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }, this::showError);
                }));
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
