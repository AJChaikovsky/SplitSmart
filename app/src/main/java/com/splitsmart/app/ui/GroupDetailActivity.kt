package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityGroupDetailBinding
import com.splitsmart.app.model.Group

class GroupDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupDetailBinding
    private val groupManager = GroupManager.getInstance()
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

        binding.btnAddMember.setOnClickListener { addMember() }
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
        launchIo {
            val g = groupManager.getGroup(groupId)
            group = g
            val members = g.memberIds.map { uid ->
                userRepository.getById(uid)
            }
            val isOwner = g.ownerId == accountManager.currentUid()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                supportActionBar?.title = g.name
                binding.tvName.text = g.name
                binding.tvDescription.text = g.description.ifBlank { "No description" }
                binding.tvGroupAvatar.text = initials(g.name)
                binding.llMemberList.removeAllViews()
                members.forEachIndexed { index, user ->
                    val row = layoutInflater.inflate(R.layout.item_member, binding.llMemberList, false)
                    val name = user?.name?.takeIf { it.isNotBlank() } ?: g.memberIds[index]
                    row.findViewById<TextView>(R.id.tvAvatar).text = initials(name)
                    row.findViewById<TextView>(R.id.tvMemberName).text = name
                    row.findViewById<TextView>(R.id.tvMemberEmail).text = user?.email.orEmpty()
                    binding.llMemberList.addView(row)
                }
                binding.btnEditGroup.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.btnAddMember.isEnabled = isOwner
            }
        }
    }

    private fun addMember() {
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
