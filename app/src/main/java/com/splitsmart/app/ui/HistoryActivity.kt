package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.business.HistoryManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityHistoryBinding
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.Transaction
import com.splitsmart.app.model.TransactionType
import com.splitsmart.app.model.User

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val historyManager = HistoryManager.getInstance()
    private val groupManager = GroupManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private lateinit var groupId: String
    private var members: List<User> = emptyList()
    private var memberLabels: List<String> = listOf("All members")

    private val adapter = object : RecyclerView.Adapter<VH>() {
        private val items = mutableListOf<Transaction>()

        fun submit(list: List<Transaction>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = items[position]
            holder.title.text = when (t.type) {
                TransactionType.EXPENSE -> t.title
                TransactionType.SETTLEMENT -> "Settlement"
            }
            holder.meta.text = t.date
            holder.amount.text = formatMoney(t.amount)
            holder.category.text = if (t.type == TransactionType.SETTLEMENT) "Settlement" else displayChoice(t.category.ifBlank { "OTHER" })
            holder.icon.setImageResource(if (t.type == TransactionType.SETTLEMENT) R.drawable.ic_swap else when (ExpenseCategory.fromString(t.category)) {
                ExpenseCategory.FOOD -> R.drawable.ic_food
                ExpenseCategory.HOUSING -> R.drawable.ic_house
                ExpenseCategory.TRANSPORTATION -> R.drawable.ic_car
                ExpenseCategory.ENTERTAINMENT -> R.drawable.ic_play
                ExpenseCategory.OTHER -> R.drawable.ic_receipt
            })
            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@HistoryActivity, TransactionDetailActivity::class.java)
                        .putExtra(EXTRA_TX_ID, t.id)
                        .putExtra(EXTRA_TX_TYPE, t.type.name)
                        .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        val types = listOf("ALL") + TransactionType.entries.map { it.name }
        val categories = listOf("ALL") + ExpenseCategory.entries.map { it.name }
        val sorts = listOf("NEWEST", "OLDEST")
        binding.spType.adapter = this.choiceAdapter(types)
        binding.spCategory.adapter =
            this.choiceAdapter(categories)
        binding.spSort.adapter = this.choiceAdapter(sorts)
        binding.spMember.adapter =
            this.choiceAdapter(memberLabels)

        binding.btnApply.setOnClickListener { load() }
        loadMembers()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun loadMembers() {
        launchIo {
            val group = groupManager.getGroup(groupId)
            members = group.memberIds.mapNotNull { userRepository.getById(it) }
            memberLabels = listOf("All members") + members.map { "${it.name} (${it.email})" }
            runOnUiThread {
                binding.spMember.adapter = this@HistoryActivity.choiceAdapter(memberLabels)
            }
        }
    }

    private fun load() {
        val typeSel = binding.spType.selectedItem?.toString()
        val catSel = binding.spCategory.selectedItem?.toString()
        val sortSel = binding.spSort.selectedItem?.toString() ?: "NEWEST"
        val memberPos = binding.spMember.selectedItemPosition
        val memberId = if (memberPos > 0 && memberPos - 1 < members.size) {
            members[memberPos - 1].uid
        } else {
            null
        }
        val filter = HistoryManager.HistoryFilter(
            keyword = binding.etSearch.text?.toString(),
            type = typeSel?.takeIf { it != "ALL" }?.let { TransactionType.valueOf(it) },
            category = catSel?.takeIf { it != "ALL" }?.let { ExpenseCategory.valueOf(it) },
            memberId = memberId,
            sortOrder = if (sortSel == "OLDEST") HistoryManager.SortOrder.OLDEST
            else HistoryManager.SortOrder.NEWEST
        )
        launchIo {
            val list = historyManager.getHistory(groupId, filter)
            runOnUiThread {
                adapter.submit(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TX_ID = "txId"
        const val EXTRA_TX_TYPE = "txType"
    }

    private class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivCategory)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val category: TextView = view.findViewById(R.id.tvCategory)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val meta: TextView = view.findViewById(R.id.tvMeta)
    }
}
