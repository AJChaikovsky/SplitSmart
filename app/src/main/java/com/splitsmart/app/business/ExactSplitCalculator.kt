package com.splitsmart.app.business

import com.splitsmart.app.model.ExpenseShare

/**
 * Validates that exact amounts sum to the total (within 0.01) and returns those shares.
 */
class ExactSplitCalculator : SplitCalculator {
    override fun calculate(
        totalAmount: Double,
        participantIds: List<String>,
        exactAmounts: Map<String, Double>,
        percentages: Map<String, Double>
    ): List<ExpenseShare> {
        require(participantIds.isNotEmpty()) { "At least one participant is required" }
        require(totalAmount > 0) { "Amount must be greater than 0" }

        participantIds.forEach { id ->
            require(exactAmounts.containsKey(id)) { "Missing exact amount for participant $id" }
            val amount = exactAmounts[id]!!
            require(amount >= 0) { "Exact amounts must be non-negative" }
            require(MoneyUtils.hasAtMostTwoDecimals(amount)) { "Exact amounts must have at most 2 decimals" }
        }

        val sum = participantIds.sumOf { MoneyUtils.toCents(exactAmounts[it]!!) }
        val totalCents = MoneyUtils.toCents(totalAmount)
        require(Math.abs(sum - totalCents) <= 1) {
            "Exact amounts must sum to total (within 0.01). Sum=${MoneyUtils.fromCents(sum)}, total=$totalAmount"
        }

        return participantIds.map { id ->
            ExpenseShare(userId = id, amount = MoneyUtils.roundToCents(exactAmounts[id]!!))
        }
    }
}
