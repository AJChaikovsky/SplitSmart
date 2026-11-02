package com.splitsmart.app.data

import com.splitsmart.app.model.Group
import java.util.UUID

class GroupRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(group: Group): Group {
        val id = group.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = group.copy(id = id)
        firestore.setDocument(FirestoreManager.COLLECTION_GROUPS, id, toSave.toMap())
        return toSave
    }

    suspend fun getById(id: String): Group? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_GROUPS, id) ?: return null
        return Group.fromMap(data)
    }

    suspend fun getGroupsForUser(userId: String): List<Group> {
        val results = firestore.queryArrayContains(
            FirestoreManager.COLLECTION_GROUPS,
            "memberIds",
            userId
        )
        return results.map { Group.fromMap(it) }.sortedByDescending { it.createdAt }
    }

    suspend fun delete(id: String) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_GROUPS, id)
    }

    companion object {
        @Volatile
        private var instance: GroupRepository? = null

        fun getInstance(): GroupRepository {
            return instance ?: synchronized(this) {
                instance ?: GroupRepository().also { instance = it }
            }
        }
    }
}
