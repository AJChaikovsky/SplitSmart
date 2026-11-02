package com.splitsmart.app.business

import com.splitsmart.app.model.Expense
import com.splitsmart.app.model.ExpenseShare
import com.splitsmart.app.model.Settlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceManagerTest {
    private val manager = BalanceManager()

    @Test
    fun expenseCreatesPairwiseDebtFromNonPayersToPayer() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            title = "Dinner",
            amount = 30.0,
            payerId = "alice",
            participantIds = listOf("alice", "bob", "carol"),
            shares = listOf(
                ExpenseShare("alice", 10.0),
                ExpenseShare("bob", 10.0),
                ExpenseShare("carol", 10.0)
            )
        )
        val balances = manager.computePairwiseBalances(listOf(expense), emptyList())
        assertEquals(2, balances.size)
        assertTrue(balances.any { it.fromUserId == "bob" && it.toUserId == "alice" && it.amount == 10.0 })
        assertTrue(balances.any { it.fromUserId == "carol" && it.toUserId == "alice" && it.amount == 10.0 })
    }

    @Test
    fun settlementReducesOwedAmount() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            title = "Taxi",
            amount = 20.0,
            payerId = "alice",
            participantIds = listOf("alice", "bob"),
            shares = listOf(
                ExpenseShare("alice", 10.0),
                ExpenseShare("bob", 10.0)
            )
        )
        val settlement = Settlement(
            id = "s1",
            groupId = "g1",
            fromUserId = "bob",
            toUserId = "alice",
            amount = 4.0
        )
        val balances = manager.computePairwiseBalances(listOf(expense), listOf(settlement))
        assertEquals(1, balances.size)
        assertEquals("bob", balances[0].fromUserId)
        assertEquals("alice", balances[0].toUserId)
        assertEquals(6.0, balances[0].amount, 0.0)
    }

    @Test
    fun netsPairwiseWithoutSimplifyingTriples() {
        // alice paid for bob ($5) and carol paid for bob ($5) — bob owes both, no triple simplification
        val e1 = Expense(
            id = "e1",
            payerId = "alice",
            participantIds = listOf("alice", "bob"),
            shares = listOf(ExpenseShare("alice", 0.0), ExpenseShare("bob", 5.0)),
            amount = 5.0
        )
        val e2 = Expense(
            id = "e2",
            payerId = "carol",
            participantIds = listOf("carol", "bob"),
            shares = listOf(ExpenseShare("carol", 0.0), ExpenseShare("bob", 5.0)),
            amount = 5.0
        )
        val balances = manager.computePairwiseBalances(listOf(e1, e2), emptyList())
        assertEquals(2, balances.size)
        assertTrue(balances.any { it.fromUserId == "bob" && it.toUserId == "alice" })
        assertTrue(balances.any { it.fromUserId == "bob" && it.toUserId == "carol" })
    }

    @Test
    fun totalToPayAndReceiveAreSeparate() {
        val expense = Expense(
            id = "e1",
            payerId = "alice",
            participantIds = listOf("alice", "bob"),
            shares = listOf(ExpenseShare("alice", 10.0), ExpenseShare("bob", 10.0)),
            amount = 20.0
        )
        val expense2 = Expense(
            id = "e2",
            payerId = "bob",
            participantIds = listOf("alice", "bob"),
            shares = listOf(ExpenseShare("alice", 3.0), ExpenseShare("bob", 3.0)),
            amount = 6.0
        )
        val balances = manager.computePairwiseBalances(listOf(expense, expense2), emptyList())
        val aliceSummary = manager.summarizeForUser("alice", balances)
        // bob owes alice 10, alice owes bob 3 => net bob owes alice 7
        // After pairwise netting there should be one debt
        assertEquals(1, balances.size)
        assertEquals(0.0, aliceSummary.totalToPay, 0.0)
        assertEquals(7.0, aliceSummary.totalToReceive, 0.0)

        val bobSummary = manager.summarizeForUser("bob", balances)
        assertEquals(7.0, bobSummary.totalToPay, 0.0)
        assertEquals(0.0, bobSummary.totalToReceive, 0.0)
    }
}
