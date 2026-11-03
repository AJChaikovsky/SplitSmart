package com.splitsmart.app.model

data class Friendship(
    var id: String = "",
    var userIds: List<String> = emptyList(),
    var createdBy: String = "",
    var createdAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userIds" to userIds,
        "createdBy" to createdBy,
        "createdAt" to createdAt
    )

    companion object {
        fun docIdFor(uidA: String, uidB: String): String {
            val sorted = listOf(uidA, uidB).sorted()
            return "${sorted[0]}_${sorted[1]}"
        }

        fun fromMap(map: Map<String, Any?>): Friendship {
            @Suppress("UNCHECKED_CAST")
            val ids = (map["userIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            return Friendship(
                id = map["id"] as? String ?: "",
                userIds = ids,
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class FriendBalance(
    val friend: User,
    /** Positive => friend owes you; negative => you owe friend. */
    val netAmount: Double
) {
    val youAreOwed: Boolean get() = netAmount > 0.009
    val youOwe: Boolean get() = netAmount < -0.009
    val isSettled: Boolean get() = !youAreOwed && !youOwe
}
