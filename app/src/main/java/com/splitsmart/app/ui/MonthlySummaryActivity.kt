package com.splitsmart.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.SummaryManager
import com.splitsmart.app.databinding.ActivityMonthlySummaryBinding
import java.util.Calendar

class MonthlySummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonthlySummaryBinding
    private val summaryManager = SummaryManager.getInstance()
    private lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.monthly_summary)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        val cal = Calendar.getInstance()
        binding.etYear.setText(cal.get(Calendar.YEAR).toString())
        binding.etMonth.setText((cal.get(Calendar.MONTH) + 1).toString())
        binding.btnLoad.setOnClickListener { load() }
        load()
    }

    private fun load() {
        val year = binding.etYear.text?.toString()?.toIntOrNull()
        val month = binding.etMonth.text?.toString()?.toIntOrNull()
        if (year == null || month == null || month !in 1..12) {
            toast("Enter valid year and month (1-12)")
            return
        }
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val summary = summaryManager.getMonthlySummary(groupId, year, month)
            runOnUiThread {
                binding.progress.visibility = View.GONE
                binding.tvTotalSpending.text = formatMoney(summary.totalSpending)
                binding.tvExpenseCount.text = resources.getQuantityString(
                    R.plurals.expense_count, summary.expenseCount, summary.expenseCount
                )
                binding.tvSummary.text = if (summary.isEmpty()) {
                    getString(R.string.no_expenses_month)
                } else {
                    "${summary.year}-${summary.month.toString().padStart(2, '0')} · By category"
                }
                binding.layoutCharts.visibility = if (summary.isEmpty()) View.GONE else View.VISIBLE
                if (summary.isEmpty()) {
                    binding.categoryChart.clear()
                    binding.payerChart.clear()
                    binding.tvPayerInsights.text = ""
                } else {
                    binding.categoryChart.setData(summary.categoryTotals, formatMoney(summary.totalSpending))
                    binding.payerChart.setData(summary.payerTotals)
                    binding.tvPayerInsights.text = if (summary.payerInsights.isEmpty()) {
                        "Everyone paid the same this month."
                    } else {
                        summary.payerInsights.joinToString("\n") { "• $it" }
                    }
                }
                binding.llCategoryTotals.removeAllViews()
                binding.llCategoryTotals.visibility = if (summary.isEmpty()) View.GONE else View.VISIBLE
                summary.categoryTotals.entries.sortedByDescending { it.value }.forEach { (category, total) ->
                    val row = layoutInflater.inflate(R.layout.item_category_total, binding.llCategoryTotals, false)
                    row.findViewById<TextView>(R.id.tvCategory).text = displayChoice(category)
                    row.findViewById<TextView>(R.id.tvTotal).text = formatMoney(total)
                    binding.llCategoryTotals.addView(row)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
