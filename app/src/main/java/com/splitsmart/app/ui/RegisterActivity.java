package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private final AccountManager accountManager = AccountManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(getString(R.string.register));

        binding.btnRegister.setOnClickListener(v -> doRegister());
        binding.btnGoLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String name = UiUtils.text(binding.etName);
        String email = UiUtils.text(binding.etEmail);
        String uscId = UiUtils.text(binding.etUscId);
        String password = UiUtils.text(binding.etPassword);
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> accountManager.register(name, email, password, uscId), user -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, "Account created");
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, message);
        });
    }
}
