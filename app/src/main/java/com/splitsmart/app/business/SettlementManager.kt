package com.splitsmart.app.business

import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.model.Settlement

class SettlementManager(
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance(),
    private val balanceManager: BalanceManager = BalanceManager.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance()
) {
    suspend fun createSettlement(
        groupId: String,
        toUserId: String,
        amount: Double,
        date: String,
        note: String
    ): Settlement {
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(amount > 0) { "Amount must be greater than 0" }
        require(MoneyUtils.hasAtMostTwoDecimals(amount)) { "Amount may have at most 2 decimal places" }
        require(date.isNotBlank()) { "Date is required" }
        require(toUserId != currentUid) { "Cannot settle with yourself" }

        val balances = balanceManager.getBalancesForGroup(groupId)
        val owed = balanceManager.amountOwed(currentUid, toUserId, balances)
        require(owed > 0) { "You do not owe this user anything" }
        require(amount <= owed + 0.001) { "Amount cannot exceed what you owe ($owed)" }

        val settlement = Settlement(
            groupId = groupId,
            fromUserId = currentUid,
            toUserId = toUserId,
            amount = MoneyUtils.roundToCents(amount),
            date = date.trim(),
            note = note.trim(),
            creatorId = currentUid
        )
        return settlementRepository.save(settlement)
    }

    suspend fun editSettlement(
        settlementId: String,
        amount: Double,
        date: String,
        note: String
    ): Settlement {
        val existing = settlementRepository.getById(settlementId)
            ?: throw IllegalArgumentException("Settlement not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(existing.creatorId == currentUid) { "Only the creator can edit this settlement" }
        require(amount > 0) { "Amount must be greater than 0" }
        require(MoneyUtils.hasAtMostTwoDecimals(amount)) { "Amount may have at most 2 decimal places" }
        require(date.isNotBlank()) { "Date is required" }

        // Re-check owed excluding this settlement's effect: add it back temporarily
        val balances = balanceManager.getBalancesForGroup(existing.groupId)
        val currentOwed = balanceManager.amountOwed(existing.fromUserId, existing.toUserId, balances)
        val maxAllowed = currentOwed + existing.amount
        require(amount <= maxAllowed + 0.001) {
            "Amount cannot exceed what is owed ($maxAllowed)"
        }

        val updated = existing.copy(
            amount = MoneyUtils.roundToCents(amount),
            date = date.trim(),
            note = note.trim()
        )
        return settlementRepository.save(updated)
    }

    suspend fun deleteSettlement(settlementId: String) {
        val existing = settlementRepository.getById(settlementId)
            ?: throw IllegalArgumentException("Settlement not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(existing.creatorId == currentUid) { "Only the creator can delete this settlement" }
        settlementRepository.delete(settlementId)
    }

    suspend fun getSettlement(settlementId: String): Settlement {
        return settlementRepository.getById(settlementId)
            ?: throw IllegalArgumentException("Settlement not found")
    }

    companion object {
        @Volatile
        private var instance: SettlementManager? = null

        fun getInstance(): SettlementManager {
            return instance ?: synchronized(this) {
                instance ?: SettlementManager().also { instance = it }
            }
        }
    }
}
