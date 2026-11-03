package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Older internal routes now enter the same persistent group workspace. */
public abstract class GroupTabRedirectActivity extends AppCompatActivity {
    private final int tab;

    protected GroupTabRedirectActivity(int tab) {
        this.tab = tab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, GroupDetailActivity.class)
                .putExtra(HomeActivity.EXTRA_GROUP_ID, getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID))
                .putExtra(GroupDetailActivity.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
