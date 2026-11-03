package com.splitsmart.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.splitsmart.app.R
import com.splitsmart.app.business.SummaryManager
import com.splitsmart.app.databinding.ActivityMonthlySummaryBinding
import kotlinx.coroutines.Job
import java.util.Calendar

class GroupSummaryFragment : GroupTabFragment(R.layout.activity_monthly_summary) {
    private var viewBinding: ActivityMonthlySummaryBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private val summaryManager = SummaryManager.getInstance()
    private var loading: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = ActivityMonthlySummaryBinding.bind(view)
        val calendar = Calendar.getInstance()
        binding.etYear.setText(savedInstanceState?.getString("year") ?: calendar.get(Calendar.YEAR).toString())
        binding.etMonth.setText(savedInstanceState?.getString("month") ?: (calendar.get(Calendar.MONTH) + 1).toString())
        binding.btnLoad.setOnClickListener { refresh() }
    }

    override fun refresh() {
        val year = binding.etYear.text?.toString()?.toIntOrNull()
        val month = binding.etMonth.text?.toString()?.toIntOrNull()
        if (year == null || month == null || month !in 1..12) {
            requireContext().toast(getString(R.string.invalid_period))
            return
        }
        loading?.cancel()
        binding.progress.visibility = View.VISIBLE
        loading = loadData(fetch = { summaryManager.getMonthlySummary(groupId, year, month) }, render = { summary ->
            binding.tvTotalSpending.text = formatMoney(summary.totalSpending)
            binding.tvExpenseCount.text = resources.getQuantityString(
                R.plurals.expense_count, summary.expenseCount, summary.expenseCount)
            binding.tvSummary.text = if (summary.isEmpty()) getString(R.string.no_expenses_month)
                else getString(R.string.category_period, summary.year, summary.month)
            binding.layoutCharts.visibility = if (summary.isEmpty()) View.GONE else View.VISIBLE
            if (summary.isEmpty()) {
                binding.categoryChart.clear()
                binding.payerChart.clear()
                binding.tvPayerInsights.text = ""
            } else {
                binding.categoryChart.setData(summary.categoryTotals, formatMoney(summary.totalSpending))
                binding.payerChart.setData(summary.payerTotals)
                binding.tvPayerInsights.text = if (summary.payerInsights.isEmpty())
                    getString(R.string.payers_equal) else summary.payerInsights.joinToString("\n") { "• $it" }
            }
            binding.llCategoryTotals.removeAllViews()
            binding.llCategoryTotals.visibility = if (summary.isEmpty()) View.GONE else View.VISIBLE
            summary.categoryTotals.entries.sortedByDescending { it.value }.forEach { (category, total) ->
                val row = layoutInflater.inflate(R.layout.item_category_total, binding.llCategoryTotals, false)
                row.findViewById<TextView>(R.id.tvCategory).text = displayChoice(category)
                row.findViewById<TextView>(R.id.tvTotal).text = formatMoney(total)
                binding.llCategoryTotals.addView(row)
            }
            binding.progress.visibility = View.GONE
        }, onError = {
            binding.progress.visibility = View.GONE
            requireContext().toast(it)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewBinding?.let {
            outState.putString("year", it.etYear.text?.toString())
            outState.putString("month", it.etMonth.text?.toString())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}
