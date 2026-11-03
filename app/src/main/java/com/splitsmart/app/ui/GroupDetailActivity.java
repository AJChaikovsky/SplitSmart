package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.tabs.TabLayout;
import com.splitsmart.app.R;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.databinding.ActivityGroupDetailBinding;

/** One group workspace: persistent navigation with balances as the default destination. */
public class GroupDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TAB = "groupTab";
    public static final String MEMBERS_CHANGED = "groupMembersChanged";

    private ActivityGroupDetailBinding binding;
    private String groupId;
    private int selectedTab = 0;
    private UiTask loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String extra = getIntent().getStringExtra(HomeActivity.EXTRA_GROUP_ID);
        groupId = extra == null ? "" : extra;
        if (groupId.trim().isEmpty()) {
            UiUtils.toast(this, getString(R.string.missing_group));
            finish();
            return;
        }
        binding = ActivityGroupDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.group_title));
        }
        binding.btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, CreateExpenseActivity.class)
                        .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)));
        for (int title : new int[]{R.string.balances, R.string.history, R.string.overview}) {
            binding.groupTabs.addTab(binding.groupTabs.newTab().setText(title), false);
        }
        binding.groupTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        int requested = savedInstanceState != null
                ? savedInstanceState.getInt(EXTRA_TAB)
                : getIntent().getIntExtra(EXTRA_TAB, 0);
        selectedTab = clampTab(requested);
        TabLayout.Tab tab = binding.groupTabs.getTabAt(selectedTab);
        if (tab != null) tab.select();
        getSupportFragmentManager().setFragmentResultListener(MEMBERS_CHANGED, this, (key, result) -> {
            refreshGroup();
            Fragment primary = getSupportFragmentManager().getPrimaryNavigationFragment();
            if (primary instanceof GroupTabFragment) ((GroupTabFragment) primary).refresh();
        });
    }

    private static int clampTab(int index) {
        return Math.max(0, Math.min(2, index));
    }

    private void showTab(int index) {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        selectedTab = index;
        FragmentTransaction transaction = manager.beginTransaction().setReorderingAllowed(true);
        for (int position = 0; position <= 2; position++) {
            Fragment other = manager.findFragmentByTag("group_tab_" + position);
            if (other != null && position != index) {
                transaction.hide(other).setMaxLifecycle(other, Lifecycle.State.STARTED);
            }
        }
        String tag = "group_tab_" + index;
        Fragment fragment = manager.findFragmentByTag(tag);
        if (fragment == null) {
            if (index == 1) fragment = new GroupHistoryFragment();
            else if (index == 2) fragment = new GroupSummaryFragment();
            else fragment = new GroupBalanceFragment();
            Bundle args = new Bundle();
            args.putString(HomeActivity.EXTRA_GROUP_ID, groupId);
            fragment.setArguments(args);
        }
        if (fragment.isAdded()) transaction.show(fragment);
        else transaction.add(R.id.groupContent, fragment, tag);
        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                .setPrimaryNavigationFragment(fragment)
                .commitNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) refreshGroup();
    }

    private void refreshGroup() {
        if (loading != null) loading.cancel();
        loading = UiUtils.launchIo(this, () -> GroupManager.getInstance().getGroup(groupId), group -> {
            int count = group.getMemberIds().size();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(group.getName());
                getSupportActionBar().setSubtitle(
                        getResources().getQuantityString(R.plurals.group_members, count, count));
            }
        }, message -> UiUtils.toast(this, message != null ? message : getString(R.string.group_load_error)));
    }

    public void showMembers() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag("members") != null) return;
        GroupMembersSheet sheet = new GroupMembersSheet();
        Bundle args = new Bundle();
        args.putString(HomeActivity.EXTRA_GROUP_ID, groupId);
        sheet.setArguments(args);
        sheet.show(manager, "members");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, R.id.action_group_options, Menu.NONE, R.string.group_options);
        item.setIcon(R.drawable.ic_more);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() != R.id.action_group_options) return super.onOptionsItemSelected(item);
        FragmentManager manager = getSupportFragmentManager();
        if (!manager.isStateSaved() && manager.findFragmentByTag("group_options") == null) {
            GroupOptionsSheet sheet = new GroupOptionsSheet();
            Bundle args = new Bundle();
            args.putString(HomeActivity.EXTRA_GROUP_ID, groupId);
            sheet.setArguments(args);
            sheet.show(manager, "group_options");
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!groupId.equals(intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID))) {
            finish();
            startActivity(new Intent(this, GroupDetailActivity.class).putExtras(intent));
        } else {
            setIntent(intent);
            TabLayout.Tab tab = binding.groupTabs.getTabAt(clampTab(intent.getIntExtra(EXTRA_TAB, 0)));
            if (tab != null) tab.select();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
