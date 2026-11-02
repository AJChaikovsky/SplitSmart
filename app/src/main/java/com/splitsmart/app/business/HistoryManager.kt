package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.Transaction
import com.splitsmart.app.model.TransactionType

class HistoryManager(
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance()
) {
    enum class SortOrder { NEWEST, OLDEST }

    data class HistoryFilter(
        val keyword: String? = null,
        val type: TransactionType? = null,
        val category: ExpenseCategory? = null,
        val memberId: String? = null,
        val sortOrder: SortOrder = SortOrder.NEWEST
    )

    suspend fun getHistory(groupId: String, filter: HistoryFilter = HistoryFilter()): List<Transaction> {
        val expenses = expenseRepository.getByGroup(groupId).map { Transaction.fromExpense(it) }
        val settlements = settlementRepository.getByGroup(groupId).map { Transaction.fromSettlement(it) }
        var merged = expenses + settlements

        filter.keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { kw ->
            val lower = kw.lowercase()
            merged = merged.filter {
                it.title.lowercase().contains(lower) || it.note.lowercase().contains(lower)
            }
        }

        filter.type?.let { t ->
            merged = merged.filter { it.type == t }
        }

        filter.category?.let { cat ->
            merged = merged.filter {
                it.type == TransactionType.EXPENSE && it.category.equals(cat.name, ignoreCase = true)
            }
        }

        filter.memberId?.let { mid ->
            merged = merged.filter { mid in it.memberIds }
        }

        merged = when (filter.sortOrder) {
            SortOrder.NEWEST -> merged.sortedWith(
                compareByDescending<Transaction> { it.date }.thenByDescending { it.id }
            )
            SortOrder.OLDEST -> merged.sortedWith(
                compareBy<Transaction> { it.date }.thenBy { it.id }
            )
        }
        return merged
    }

    companion object {
        @Volatile
        private var instance: HistoryManager? = null

        fun getInstance(): HistoryManager {
            return instance ?: synchronized(this) {
                instance ?: HistoryManager().also { instance = it }
            }
        }
    }
}
