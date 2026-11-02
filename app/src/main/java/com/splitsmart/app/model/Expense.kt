package com.splitsmart.app.model

data class Expense(
    var id: String = "",
    var groupId: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var category: String = ExpenseCategory.OTHER.name,
    var payerId: String = "",
    var participantIds: List<String> = emptyList(),
    var splitMethod: String = SplitMethod.EQUAL.name,
    var note: String = "",
    var creatorId: String = "",
    var shares: List<ExpenseShare> = emptyList()
) {
    fun categoryEnum(): ExpenseCategory = ExpenseCategory.fromString(category)
    fun splitMethodEnum(): SplitMethod = SplitMethod.fromString(splitMethod)

    fun shareMap(): Map<String, Double> = shares.associate { it.userId to it.amount }

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "groupId" to groupId,
        "title" to title,
        "amount" to amount,
        "date" to date,
        "category" to category,
        "payerId" to payerId,
        "participantIds" to participantIds,
        "splitMethod" to splitMethod,
        "note" to note,
        "creatorId" to creatorId,
        "shares" to shares.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Expense {
            val rawShares = map["shares"]
            val sharesList: List<ExpenseShare> = when (rawShares) {
                is List<*> -> rawShares.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { ExpenseShare.fromMap(it as Map<String, Any?>) }
                }
                is Map<*, *> -> rawShares.mapNotNull { (k, v) ->
                    val userId = k as? String ?: return@mapNotNull null
                    val amount = (v as? Number)?.toDouble() ?: return@mapNotNull null
                    ExpenseShare(userId = userId, amount = amount)
                }
                else -> emptyList()
            }
            return Expense(
                id = map["id"] as? String ?: "",
                groupId = map["groupId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                date = map["date"] as? String ?: "",
                category = map["category"] as? String ?: ExpenseCategory.OTHER.name,
                payerId = map["payerId"] as? String ?: "",
                participantIds = (map["participantIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                splitMethod = map["splitMethod"] as? String ?: SplitMethod.EQUAL.name,
                note = map["note"] as? String ?: "",
                creatorId = map["creatorId"] as? String ?: "",
                shares = sharesList
            )
        }
    }
}
