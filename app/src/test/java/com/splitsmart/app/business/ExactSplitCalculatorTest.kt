package com.splitsmart.app.business

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactSplitCalculatorTest {
    private val calculator = ExactSplitCalculator()

    @Test
    fun acceptsAmountsThatSumToTotal() {
        val shares = calculator.calculate(
            totalAmount = 50.0,
            participantIds = listOf("a", "b"),
            exactAmounts = mapOf("a" to 20.0, "b" to 30.0)
        )
        assertEquals(20.0, shares[0].amount, 0.0)
        assertEquals(30.0, shares[1].amount, 0.0)
    }

    @Test
    fun acceptsWithinOneCentTolerance() {
        val shares = calculator.calculate(
            totalAmount = 10.00,
            participantIds = listOf("a", "b"),
            exactAmounts = mapOf("a" to 5.00, "b" to 5.00)
        )
        assertEquals(10.0, shares.sumOf { it.amount }, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWhenSumDoesNotMatch() {
        calculator.calculate(
            totalAmount = 50.0,
            participantIds = listOf("a", "b"),
            exactAmounts = mapOf("a" to 10.0, "b" to 10.0)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingParticipantAmount() {
        calculator.calculate(
            totalAmount = 10.0,
            participantIds = listOf("a", "b"),
            exactAmounts = mapOf("a" to 10.0)
        )
    }
}
