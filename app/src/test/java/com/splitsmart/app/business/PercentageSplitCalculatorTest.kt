package com.splitsmart.app.business

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentageSplitCalculatorTest {
    private val calculator = PercentageSplitCalculator()

    @Test
    fun convertsPercentagesWithCentRounding() {
        val shares = calculator.calculate(
            totalAmount = 100.0,
            participantIds = listOf("a", "b"),
            percentages = mapOf("a" to 50.0, "b" to 50.0)
        )
        assertEquals(50.0, shares[0].amount, 0.0)
        assertEquals(50.0, shares[1].amount, 0.0)
        assertEquals(100.0, shares.sumOf { it.amount }, 0.0)
    }

    @Test
    fun leftoverCentsGoToFirstParticipants() {
        // 33.33% + 33.33% + 33.34% of $10.00
        val shares = calculator.calculate(
            totalAmount = 10.0,
            participantIds = listOf("a", "b", "c"),
            percentages = mapOf("a" to 33.33, "b" to 33.33, "c" to 33.34)
        )
        assertEquals(10.0, shares.sumOf { it.amount }, 0.0)
        assertEquals(3, shares.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPercentagesNot100() {
        calculator.calculate(
            totalAmount = 10.0,
            participantIds = listOf("a", "b"),
            percentages = mapOf("a" to 40.0, "b" to 40.0)
        )
    }
}
