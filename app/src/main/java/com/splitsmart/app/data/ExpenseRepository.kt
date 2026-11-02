package com.splitsmart.app.data

import com.splitsmart.app.model.Expense
import java.util.UUID

class ExpenseRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(expense: Expense): Expense {
        val id = expense.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = expense.copy(id = id)
        firestore.setDocument(FirestoreManager.COLLECTION_EXPENSES, id, toSave.toMap())
        return toSave
    }

    suspend fun getById(id: String): Expense? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_EXPENSES, id) ?: return null
        return Expense.fromMap(data)
    }

    suspend fun getByGroup(groupId: String): List<Expense> {
        val results = firestore.queryEquals(
            FirestoreManager.COLLECTION_EXPENSES,
            "groupId",
            groupId
        )
        return results.map { Expense.fromMap(it) }.sortedByDescending { it.date }
    }

    suspend fun delete(id: String) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_EXPENSES, id)
    }

    suspend fun deleteByGroup(groupId: String) {
        getByGroup(groupId).forEach { delete(it.id) }
    }

    companion object {
        @Volatile
        private var instance: ExpenseRepository? = null

        fun getInstance(): ExpenseRepository {
            return instance ?: synchronized(this) {
                instance ?: ExpenseRepository().also { instance = it }
            }
        }
    }
}
