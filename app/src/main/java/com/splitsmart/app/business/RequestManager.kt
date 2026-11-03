package com.splitsmart.app.business

import com.splitsmart.app.data.FriendRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.RequestRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.AppRequest
import com.splitsmart.app.model.Friendship
import com.splitsmart.app.model.IncomingRequest
import com.splitsmart.app.model.RequestStatus
import com.splitsmart.app.model.RequestType

class RequestManager(
    private val requestRepository: RequestRepository = RequestRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val friendRepository: FriendRepository = FriendRepository.getInstance(),
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance()
) {
    suspend fun sendFriendRequestByEmail(email: String): AppRequest {
        val current = accountManager.getCurrentUser()
            ?: throw IllegalStateException("Not logged in")
        val normalized = email.trim().lowercase()
        require(normalized.endsWith("@usc.edu")) { "Friend email must be a @usc.edu address" }
        require(normalized != current.email) { "You cannot add yourself as a friend" }

        val other = userRepository.getByEmail(normalized)
            ?: throw IllegalArgumentException("No registered user with that USC email")

        val alreadyFriends = friendRepository.listForUser(current.uid)
            .any { other.uid in it.userIds }
        require(!alreadyFriends) { "You're already friends with ${other.name}" }

        assertNoPending(
            fromUserId = current.uid,
            toUserId = other.uid,
            type = RequestType.FRIEND
        )

        return requestRepository.save(
            AppRequest(
                type = RequestType.FRIEND.name,
                status = RequestStatus.PENDING.name,
                fromUserId = current.uid,
                toUserId = other.uid,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendGroupInvite(groupId: String, toUserId: String): AppRequest {
        val current = accountManager.getCurrentUser()
            ?: throw IllegalStateException("Not logged in")
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        require(group.ownerId == current.uid) { "Only the group owner can invite members" }
        require(toUserId != current.uid) { "You are already in this group" }
        require(toUserId !in group.memberIds) { "User is already a member" }

        val other = userRepository.getById(toUserId)
            ?: throw IllegalArgumentException("User not found")

        assertNoPending(
            fromUserId = current.uid,
            toUserId = toUserId,
            type = RequestType.GROUP_INVITE,
            groupId = groupId
        )

        return requestRepository.save(
            AppRequest(
                type = RequestType.GROUP_INVITE.name,
                status = RequestStatus.PENDING.name,
                fromUserId = current.uid,
                toUserId = toUserId,
                groupId = group.id,
                groupName = group.name,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendGroupInviteByEmail(groupId: String, email: String): AppRequest {
        val normalized = email.trim().lowercase()
        require(normalized.endsWith("@usc.edu")) { "Member email must be a @usc.edu address" }
        val user = userRepository.getByEmail(normalized)
            ?: throw IllegalArgumentException("No registered user with that USC email")
        return sendGroupInvite(groupId, user.uid)
    }

    suspend fun listIncomingPending(): List<IncomingRequest> {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        return requestRepository.listIncoming(uid)
            .filter { it.statusEnum() == RequestStatus.PENDING }
            .map { req ->
                val from = userRepository.getById(req.fromUserId)
                IncomingRequest(
                    request = req,
                    fromName = from?.name ?: req.fromUserId.take(8),
                    fromEmail = from?.email.orEmpty()
                )
            }
    }

    suspend fun accept(requestId: String) {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        val current = accountManager.getCurrentUser()
            ?: throw IllegalStateException("Not logged in")
        val request = requestRepository.getById(requestId)
            ?: throw IllegalArgumentException("Request not found")
        require(request.toUserId == uid) { "Only the recipient can accept this request" }
        require(request.statusEnum() == RequestStatus.PENDING) { "Request is no longer pending" }

        when (request.typeEnum()) {
            RequestType.FRIEND -> {
                val id = Friendship.docIdFor(request.fromUserId, request.toUserId)
                friendRepository.save(
                    Friendship(
                        id = id,
                        userIds = listOf(request.fromUserId, request.toUserId).sorted(),
                        createdBy = uid,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            RequestType.GROUP_INVITE -> {
                require(request.groupId.isNotBlank()) { "Group invite is missing a group" }
                // Invitee may not be able to read the group yet; array-union self-join.
                groupRepository.addMemberFields(request.groupId, uid, current.email)
            }
        }

        requestRepository.updateStatus(requestId, RequestStatus.ACCEPTED)
    }

    suspend fun deny(requestId: String) {
        val uid = accountManager.currentUid() ?: throw IllegalStateException("Not logged in")
        val request = requestRepository.getById(requestId)
            ?: throw IllegalArgumentException("Request not found")
        require(request.toUserId == uid) { "Only the recipient can deny this request" }
        require(request.statusEnum() == RequestStatus.PENDING) { "Request is no longer pending" }
        requestRepository.updateStatus(requestId, RequestStatus.DENIED)
    }

    private suspend fun assertNoPending(
        fromUserId: String,
        toUserId: String,
        type: RequestType,
        groupId: String = ""
    ) {
        val pending = requestRepository.listOutgoing(fromUserId).any { req ->
            req.statusEnum() == RequestStatus.PENDING &&
                req.toUserId == toUserId &&
                req.typeEnum() == type &&
                (type != RequestType.GROUP_INVITE || req.groupId == groupId)
        }
        require(!pending) {
            when (type) {
                RequestType.FRIEND ->
                    "You already have a pending friend request to this person"
                RequestType.GROUP_INVITE ->
                    "You already have a pending invite to this person for this group"
            }
        }
    }

    companion object {
        @Volatile
        private var instance: RequestManager? = null

        fun getInstance(): RequestManager {
            return instance ?: synchronized(this) {
                instance ?: RequestManager().also { instance = it }
            }
        }
    }
}
