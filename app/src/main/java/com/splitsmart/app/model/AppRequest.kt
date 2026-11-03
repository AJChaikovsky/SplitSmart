package com.splitsmart.app.model

enum class RequestType {
    FRIEND,
    GROUP_INVITE;

    companion object {
        fun fromString(value: String?): RequestType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: FRIEND
    }
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DENIED;

    companion object {
        fun fromString(value: String?): RequestStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class AppRequest(
    var id: String = "",
    var type: String = RequestType.FRIEND.name,
    var status: String = RequestStatus.PENDING.name,
    var fromUserId: String = "",
    var toUserId: String = "",
    var groupId: String = "",
    var groupName: String = "",
    var createdAt: Long = 0L
) {
    fun typeEnum(): RequestType = RequestType.fromString(type)
    fun statusEnum(): RequestStatus = RequestStatus.fromString(status)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type,
        "status" to status,
        "fromUserId" to fromUserId,
        "toUserId" to toUserId,
        "groupId" to groupId,
        "groupName" to groupName,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): AppRequest = AppRequest(
            id = map["id"] as? String ?: "",
            type = map["type"] as? String ?: RequestType.FRIEND.name,
            status = map["status"] as? String ?: RequestStatus.PENDING.name,
            fromUserId = map["fromUserId"] as? String ?: "",
            toUserId = map["toUserId"] as? String ?: "",
            groupId = map["groupId"] as? String ?: "",
            groupName = map["groupName"] as? String ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}

/** Incoming request with sender display info for the Requests tab. */
data class IncomingRequest(
    val request: AppRequest,
    val fromName: String,
    val fromEmail: String
) {
    fun headline(): String = when (request.typeEnum()) {
        RequestType.FRIEND -> "$fromName wants to add you as a friend"
        RequestType.GROUP_INVITE -> {
            val group = request.groupName.ifBlank { "a group" }
            "$fromName invited you to the group \"$group\""
        }
    }
}
