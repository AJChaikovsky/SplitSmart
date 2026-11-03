package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.FriendManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityGroupDetailBinding
import com.splitsmart.app.databinding.ItemGroupMemberBinding
import com.splitsmart.app.model.Group
import com.splitsmart.app.model.User

class GroupDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupDetailBinding
    private val groupManager = GroupManager.getInstance()
    private val friendManager = FriendManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private val accountManager = AccountManager.getInstance()
    private lateinit var groupId: String
    private var group: Group? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            toast("Missing group")
            finish()
            return
        }

        binding.btnAddMember.setOnClickListener { addMemberByEmail() }
        binding.btnAddFromFriends.setOnClickListener { showAddFriendsDialog() }
        binding.btnLeaveGroup.setOnClickListener { leaveGroup() }
        binding.btnAddExpense.setOnClickListener {
            startActivity(
                Intent(this, CreateExpenseActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
        binding.btnBalances.setOnClickListener {
            startActivity(
                Intent(this, BalanceActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
        binding.btnHistory.setOnClickListener {
            startActivity(
                Intent(this, HistoryActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
        binding.btnSummary.setOnClickListener {
            startActivity(
                Intent(this, MonthlySummaryActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
        binding.btnEditGroup.setOnClickListener {
            startActivity(
                Intent(this, EditGroupActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroup()
    }

    private fun loadGroup() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val g = groupManager.getGroup(groupId)
            group = g
            val members = g.memberIds.map { uid ->
                val user = userRepository.getById(uid)
                Triple(uid, user?.name ?: uid, user?.email.orEmpty())
            }
            val currentUid = accountManager.currentUid()
            val isOwner = g.ownerId == currentUid
            val isMember = currentUid != null && currentUid in g.memberIds
            runOnUiThread {
                binding.progress.visibility = View.GONE
                supportActionBar?.title = g.name
                binding.tvName.text = g.name
                binding.tvDescription.text = g.description.ifBlank { "No description" }
                binding.tvGroupAvatar.text = initials(g.name)
                binding.btnEditGroup.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.layoutAddMember.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.btnAddFromFriends.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.btnLeaveGroup.visibility =
                    if (isMember && !isOwner) View.VISIBLE else View.GONE
                renderMembers(members, isOwner, g.ownerId)
            }
        }
    }

    private fun renderMembers(
        members: List<Triple<String, String, String>>,
        isOwner: Boolean,
        ownerId: String
    ) {
        binding.llMemberList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        members.forEach { (uid, name, email) ->
            val row = ItemGroupMemberBinding.inflate(inflater, binding.llMemberList, false)
            row.tvAvatar.text = initials(name)
            row.tvMemberName.text = if (uid == ownerId) "$name · owner" else name
            row.tvMemberEmail.text = email
            val canRemove = isOwner && uid != ownerId
            row.btnRemoveMember.visibility = if (canRemove) View.VISIBLE else View.GONE
            if (canRemove) {
                row.btnRemoveMember.setOnClickListener {
                    confirm("Remove member", "Remove $name from this group?") {
                        removeMember(uid)
                    }
                }
            }
            binding.llMemberList.addView(row.root)
        }
    }

    private fun addMemberByEmail() {
        val email = binding.etMemberEmail.text?.toString().orEmpty()
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            groupManager.addMemberByEmail(groupId, email)
            runOnUiThread {
                binding.progress.visibility = View.GONE
                binding.etMemberEmail.setText("")
                toast("Member added")
                loadGroup()
            }
        }
    }

    private fun showAddFriendsDialog() {
        val current = group ?: return
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val candidates = friendManager.listFriends()
                .filter { it.uid !in current.memberIds }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                if (candidates.isEmpty()) {
                    toast(getString(com.splitsmart.app.R.string.no_friends_to_add))
                    return@runOnUiThread
                }
                val labels = candidates.map { "${it.name} (${it.email})" }.toTypedArray()
                val checked = BooleanArray(candidates.size)
                AlertDialog.Builder(this@GroupDetailActivity)
                    .setTitle(com.splitsmart.app.R.string.add_from_friends)
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setPositiveButton(com.splitsmart.app.R.string.add_short) { _, _ ->
                        val selected = candidates.filterIndexed { index, _ -> checked[index] }
                        addFriendsToGroup(selected)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun addFriendsToGroup(friends: List<User>) {
        if (friends.isEmpty()) return
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            friends.forEach { groupManager.addMemberByUid(groupId, it.uid) }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                toast("Added ${friends.size} friend(s)")
                loadGroup()
            }
        }
    }

    private fun removeMember(memberUid: String) {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            groupManager.removeMember(groupId, memberUid)
            runOnUiThread {
                binding.progress.visibility = View.GONE
                toast("Member removed")
                loadGroup()
            }
        }
    }

    private fun leaveGroup() {
        confirm("Leave group", "Leave this group? You will lose access until invited again.") {
            binding.progress.visibility = View.VISIBLE
            launchIo(onError = {
                binding.progress.visibility = View.GONE
                toast(it)
            }) {
                groupManager.leaveGroup(groupId)
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    toast("Left group")
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
