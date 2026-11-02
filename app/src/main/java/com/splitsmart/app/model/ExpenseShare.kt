package com.splitsmart.app.model

data class ExpenseShare(
    var userId: String = "",
    var amount: Double = 0.0,
    var percentage: Double? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "amount" to amount,
        "percentage" to percentage
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ExpenseShare = ExpenseShare(
            userId = map["userId"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            percentage = (map["percentage"] as? Number)?.toDouble()
        )
    }
}
