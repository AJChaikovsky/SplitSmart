package com.splitsmart.app.model

data class Group(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var ownerId: String = "",
    var memberEmails: List<String> = emptyList(),
    var memberIds: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "ownerId" to ownerId,
        "memberEmails" to memberEmails,
        "memberIds" to memberIds,
        "createdAt" to createdAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Group = Group(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            description = map["description"] as? String ?: "",
            ownerId = map["ownerId"] as? String ?: "",
            memberEmails = (map["memberEmails"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            memberIds = (map["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
