package com.splitsmart.app.business

import com.splitsmart.app.model.ExpenseShare

/**
 * Divides evenly to cents; leftover cents go to the first participants so the sum equals total.
 */
class EqualSplitCalculator : SplitCalculator {
    override fun calculate(
        totalAmount: Double,
        participantIds: List<String>,
        exactAmounts: Map<String, Double>,
        percentages: Map<String, Double>
    ): List<ExpenseShare> {
        require(participantIds.isNotEmpty()) { "At least one participant is required" }
        require(totalAmount > 0) { "Amount must be greater than 0" }

        val totalCents = MoneyUtils.toCents(totalAmount)
        val n = participantIds.size
        val base = totalCents / n
        val remainder = (totalCents % n).toInt()

        return participantIds.mapIndexed { index, userId ->
            val cents = base + if (index < remainder) 1 else 0
            ExpenseShare(userId = userId, amount = MoneyUtils.fromCents(cents))
        }
    }
}
