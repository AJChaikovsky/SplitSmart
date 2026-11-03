package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.FriendRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.FriendBalance
import com.splitsmart.app.model.User

class FriendManager(
    private val friendRepository: FriendRepository = FriendRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance(),
    private val balanceManager: BalanceManager = BalanceManager.getInstance(),
    private val requestManager: RequestManager = RequestManager.getInstance()
) {
    /** Sends a friend request (recipient must accept on the Requests tab). */
    suspend fun addFriendByEmail(email: String) {
        requestManager.sendFriendRequestByEmail(email)
    }

    suspend fun listFriends(): List<User> {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        return friendRepository.listForUser(uid).mapNotNull { friendship ->
            val otherId = friendship.userIds.firstOrNull { it != uid } ?: return@mapNotNull null
            userRepository.getById(otherId)
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Friends with net balance across all shared groups.
     * Positive netAmount => friend owes you; negative => you owe them.
     */
    suspend fun listFriendsWithBalances(): List<FriendBalance> {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        val friends = listFriends()
        if (friends.isEmpty()) return emptyList()

        val myGroups = groupRepository.getGroupsForUser(uid)
        val netByFriend = mutableMapOf<String, Long>()

        myGroups.forEach { group ->
            val expenses = expenseRepository.getByGroup(group.id)
            val settlements = settlementRepository.getByGroup(group.id)
            val balances = balanceManager.computePairwiseBalances(expenses, settlements)
            balances.forEach { pair ->
                when {
                    pair.toUserId == uid -> {
                        netByFriend[pair.fromUserId] =
                            (netByFriend[pair.fromUserId] ?: 0L) + MoneyUtils.toCents(pair.amount)
                    }
                    pair.fromUserId == uid -> {
                        netByFriend[pair.toUserId] =
                            (netByFriend[pair.toUserId] ?: 0L) - MoneyUtils.toCents(pair.amount)
                    }
                }
            }
        }

        return friends.map { friend ->
            FriendBalance(
                friend = friend,
                netAmount = MoneyUtils.fromCents(netByFriend[friend.uid] ?: 0L)
            )
        }.sortedWith(
            compareByDescending<FriendBalance> { kotlin.math.abs(it.netAmount) }
                .thenBy { it.friend.name.lowercase() }
        )
    }

    suspend fun removeFriend(friendUid: String) {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        require(friendUid != uid) { "Invalid friend" }
        friendRepository.delete(
            com.splitsmart.app.model.Friendship.docIdFor(uid, friendUid)
        )
    }

    companion object {
        @Volatile
        private var instance: FriendManager? = null

        fun getInstance(): FriendManager {
            return instance ?: synchronized(this) {
                instance ?: FriendManager().also { instance = it }
            }
        }
    }
}
