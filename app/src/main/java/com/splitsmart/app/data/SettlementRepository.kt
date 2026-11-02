package com.splitsmart.app.data

import com.splitsmart.app.model.Settlement
import java.util.UUID

class SettlementRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(settlement: Settlement): Settlement {
        val id = settlement.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = settlement.copy(id = id)
        firestore.setDocument(FirestoreManager.COLLECTION_SETTLEMENTS, id, toSave.toMap())
        return toSave
    }

    suspend fun getById(id: String): Settlement? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_SETTLEMENTS, id) ?: return null
        return Settlement.fromMap(data)
    }

    suspend fun getByGroup(groupId: String): List<Settlement> {
        val results = firestore.queryEquals(
            FirestoreManager.COLLECTION_SETTLEMENTS,
            "groupId",
            groupId
        )
        return results.map { Settlement.fromMap(it) }.sortedByDescending { it.date }
    }

    suspend fun delete(id: String) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_SETTLEMENTS, id)
    }

    suspend fun deleteByGroup(groupId: String) {
        getByGroup(groupId).forEach { delete(it.id) }
    }

    companion object {
        @Volatile
        private var instance: SettlementRepository? = null

        fun getInstance(): SettlementRepository {
            return instance ?: synchronized(this) {
                instance ?: SettlementRepository().also { instance = it }
            }
        }
    }
}
