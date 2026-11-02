package com.splitsmart.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreManager(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun users() = db.collection(COLLECTION_USERS)
    fun groups() = db.collection(COLLECTION_GROUPS)
    fun expenses() = db.collection(COLLECTION_EXPENSES)
    fun settlements() = db.collection(COLLECTION_SETTLEMENTS)

    suspend fun getDocument(collection: String, id: String): Map<String, Any?>? {
        val snapshot = db.collection(collection).document(id).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun setDocument(collection: String, id: String, data: Map<String, Any?>) {
        db.collection(collection).document(id).set(data).await()
    }

    suspend fun updateDocument(collection: String, id: String, data: Map<String, Any?>) {
        db.collection(collection).document(id).update(data).await()
    }

    suspend fun deleteDocument(collection: String, id: String) {
        db.collection(collection).document(id).delete().await()
    }

    suspend fun queryEquals(
        collection: String,
        field: String,
        value: Any
    ): List<Map<String, Any?>> {
        val snapshot = db.collection(collection)
            .whereEqualTo(field, value)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    suspend fun queryArrayContains(
        collection: String,
        field: String,
        value: Any
    ): List<Map<String, Any?>> {
        val snapshot = db.collection(collection)
            .whereArrayContains(field, value)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    suspend fun queryEqualsOrdered(
        collection: String,
        field: String,
        value: Any,
        orderBy: String,
        ascending: Boolean = false
    ): List<Map<String, Any?>> {
        val direction = if (ascending) Query.Direction.ASCENDING else Query.Direction.DESCENDING
        val snapshot = db.collection(collection)
            .whereEqualTo(field, value)
            .orderBy(orderBy, direction)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_GROUPS = "groups"
        const val COLLECTION_EXPENSES = "expenses"
        const val COLLECTION_SETTLEMENTS = "settlements"

        @Volatile
        private var instance: FirestoreManager? = null

        fun getInstance(): FirestoreManager {
            return instance ?: synchronized(this) {
                instance ?: FirestoreManager().also { instance = it }
            }
        }
    }
}
