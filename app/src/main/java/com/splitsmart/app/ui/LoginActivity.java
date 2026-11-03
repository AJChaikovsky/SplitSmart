package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private final AccountManager accountManager = AccountManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (accountManager.isLoggedIn()) {
            goHome();
            return;
        }
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(getString(R.string.login));

        binding.btnLogin.setOnClickListener(v -> doLogin());
        binding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String email = UiUtils.text(binding.etEmail);
        String password = UiUtils.text(binding.etPassword);
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, () -> accountManager.login(email, password), user -> {
            binding.progress.setVisibility(View.GONE);
            goHome();
        }, message -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(this, message);
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
