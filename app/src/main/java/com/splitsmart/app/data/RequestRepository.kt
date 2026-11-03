package com.splitsmart.app.data

import com.splitsmart.app.model.AppRequest
import com.splitsmart.app.model.RequestStatus
import java.util.UUID

class RequestRepository(
    private val firestore: FirestoreManager = FirestoreManager.getInstance()
) {
    suspend fun save(request: AppRequest): AppRequest {
        val id = request.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = request.copy(id = id)
        firestore.setDocument(FirestoreManager.COLLECTION_REQUESTS, id, toSave.toMap())
        return toSave
    }

    suspend fun getById(id: String): AppRequest? {
        val data = firestore.getDocument(FirestoreManager.COLLECTION_REQUESTS, id) ?: return null
        return AppRequest.fromMap(data)
    }

    suspend fun listIncoming(userId: String): List<AppRequest> {
        return firestore.queryEquals(FirestoreManager.COLLECTION_REQUESTS, "toUserId", userId)
            .map { AppRequest.fromMap(it) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun listOutgoing(userId: String): List<AppRequest> {
        return firestore.queryEquals(FirestoreManager.COLLECTION_REQUESTS, "fromUserId", userId)
            .map { AppRequest.fromMap(it) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun updateStatus(id: String, status: RequestStatus): AppRequest {
        val existing = getById(id) ?: throw IllegalArgumentException("Request not found")
        val updated = existing.copy(status = status.name)
        firestore.setDocument(FirestoreManager.COLLECTION_REQUESTS, id, updated.toMap())
        return updated
    }

    companion object {
        @Volatile
        private var instance: RequestRepository? = null

        fun getInstance(): RequestRepository {
            return instance ?: synchronized(this) {
                instance ?: RequestRepository().also { instance = it }
            }
        }
    }
}
