package com.splitsmart.app.business

import com.splitsmart.app.model.ExpenseShare

/**
 * Validates percentages sum to 100%, converts to amounts with cent rounding so sum equals total.
 * Leftover cents from rounding are assigned to the first participant.
 */
class PercentageSplitCalculator : SplitCalculator {
    override fun calculate(
        totalAmount: Double,
        participantIds: List<String>,
        exactAmounts: Map<String, Double>,
        percentages: Map<String, Double>
    ): List<ExpenseShare> {
        require(participantIds.isNotEmpty()) { "At least one participant is required" }
        require(totalAmount > 0) { "Amount must be greater than 0" }

        participantIds.forEach { id ->
            require(percentages.containsKey(id)) { "Missing percentage for participant $id" }
            val pct = percentages[id]!!
            require(pct >= 0) { "Percentages must be non-negative" }
        }

        val pctSum = participantIds.sumOf { percentages[it]!! }
        require(Math.abs(pctSum - 100.0) < 0.01) {
            "Percentages must sum to 100% (got $pctSum)"
        }

        val totalCents = MoneyUtils.toCents(totalAmount)
        val rawCents = participantIds.map { id ->
            val pct = percentages[id]!!
            Math.floor(totalCents * pct / 100.0).toLong()
        }.toMutableList()

        var assigned = rawCents.sum()
        var leftover = totalCents - assigned
        var i = 0
        while (leftover > 0 && i < rawCents.size) {
            rawCents[i] = rawCents[i] + 1
            leftover--
            i++
        }

        return participantIds.mapIndexed { index, id ->
            ExpenseShare(
                userId = id,
                amount = MoneyUtils.fromCents(rawCents[index]),
                percentage = percentages[id]
            )
        }
    }
}
