package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.model.Expense
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.ExpenseShare
import com.splitsmart.app.model.SplitMethod

class ExpenseManager(
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance()
) {
    fun validateExpenseInput(
        amount: Double,
        payerId: String,
        participantIds: List<String>,
        groupMemberIds: List<String>
    ) {
        require(amount > 0) { "Amount must be greater than 0" }
        require(MoneyUtils.hasAtMostTwoDecimals(amount)) { "Amount may have at most 2 decimal places" }
        require(participantIds.size >= 2) { "At least 2 participants are required" }
        require(payerId in participantIds) { "Payer must be one of the participants" }
        require(participantIds.all { it in groupMemberIds }) {
            "All participants must be members of the group"
        }
        require(payerId in groupMemberIds) { "Payer must be a member of the group" }
    }

    fun computeShares(
        amount: Double,
        participantIds: List<String>,
        splitMethod: SplitMethod,
        exactAmounts: Map<String, Double> = emptyMap(),
        percentages: Map<String, Double> = emptyMap()
    ): List<ExpenseShare> {
        val calculator: SplitCalculator = when (splitMethod) {
            SplitMethod.EQUAL -> EqualSplitCalculator()
            SplitMethod.EXACT -> ExactSplitCalculator()
            SplitMethod.PERCENTAGE -> PercentageSplitCalculator()
        }
        return calculator.calculate(amount, participantIds, exactAmounts, percentages)
    }

    suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        date: String,
        category: ExpenseCategory,
        payerId: String,
        participantIds: List<String>,
        splitMethod: SplitMethod,
        note: String,
        exactAmounts: Map<String, Double> = emptyMap(),
        percentages: Map<String, Double> = emptyMap()
    ): Expense {
        require(title.isNotBlank()) { "Title is required" }
        MoneyUtils.requireIsoDate(date)

        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val creatorId = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(creatorId in group.memberIds) { "You must be a group member to add expenses" }

        validateExpenseInput(amount, payerId, participantIds, group.memberIds)
        val shares = computeShares(amount, participantIds, splitMethod, exactAmounts, percentages)

        val expense = Expense(
            groupId = groupId,
            title = title.trim(),
            amount = MoneyUtils.roundToCents(amount),
            date = date.trim(),
            category = category.name,
            payerId = payerId,
            participantIds = participantIds,
            splitMethod = splitMethod.name,
            note = note.trim(),
            creatorId = creatorId,
            shares = shares
        )
        return expenseRepository.save(expense)
    }

    suspend fun editExpense(
        expenseId: String,
        title: String,
        amount: Double,
        date: String,
        category: ExpenseCategory,
        payerId: String,
        participantIds: List<String>,
        splitMethod: SplitMethod,
        note: String,
        exactAmounts: Map<String, Double> = emptyMap(),
        percentages: Map<String, Double> = emptyMap()
    ): Expense {
        val existing = expenseRepository.getById(expenseId)
            ?: throw IllegalArgumentException("Expense not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(existing.creatorId == currentUid) { "Only the creator can edit this expense" }

        val group = groupRepository.getById(existing.groupId)
            ?: throw IllegalArgumentException("Group not found")

        require(title.isNotBlank()) { "Title is required" }
        MoneyUtils.requireIsoDate(date)
        validateExpenseInput(amount, payerId, participantIds, group.memberIds)
        val shares = computeShares(amount, participantIds, splitMethod, exactAmounts, percentages)

        val updated = existing.copy(
            title = title.trim(),
            amount = MoneyUtils.roundToCents(amount),
            date = date.trim(),
            category = category.name,
            payerId = payerId,
            participantIds = participantIds,
            splitMethod = splitMethod.name,
            note = note.trim(),
            shares = shares
        )
        return expenseRepository.save(updated)
    }

    suspend fun deleteExpense(expenseId: String) {
        val existing = expenseRepository.getById(expenseId)
            ?: throw IllegalArgumentException("Expense not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(existing.creatorId == currentUid) { "Only the creator can delete this expense" }
        expenseRepository.delete(expenseId)
    }

    suspend fun getExpense(expenseId: String): Expense {
        return expenseRepository.getById(expenseId)
            ?: throw IllegalArgumentException("Expense not found")
    }

    suspend fun listExpenses(groupId: String): List<Expense> {
        return expenseRepository.getByGroup(groupId)
    }

    companion object {
        @Volatile
        private var instance: ExpenseManager? = null

        fun getInstance(): ExpenseManager {
            return instance ?: synchronized(this) {
                instance ?: ExpenseManager().also { instance = it }
            }
        }
    }
}
