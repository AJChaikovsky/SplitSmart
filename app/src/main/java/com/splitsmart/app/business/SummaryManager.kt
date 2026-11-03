package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.MonthlySummary

class SummaryManager(
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val groupRepository: GroupRepository = GroupRepository.getInstance()
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

        val group = groupRepository.getById(groupId)
        val memberIds = group?.memberIds.orEmpty().ifEmpty { payerIdTotals.keys.toList() }

        // Include members who paid $0 so differences are visible.
        val nameById = memberIds.associateWith { uid ->
            userRepository.getById(uid)?.name ?: uid.take(8)
        }
        val payerTotals = linkedMapOf<String, Double>()
        memberIds.forEach { uid ->
            val name = nameById[uid] ?: uid.take(8)
            payerTotals[name] = payerIdTotals[uid] ?: 0.0
        }
        // Anyone who paid but is no longer a member
        payerIdTotals.forEach { (uid, amount) ->
            if (uid !in memberIds) {
                val name = userRepository.getById(uid)?.name ?: uid.take(8)
                payerTotals[name] = amount
            }
        }

        val roundedTotal = MoneyUtils.roundToCents(total)
        return MonthlySummary(
            year = year,
            month = month,
            expenseCount = expenses.size,
            totalSpending = roundedTotal,
            categoryTotals = categoryTotals,
            payerTotals = payerTotals,
            payerInsights = buildPayerInsights(payerTotals, roundedTotal)
        )
    }

    private fun buildPayerInsights(
        payerTotals: Map<String, Double>,
        total: Double
    ): List<String> {
        if (payerTotals.isEmpty() || total <= 0) return emptyList()

        val ranked = payerTotals.entries.sortedByDescending { it.value }
        val insights = mutableListOf<String>()
        val top = ranked.first()
        val bottom = ranked.last()
        val memberCount = ranked.size
        val fairShare = MoneyUtils.roundToCents(total / memberCount)

        insights.add(
            "${top.key} paid the most (${format(top.value)} · ${pct(top.value, total)}% of group spend)"
        )

        if (ranked.size >= 2 && bottom.value < top.value) {
            val gap = MoneyUtils.roundToCents(top.value - bottom.value)
            insights.add(
                "${top.key} paid ${format(gap)} more than ${bottom.key}"
            )
        }

        if (ranked.size >= 2) {
            val leastPayers = ranked.filter { it.value == bottom.value }
            if (bottom.value == 0.0) {
                val names = leastPayers.joinToString(", ") { it.key }
                insights.add("$names paid $0 this month")
            } else if (leastPayers.size == 1) {
                insights.add(
                    "${bottom.key} paid the least (${format(bottom.value)} · ${pct(bottom.value, total)}%)"
                )
            }
        }

        ranked.forEach { (name, amount) ->
            val diff = MoneyUtils.roundToCents(amount - fairShare)
            when {
                diff > 0.009 -> insights.add(
                    "$name is ${format(diff)} above an even split (${format(fairShare)} each)"
                )
                diff < -0.009 -> insights.add(
                    "$name is ${format(-diff)} below an even split (${format(fairShare)} each)"
                )
            }
        }

        return insights.distinct().take(6)
    }

    private fun format(amount: Double): String = String.format("$%.2f", amount)

    private fun pct(part: Double, total: Double): Int =
        if (total <= 0) 0 else Math.round((part / total * 100.0)).toInt()

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
