package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Older internal routes now enter the same persistent group workspace. */
abstract class GroupTabRedirectActivity(private val tab: Int) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, GroupDetailActivity::class.java)
            .putExtra(HomeActivity.EXTRA_GROUP_ID, intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID))
            .putExtra(GroupDetailActivity.EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
