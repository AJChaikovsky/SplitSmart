package com.splitsmart.app.model

enum class TransactionType {
    EXPENSE,
    SETTLEMENT
}

data class Transaction(
    var id: String = "",
    var type: TransactionType = TransactionType.EXPENSE,
    var groupId: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var category: String = "",
    var note: String = "",
    var memberIds: List<String> = emptyList(),
    var expense: Expense? = null,
    var settlement: Settlement? = null
) {
    companion object {
        fun fromExpense(expense: Expense): Transaction = Transaction(
            id = expense.id,
            type = TransactionType.EXPENSE,
            groupId = expense.groupId,
            title = expense.title,
            amount = expense.amount,
            date = expense.date,
            category = expense.category,
            note = expense.note,
            memberIds = expense.participantIds,
            expense = expense
        )

        fun fromSettlement(settlement: Settlement): Transaction = Transaction(
            id = settlement.id,
            type = TransactionType.SETTLEMENT,
            groupId = settlement.groupId,
            title = "Settlement",
            amount = settlement.amount,
            date = settlement.date,
            category = "",
            note = settlement.note,
            memberIds = listOf(settlement.fromUserId, settlement.toUserId),
            settlement = settlement
        )
    }
}
