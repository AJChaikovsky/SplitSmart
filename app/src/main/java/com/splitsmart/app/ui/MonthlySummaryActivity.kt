package com.splitsmart.app.ui

import android.os.Bundle
import android.view.View
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
        if (year == null || month == null) {
            toast("Enter valid year and month")
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
                if (summary.isEmpty()) {
                    binding.tvSummary.text = getString(R.string.no_expenses_month)
                } else {
                    binding.tvSummary.text = buildString {
                        appendLine("Period: ${summary.year}-${summary.month.toString().padStart(2, '0')}")
                        appendLine("Expenses: ${summary.expenseCount}")
                        appendLine("Total spending: ${formatMoney(summary.totalSpending)}")
                        appendLine()
                        appendLine("By category:")
                        summary.categoryTotals.forEach { (cat, total) ->
                            appendLine("  $cat: ${formatMoney(total)}")
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
