package com.splitsmart.app.business

import com.splitsmart.app.model.ExpenseShare

interface SplitCalculator {
    /**
     * @param totalAmount expense total (dollars with up to 2 decimals)
     * @param participantIds ordered list of participants
     * @param exactAmounts optional map of userId -> exact dollar amount (for EXACT)
     * @param percentages optional map of userId -> percentage (for PERCENTAGE)
     * @return list of ExpenseShare whose amounts sum to totalAmount
     */
    fun calculate(
        totalAmount: Double,
        participantIds: List<String>,
        exactAmounts: Map<String, Double> = emptyMap(),
        percentages: Map<String, Double> = emptyMap()
    ): List<ExpenseShare>
}

object MoneyUtils {
    fun toCents(amount: Double): Long = Math.round(amount * 100.0)

    fun fromCents(cents: Long): Double = cents / 100.0

    fun roundToCents(amount: Double): Double = fromCents(toCents(amount))

    fun hasAtMostTwoDecimals(amount: Double): Boolean {
        val cents = amount * 100.0
        return Math.abs(cents - Math.round(cents)) < 1e-9
    }

    private val isoDate = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun requireIsoDate(date: String) {
        require(isoDate.matches(date.trim())) {
            "Date must be in YYYY-MM-DD format"
        }
    }
}
