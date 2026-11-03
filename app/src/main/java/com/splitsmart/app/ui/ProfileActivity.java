package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private final AccountManager accountManager = AccountManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.profile));
        }

        binding.btnSave.setOnClickListener(v -> {
            String name = UiUtils.text(binding.etName);
            binding.progress.setVisibility(View.VISIBLE);
            UiUtils.launchIo(this, () -> accountManager.updateName(name), user -> {
                binding.progress.setVisibility(View.GONE);
                UiUtils.toast(this, "Name updated");
            }, this::showError);
        });

        binding.btnLogout.setOnClickListener(v -> {
            accountManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, accountManager::getCurrentUser, user -> {
            binding.progress.setVisibility(View.GONE);
            if (user == null) {
                UiUtils.toast(this, "Not logged in");
                finish();
                return;
            }
            binding.tvEmail.setText(user.getEmail());
            binding.tvUscId.setText("USC ID: " + user.getUscId());
            binding.etName.setText(user.getName());
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
