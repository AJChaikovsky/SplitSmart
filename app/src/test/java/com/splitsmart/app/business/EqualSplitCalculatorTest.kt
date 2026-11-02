package com.splitsmart.app.business

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualSplitCalculatorTest {
    private val calculator = EqualSplitCalculator()

    @Test
    fun dividesEvenlyWhenCentsDivideCleanly() {
        val shares = calculator.calculate(30.0, listOf("a", "b", "c"))
        assertEquals(3, shares.size)
        shares.forEach { assertEquals(10.0, it.amount, 0.0) }
        assertEquals(30.0, shares.sumOf { it.amount }, 0.0)
    }

    @Test
    fun assignsLeftoverCentsToFirstParticipants() {
        // $10.00 / 3 = 333, 333, 334 cents -> first gets leftover
        val shares = calculator.calculate(10.0, listOf("a", "b", "c"))
        assertEquals(3.34, shares[0].amount, 0.0)
        assertEquals(3.33, shares[1].amount, 0.0)
        assertEquals(3.33, shares[2].amount, 0.0)
        assertEquals(10.0, shares.sumOf { it.amount }, 0.0)
    }

    @Test
    fun twoParticipantsOddCent() {
        val shares = calculator.calculate(0.01, listOf("a", "b"))
        assertEquals(0.01, shares.sumOf { it.amount }, 0.0)
        assertEquals(0.01, shares[0].amount, 0.0)
        assertEquals(0.0, shares[1].amount, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyParticipants() {
        calculator.calculate(10.0, emptyList())
    }
}
