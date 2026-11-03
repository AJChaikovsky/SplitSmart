package com.splitsmart.app.model

enum class ActivityKind {
    EXPENSE,
    SETTLEMENT
}

data class ActivityItem(
    val id: String,
    val kind: ActivityKind,
    val groupId: String,
    val groupName: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: String,
    val actorName: String
)
