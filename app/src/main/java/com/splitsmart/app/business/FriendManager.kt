package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.FriendRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.FriendBalance
import com.splitsmart.app.model.Friendship
import com.splitsmart.app.model.User

class FriendManager(
    private val friendRepository: FriendRepository = FriendRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance(),
    private val balanceManager: BalanceManager = BalanceManager.getInstance()
) {
    suspend fun addFriendByEmail(email: String): Friendship {
        val current = accountManager.getCurrentUser()
            ?: throw IllegalStateException("Not logged in")
        val normalized = email.trim().lowercase()
        require(normalized.endsWith("@usc.edu")) { "Friend email must be a @usc.edu address" }
        require(normalized != current.email) { "You cannot add yourself as a friend" }

        val other = userRepository.getByEmail(normalized)
            ?: throw IllegalArgumentException("No registered user with that USC email")

        val id = Friendship.docIdFor(current.uid, other.uid)
        // Use list query (not get-by-id) so missing docs don't hit get-on-null rules.
        val alreadyFriends = friendRepository.listForUser(current.uid)
            .any { friendship -> other.uid in friendship.userIds }
        require(!alreadyFriends) { "You're already friends with ${other.name}" }

        return friendRepository.save(
            Friendship(
                id = id,
                userIds = listOf(current.uid, other.uid).sorted(),
                createdBy = current.uid,
                createdAt = System.currentTimeMillis()
            )
        )
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
                        // Friend owes me
                        netByFriend[pair.fromUserId] =
                            (netByFriend[pair.fromUserId] ?: 0L) + MoneyUtils.toCents(pair.amount)
                    }
                    pair.fromUserId == uid -> {
                        // I owe friend
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
        friendRepository.delete(Friendship.docIdFor(uid, friendUid))
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
