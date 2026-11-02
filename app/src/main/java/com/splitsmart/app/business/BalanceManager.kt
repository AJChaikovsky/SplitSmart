package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.model.Expense
import com.splitsmart.app.model.Settlement

/**
 * Pairwise balances from expenses + settlements.
 * Non-payer participants owe the payer their share.
 * totalToPay and totalToReceive are shown separately (not netted into one number for display totals).
 * No debt simplification across triples.
 */
class BalanceManager(
    private val expenseRepository: ExpenseRepository? = null,
    private val settlementRepository: SettlementRepository? = null
) {
    private fun expenses(): ExpenseRepository =
        expenseRepository ?: ExpenseRepository.getInstance()

    private fun settlements(): SettlementRepository =
        settlementRepository ?: SettlementRepository.getInstance()
    data class PairBalance(
        val fromUserId: String,
        val toUserId: String,
        val amount: Double
    )

    data class UserBalanceSummary(
        val userId: String,
        val totalToPay: Double,
        val totalToReceive: Double,
        val debts: List<PairBalance>
    )

    /**
     * Computes net pairwise balances. Positive amount means [fromUserId] owes [toUserId].
     */
    fun computePairwiseBalances(
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<PairBalance> {
        // keyed by unordered pair, stored as smallerUid|largerUid -> signed net where
        // positive means smaller owes larger? Better: use directed map from->to.
        val directed = mutableMapOf<Pair<String, String>, Long>()

        fun addOwes(from: String, to: String, cents: Long) {
            if (from == to || cents == 0L) return
            val key = from to to
            directed[key] = (directed[key] ?: 0L) + cents
        }

        expenses.forEach { expense ->
            val payer = expense.payerId
            expense.shares.forEach { share ->
                if (share.userId != payer) {
                    addOwes(share.userId, payer, MoneyUtils.toCents(share.amount))
                }
            }
        }

        settlements.forEach { settlement ->
            // Settlement: fromUser pays toUser, reducing what from owes to
            addOwes(settlement.fromUserId, settlement.toUserId, -MoneyUtils.toCents(settlement.amount))
        }

        // Net each unordered pair into a single directed debt
        val seen = mutableSetOf<Set<String>>()
        val results = mutableListOf<PairBalance>()

        val users = directed.keys.flatMap { listOf(it.first, it.second) }.toSet()
        for (a in users) {
            for (b in users) {
                if (a >= b) continue
                val pair = setOf(a, b)
                if (!seen.add(pair)) continue

                val aOwesB = directed[a to b] ?: 0L
                val bOwesA = directed[b to a] ?: 0L
                val net = aOwesB - bOwesA
                when {
                    net > 0 -> results.add(
                        PairBalance(fromUserId = a, toUserId = b, amount = MoneyUtils.fromCents(net))
                    )
                    net < 0 -> results.add(
                        PairBalance(fromUserId = b, toUserId = a, amount = MoneyUtils.fromCents(-net))
                    )
                }
            }
        }
        return results.sortedByDescending { it.amount }
    }

    fun summarizeForUser(userId: String, balances: List<PairBalance>): UserBalanceSummary {
        val debts = balances.filter { it.fromUserId == userId || it.toUserId == userId }
        val totalToPay = debts.filter { it.fromUserId == userId }.sumOf { it.amount }
        val totalToReceive = debts.filter { it.toUserId == userId }.sumOf { it.amount }
        return UserBalanceSummary(
            userId = userId,
            totalToPay = MoneyUtils.roundToCents(totalToPay),
            totalToReceive = MoneyUtils.roundToCents(totalToReceive),
            debts = debts
        )
    }

    suspend fun getBalancesForGroup(groupId: String): List<PairBalance> {
        val expenses = expenses().getByGroup(groupId)
        val settlements = settlements().getByGroup(groupId)
        return computePairwiseBalances(expenses, settlements)
    }

    suspend fun getUserSummary(groupId: String, userId: String): UserBalanceSummary {
        return summarizeForUser(userId, getBalancesForGroup(groupId))
    }

    fun amountOwed(fromUserId: String, toUserId: String, balances: List<PairBalance>): Double {
        return balances.find { it.fromUserId == fromUserId && it.toUserId == toUserId }?.amount ?: 0.0
    }

    companion object {
        @Volatile
        private var instance: BalanceManager? = null

        fun getInstance(): BalanceManager {
            return instance ?: synchronized(this) {
                instance ?: BalanceManager().also { instance = it }
            }
        }
    }
}
