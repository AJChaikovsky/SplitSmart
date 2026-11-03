package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.splitsmart.app.R
import com.splitsmart.app.business.FriendManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityCreateGroupBinding
import com.splitsmart.app.model.User

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private val groupManager = GroupManager.getInstance()
    private val friendManager = FriendManager.getInstance()
    private val friendChecks = linkedMapOf<String, MaterialCheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.create_group)

        loadFriends()

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty()
            val description = binding.etDescription.text?.toString().orEmpty()
            val selected = friendChecks.filter { it.value.isChecked }.keys.toList()
            binding.progress.visibility = View.VISIBLE
            launchIo(onError = {
                binding.progress.visibility = View.GONE
                toast(it)
            }) {
                val group = groupManager.createGroup(name, description, selected)
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    startActivity(
                        Intent(this@CreateGroupActivity, GroupDetailActivity::class.java)
                            .putExtra(HomeActivity.EXTRA_GROUP_ID, group.id)
                    )
                    finish()
                }
            }
        }
    }

    private fun loadFriends() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val friends = friendManager.listFriends()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                renderFriendChecks(friends)
            }
        }
    }

    private fun renderFriendChecks(friends: List<User>) {
        binding.llFriendChecks.removeAllViews()
        friendChecks.clear()
        if (friends.isEmpty()) {
            binding.tvNoFriendsHint.visibility = View.VISIBLE
            return
        }
        binding.tvNoFriendsHint.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        friends.forEach { friend ->
            val cb = inflater.inflate(
                R.layout.item_friend_check,
                binding.llFriendChecks,
                false
            ) as MaterialCheckBox
            cb.text = "${friend.name} (${friend.email})"
            friendChecks[friend.uid] = cb
            binding.llFriendChecks.addView(cb)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
