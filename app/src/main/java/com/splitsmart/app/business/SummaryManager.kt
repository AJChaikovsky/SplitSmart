package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.model.MonthlySummary

class SummaryManager(
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance()
) {
    suspend fun getMonthlySummary(groupId: String, year: Int, month: Int): MonthlySummary {
        require(month in 1..12) { "Month must be 1-12" }
        val monthStr = month.toString().padStart(2, '0')
        val prefix = "$year-$monthStr"

        val expenses = expenseRepository.getByGroup(groupId)
            .filter { it.date.startsWith(prefix) }

        if (expenses.isEmpty()) {
            return MonthlySummary(year = year, month = month)
        }

        val categoryTotals = mutableMapOf<String, Double>()
        var total = 0.0
        expenses.forEach { expense ->
            total += expense.amount
            val cat = expense.category
            categoryTotals[cat] = MoneyUtils.roundToCents(
                (categoryTotals[cat] ?: 0.0) + expense.amount
            )
        }

        return MonthlySummary(
            year = year,
            month = month,
            expenseCount = expenses.size,
            totalSpending = MoneyUtils.roundToCents(total),
            categoryTotals = categoryTotals
        )
    }

    companion object {
        @Volatile
        private var instance: SummaryManager? = null

        fun getInstance(): SummaryManager {
            return instance ?: synchronized(this) {
                instance ?: SummaryManager().also { instance = it }
            }
        }
    }
}
