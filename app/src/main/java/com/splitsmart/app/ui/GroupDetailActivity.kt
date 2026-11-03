package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.splitsmart.app.R
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityGroupDetailBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One group workspace: persistent navigation with balances as the default destination. */
class GroupDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var groupId: String
    private var selectedTab = 0
    private var loading: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            toast(getString(R.string.missing_group))
            finish()
            return
        }
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.group_title)
        binding.btnMembers.setOnClickListener { showMembers() }
        binding.btnAddExpense.setOnClickListener {
            startActivity(Intent(this, CreateExpenseActivity::class.java)
                .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId))
        }
        listOf(R.string.balances, R.string.history, R.string.overview).forEach {
            binding.groupTabs.addTab(binding.groupTabs.newTab().setText(it), false)
        }
        binding.groupTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { showTab(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        selectedTab = (savedInstanceState?.getInt(EXTRA_TAB)
            ?: intent.getIntExtra(EXTRA_TAB, 0)).coerceIn(0, 2)
        binding.groupTabs.getTabAt(selectedTab)?.select()
        supportFragmentManager.setFragmentResultListener(MEMBERS_CHANGED, this) { _, _ ->
            refreshGroup()
            (supportFragmentManager.primaryNavigationFragment as? GroupTabFragment)?.refresh()
        }
    }

    private fun showTab(index: Int) {
        if (supportFragmentManager.isStateSaved) return
        selectedTab = index
        val transaction = supportFragmentManager.beginTransaction().setReorderingAllowed(true)
        for (position in 0..2) {
            supportFragmentManager.findFragmentByTag("group_tab_$position")?.let { fragment ->
                if (position != index) {
                    transaction.hide(fragment).setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                }
            }
        }
        val tag = "group_tab_$index"
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: when (index) {
            1 -> GroupHistoryFragment()
            2 -> GroupSummaryFragment()
            else -> GroupBalanceFragment()
        }.apply {
            arguments = Bundle().apply { putString(HomeActivity.EXTRA_GROUP_ID, groupId) }
        }
        if (fragment.isAdded) transaction.show(fragment)
        else transaction.add(R.id.groupContent, fragment, tag)
        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            .setPrimaryNavigationFragment(fragment).commitNow()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refreshGroup()
    }

    private fun refreshGroup() {
        loading?.cancel()
        loading = lifecycleScope.launch {
            try {
                val group = withContext(Dispatchers.IO) { GroupManager.getInstance().getGroup(groupId) }
                supportActionBar?.title = group.name
                binding.btnMembers.text = resources.getQuantityString(
                    R.plurals.group_members, group.memberIds.size, group.memberIds.size)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                toast(error.message ?: getString(R.string.group_load_error))
            }
        }
    }

    fun showMembers() {
        if (supportFragmentManager.isStateSaved || supportFragmentManager.findFragmentByTag("members") != null) return
        GroupMembersSheet().apply {
            arguments = Bundle().apply { putString(HomeActivity.EXTRA_GROUP_ID, groupId) }
        }.show(supportFragmentManager, "members")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, R.id.action_group_options, Menu.NONE, R.string.group_options).apply {
            setIcon(R.drawable.ic_more)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.action_group_options) return super.onOptionsItemSelected(item)
        if (!supportFragmentManager.isStateSaved && supportFragmentManager.findFragmentByTag("group_options") == null) {
            GroupOptionsSheet().apply {
                arguments = Bundle().apply { putString(HomeActivity.EXTRA_GROUP_ID, groupId) }
            }.show(supportFragmentManager, "group_options")
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID) != groupId) {
            finish()
            startActivity(Intent(this, GroupDetailActivity::class.java).putExtras(intent))
        } else {
            setIntent(intent)
            binding.groupTabs.getTabAt(intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, 2))?.select()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    companion object {
        const val EXTRA_TAB = "groupTab"
        const val MEMBERS_CHANGED = "groupMembersChanged"
    }
}
