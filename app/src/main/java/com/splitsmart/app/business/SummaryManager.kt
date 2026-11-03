package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.MonthlySummary

class SummaryManager(
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance()
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
        val payerIdTotals = mutableMapOf<String, Double>()
        var total = 0.0
        expenses.forEach { expense ->
            total += expense.amount
            val cat = expense.category
            categoryTotals[cat] = MoneyUtils.roundToCents(
                (categoryTotals[cat] ?: 0.0) + expense.amount
            )
            payerIdTotals[expense.payerId] = MoneyUtils.roundToCents(
                (payerIdTotals[expense.payerId] ?: 0.0) + expense.amount
            )
        }

        val payerTotals = payerIdTotals.mapKeys { (uid, _) ->
            userRepository.getById(uid)?.name ?: uid.take(8)
        }

        return MonthlySummary(
            year = year,
            month = month,
            expenseCount = expenses.size,
            totalSpending = MoneyUtils.roundToCents(total),
            categoryTotals = categoryTotals,
            payerTotals = payerTotals
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
