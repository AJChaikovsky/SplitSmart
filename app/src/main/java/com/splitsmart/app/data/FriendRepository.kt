package com.splitsmart.app.data

import com.splitsmart.app.model.Friendship

class FriendRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(friendship: Friendship): Friendship {
        val id = friendship.id.ifBlank {
            require(friendship.userIds.size == 2) { "Friendship needs two users" }
            Friendship.docIdFor(friendship.userIds[0], friendship.userIds[1])
        }
        val toSave = friendship.copy(id = id)
        firestore.setDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, id, toSave.toMap())
        return toSave
    }

    suspend fun getById(id: String): Friendship? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, id) ?: return null
        return Friendship.fromMap(data)
    }

    suspend fun listForUser(userId: String): List<Friendship> {
        val results = firestore.queryArrayContains(
            FirestoreManager.COLLECTION_FRIENDSHIPS,
            "userIds",
            userId
        )
        return results.map { Friendship.fromMap(it) }.sortedByDescending { it.createdAt }
    }

    suspend fun delete(id: String) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, id)
    }

    companion object {
        @Volatile
        private var instance: FriendRepository? = null

        fun getInstance(): FriendRepository {
            return instance ?: synchronized(this) {
                instance ?: FriendRepository().also { instance = it }
            }
        }
    }
}
