package com.splitsmart.app.data

import com.splitsmart.app.model.User

class UserRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(user: User) {
        firestore.setDocument(FirestoreManager.COLLECTION_USERS, user.uid, user.toMap())
    }

    suspend fun getById(uid: String): User? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_USERS, uid) ?: return null
        return User.fromMap(data)
    }

    suspend fun getByEmail(email: String): User? {
        val results = firestore.queryEquals(
            FirestoreManager.COLLECTION_USERS,
            "email",
            email.lowercase()
        )
        return results.firstOrNull()?.let { User.fromMap(it) }
    }

    suspend fun getByUscId(uscId: String): User? {
        val results = firestore.queryEquals(
            FirestoreManager.COLLECTION_USERS,
            "uscId",
            uscId.trim()
        )
        return results.firstOrNull()?.let { User.fromMap(it) }
    }

    suspend fun updateName(uid: String, name: String) {
        firestore.updateDocument(
            FirestoreManager.COLLECTION_USERS,
            uid,
            mapOf("name" to name)
        )
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository().also { instance = it }
            }
        }
    }
}
