package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.ActivityItem
import com.splitsmart.app.model.ActivityKind

class ActivityManager(
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance()
) {
    /**
     * Expenses and settlements across all groups that involve the current user
     * (as payer, participant, creator, or settlement party).
     */
    suspend fun getMyActivity(): List<ActivityItem> {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        val groups = groupRepository.getGroupsForUser(uid)
        val items = mutableListOf<ActivityItem>()
        val nameCache = mutableMapOf<String, String>()

        suspend fun nameOf(userId: String): String {
            nameCache[userId]?.let { return it }
            val name = userRepository.getById(userId)?.name ?: userId.take(8)
            nameCache[userId] = name
            return name
        }

        groups.forEach { group ->
            expenseRepository.getByGroup(group.id).forEach { expense ->
                val involvesMe = uid == expense.payerId ||
                    uid == expense.creatorId ||
                    uid in expense.participantIds
                if (!involvesMe) return@forEach

                val payer = nameOf(expense.payerId)
                val actor = nameOf(expense.creatorId)
                val youLabel = if (expense.creatorId == uid) "You" else actor
                items.add(
                    ActivityItem(
                        id = expense.id,
                        kind = ActivityKind.EXPENSE,
                        groupId = group.id,
                        groupName = group.name,
                        title = expense.title,
                        subtitle = "$youLabel added · paid by $payer · ${expense.category}",
                        amount = expense.amount,
                        date = expense.date,
                        actorName = actor
                    )
                )
            }

            settlementRepository.getByGroup(group.id).forEach { settlement ->
                val involvesMe = uid == settlement.fromUserId ||
                    uid == settlement.toUserId ||
                    uid == settlement.creatorId
                if (!involvesMe) return@forEach

                val from = nameOf(settlement.fromUserId)
                val to = nameOf(settlement.toUserId)
                val actor = nameOf(settlement.creatorId)
                val youLabel = if (settlement.creatorId == uid) "You" else actor
                items.add(
                    ActivityItem(
                        id = settlement.id,
                        kind = ActivityKind.SETTLEMENT,
                        groupId = group.id,
                        groupName = group.name,
                        title = "Settlement",
                        subtitle = "$youLabel recorded · $from paid $to",
                        amount = settlement.amount,
                        date = settlement.date,
                        actorName = actor
                    )
                )
            }
        }

        return items.sortedWith(
            compareByDescending<ActivityItem> { it.date }
                .thenByDescending { it.id }
        )
    }

    companion object {
        @Volatile
        private var instance: ActivityManager? = null

        fun getInstance(): ActivityManager {
            return instance ?: synchronized(this) {
                instance ?: ActivityManager().also { instance = it }
            }
        }
    }
}
