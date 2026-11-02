package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityHomeBinding
import com.splitsmart.app.model.Group

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val groupManager = GroupManager.getInstance()
    private val accountManager = AccountManager.getInstance()
    private val adapter = GroupAdapter { group ->
        startActivity(Intent(this, GroupDetailActivity::class.java).putExtra(EXTRA_GROUP_ID, group.id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!accountManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_name)

        binding.rvGroups.layoutManager = LinearLayoutManager(this)
        binding.rvGroups.adapter = adapter
        binding.fabCreate.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun loadGroups() {
        launchIo {
            val groups = groupManager.listGroupsForCurrentUser()
            runOnUiThread {
                adapter.submit(groups)
                binding.tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
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
            holder.description.text = g.description.ifBlank { "No description" }
            holder.members.text = "${g.memberIds.size} member(s)"
            holder.itemView.setOnClickListener { onClick(g) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvName)
            val description: TextView = view.findViewById(R.id.tvDescription)
            val members: TextView = view.findViewById(R.id.tvMembers)
        }
    }
}
