package com.splitsmart.app.model

data class Settlement(
    var id: String = "",
    var groupId: String = "",
    var fromUserId: String = "",
    var toUserId: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var note: String = "",
    var creatorId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "groupId" to groupId,
        "fromUserId" to fromUserId,
        "toUserId" to toUserId,
        "amount" to amount,
        "date" to date,
        "note" to note,
        "creatorId" to creatorId
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Settlement = Settlement(
            id = map["id"] as? String ?: "",
            groupId = map["groupId"] as? String ?: "",
            fromUserId = map["fromUserId"] as? String ?: "",
            toUserId = map["toUserId"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            date = map["date"] as? String ?: "",
            note = map["note"] as? String ?: "",
            creatorId = map["creatorId"] as? String ?: ""
        )
    }
}
