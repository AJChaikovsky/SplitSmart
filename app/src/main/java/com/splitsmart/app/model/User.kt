package com.splitsmart.app.model

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var uscId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "uscId" to uscId
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): User = User(
            uid = map["uid"] as? String ?: "",
            name = map["name"] as? String ?: "",
            email = map["email"] as? String ?: "",
            uscId = map["uscId"] as? String ?: ""
        )
    }
}
