package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.ActivityManager
import com.splitsmart.app.business.FriendManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityHomeBinding
import com.splitsmart.app.model.ActivityItem
import com.splitsmart.app.model.ActivityKind
import com.splitsmart.app.model.FriendBalance
import com.splitsmart.app.model.Group
import com.splitsmart.app.model.TransactionType

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val groupManager = GroupManager.getInstance()
    private val friendManager = FriendManager.getInstance()
    private val activityManager = ActivityManager.getInstance()
    private val accountManager = AccountManager.getInstance()

    private val groupsAdapter = GroupAdapter { group ->
        startActivity(
            Intent(this, GroupDetailActivity::class.java)
                .putExtra(EXTRA_GROUP_ID, group.id)
        )
    }
    private val friendsAdapter = FriendAdapter()
    private val activityAdapter = ActivityAdapter { item ->
        openActivityItem(item)
    }

    private var currentTab = R.id.nav_groups

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!accountManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvGroups.layoutManager = LinearLayoutManager(this)
        binding.rvGroups.adapter = groupsAdapter
        binding.rvFriends.layoutManager = LinearLayoutManager(this)
        binding.rvFriends.adapter = friendsAdapter
        binding.rvActivity.layoutManager = LinearLayoutManager(this)
        binding.rvActivity.adapter = activityAdapter

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.fabCreate.setOnClickListener { onFabClick() }
        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        binding.bottomNav.selectedItemId = R.id.nav_groups
        showTab(R.id.nav_groups)
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentTab()
    }

    private fun onFabClick() {
        when (currentTab) {
            R.id.nav_groups -> startActivity(Intent(this, CreateGroupActivity::class.java))
            R.id.nav_friends -> showAddFriendDialog()
        }
    }

    private fun showTab(tabId: Int) {
        currentTab = tabId
        binding.panelGroups.visibility = if (tabId == R.id.nav_groups) View.VISIBLE else View.GONE
        binding.panelFriends.visibility = if (tabId == R.id.nav_friends) View.VISIBLE else View.GONE
        binding.panelActivity.visibility = if (tabId == R.id.nav_activity) View.VISIBLE else View.GONE

        when (tabId) {
            R.id.nav_groups -> {
                binding.tvHeaderTitle.setText(R.string.your_groups)
                binding.tvHeaderSubtitle.setText(R.string.groups_subtitle)
                binding.fabCreate.show()
                binding.fabCreate.contentDescription = getString(R.string.create_group)
            }
            R.id.nav_friends -> {
                binding.tvHeaderTitle.setText(R.string.your_friends)
                binding.tvHeaderSubtitle.setText(R.string.friends_subtitle)
                binding.fabCreate.show()
                binding.fabCreate.contentDescription = getString(R.string.add_friend)
            }
            R.id.nav_activity -> {
                binding.tvHeaderTitle.setText(R.string.your_activity)
                binding.tvHeaderSubtitle.setText(R.string.activity_subtitle)
                binding.fabCreate.hide()
            }
        }
        refreshCurrentTab()
    }

    private fun refreshCurrentTab() {
        when (currentTab) {
            R.id.nav_groups -> loadGroups()
            R.id.nav_friends -> loadFriends()
            R.id.nav_activity -> loadActivity()
        }
    }

    private fun loadGroups() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val groups = groupManager.listGroupsForCurrentUser()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                groupsAdapter.submit(groups)
                binding.tvEmptyGroups.visibility =
                    if (groups.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadFriends() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val friends = friendManager.listFriendsWithBalances()
            val owedToYou = friends.filter { it.youAreOwed }.sumOf { it.netAmount }
            val youOwe = friends.filter { it.youOwe }.sumOf { -it.netAmount }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                friendsAdapter.submit(friends)
                binding.tvEmptyFriends.visibility =
                    if (friends.isEmpty()) View.VISIBLE else View.GONE
                if (friends.isEmpty()) {
                    binding.tvFriendsSummary.visibility = View.GONE
                } else {
                    binding.tvFriendsSummary.visibility = View.VISIBLE
                    binding.tvFriendsSummary.text = buildString {
                        append(getString(R.string.you_are_owed))
                        append(": ")
                        append(formatMoney(owedToYou))
                        append("  ·  ")
                        append(getString(R.string.you_owe))
                        append(": ")
                        append(formatMoney(youOwe))
                    }
                }
            }
        }
    }

    private fun loadActivity() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val items = activityManager.getMyActivity()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                activityAdapter.submit(items)
                binding.tvEmptyActivity.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddFriendDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.friend_email)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_friend)
            .setView(input)
            .setPositiveButton(R.string.add_short) { _, _ ->
                val email = input.text?.toString().orEmpty()
                binding.progress.visibility = View.VISIBLE
                launchIo(onError = {
                    binding.progress.visibility = View.GONE
                    toast(it)
                }) {
                    friendManager.addFriendByEmail(email)
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        toast("Friend added")
                        loadFriends()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openActivityItem(item: ActivityItem) {
        when (item.kind) {
            ActivityKind.EXPENSE -> {
                startActivity(
                    Intent(this, TransactionDetailActivity::class.java)
                        .putExtra(HistoryActivity.EXTRA_TX_ID, item.id)
                        .putExtra(HistoryActivity.EXTRA_TX_TYPE, TransactionType.EXPENSE.name)
                )
            }
            ActivityKind.SETTLEMENT -> {
                startActivity(
                    Intent(this, TransactionDetailActivity::class.java)
                        .putExtra(HistoryActivity.EXTRA_TX_ID, item.id)
                        .putExtra(HistoryActivity.EXTRA_TX_TYPE, TransactionType.SETTLEMENT.name)
                )
            }
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "groupId"
    }

    private class GroupAdapter(
        private val onClick: (Group) -> Unit
    ) : RecyclerView.Adapter<GroupAdapter.VH>() {
        private val items = mutableListOf<Group>()

        fun submit(list: List<Group>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val g = items[position]
            holder.name.text = g.name
            holder.description.text =
                g.description.ifBlank { holder.itemView.context.getString(R.string.group_fallback) }
            holder.members.text = holder.itemView.resources.getQuantityString(
                R.plurals.group_members,
                g.memberIds.size,
                g.memberIds.size
            )
            holder.avatar.text = initials(g.name)
            holder.itemView.setOnClickListener { onClick(g) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: TextView = view.findViewById(R.id.tvAvatar)
            val name: TextView = view.findViewById(R.id.tvName)
            val description: TextView = view.findViewById(R.id.tvDescription)
            val members: TextView = view.findViewById(R.id.tvMembers)
        }
    }

    private class FriendAdapter : RecyclerView.Adapter<FriendAdapter.VH>() {
        private val items = mutableListOf<FriendBalance>()

        fun submit(list: List<FriendBalance>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val ctx = holder.itemView.context
            holder.avatar.text = initials(item.friend.name)
            holder.name.text = item.friend.name
            holder.email.text = item.friend.email
            when {
                item.youAreOwed -> {
                    holder.balance.text = ctx.getString(R.string.owes_you, formatMoney(item.netAmount))
                    holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
                }
                item.youOwe -> {
                    holder.balance.text =
                        ctx.getString(R.string.you_owe_amount, formatMoney(-item.netAmount))
                    holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.error))
                }
                else -> {
                    holder.balance.text = ctx.getString(R.string.settled_up)
                    holder.balance.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                }
            }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: TextView = view.findViewById(R.id.tvAvatar)
            val name: TextView = view.findViewById(R.id.tvName)
            val email: TextView = view.findViewById(R.id.tvEmail)
            val balance: TextView = view.findViewById(R.id.tvBalance)
        }
    }

    private class ActivityAdapter(
        private val onClick: (ActivityItem) -> Unit
    ) : RecyclerView.Adapter<ActivityAdapter.VH>() {
        private val items = mutableListOf<ActivityItem>()

        fun submit(list: List<ActivityItem>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.kind.text = when (item.kind) {
                ActivityKind.EXPENSE -> "Expense"
                ActivityKind.SETTLEMENT -> "Settlement"
            }
            holder.group.text = item.groupName
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle
            holder.amount.text = formatMoney(item.amount)
            holder.date.text = item.date
            holder.itemView.setOnClickListener { onClick(item) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val kind: TextView = view.findViewById(R.id.tvKind)
            val group: TextView = view.findViewById(R.id.tvGroup)
            val title: TextView = view.findViewById(R.id.tvTitle)
            val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
            val amount: TextView = view.findViewById(R.id.tvAmount)
            val date: TextView = view.findViewById(R.id.tvDate)
        }
    }
}
