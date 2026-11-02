package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.HistoryManager
import com.splitsmart.app.databinding.ActivityHistoryBinding
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.Transaction
import com.splitsmart.app.model.TransactionType

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val historyManager = HistoryManager.getInstance()
    private lateinit var groupId: String

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
            holder.meta.text = "${t.type} · ${t.date} · ${formatMoney(t.amount)}" +
                if (t.category.isNotBlank()) " · ${t.category}" else ""
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
        binding.spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        binding.spCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spSort.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sorts)

        binding.btnApply.setOnClickListener { load() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val typeSel = binding.spType.selectedItem?.toString()
        val catSel = binding.spCategory.selectedItem?.toString()
        val sortSel = binding.spSort.selectedItem?.toString() ?: "NEWEST"
        val filter = HistoryManager.HistoryFilter(
            keyword = binding.etSearch.text?.toString(),
            type = typeSel?.takeIf { it != "ALL" }?.let { TransactionType.valueOf(it) },
            category = catSel?.takeIf { it != "ALL" }?.let { ExpenseCategory.valueOf(it) },
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
        val title: TextView = view.findViewById(R.id.tvTitle)
        val meta: TextView = view.findViewById(R.id.tvMeta)
    }
}
