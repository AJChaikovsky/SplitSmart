package com.splitsmart.app.model

data class MonthlySummary(
    var year: Int = 0,
    var month: Int = 0,
    var expenseCount: Int = 0,
    var totalSpending: Double = 0.0,
    var categoryTotals: Map<String, Double> = emptyMap(),
    /** Display name -> amount paid this month. */
    var payerTotals: Map<String, Double> = emptyMap()
) {
    fun isEmpty(): Boolean = expenseCount == 0
}
