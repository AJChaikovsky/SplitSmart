package com.splitsmart.app.business

import com.splitsmart.app.data.ExpenseRepository
import com.splitsmart.app.data.GroupRepository
import com.splitsmart.app.data.SettlementRepository
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.Group

class GroupManager(
    private val groupRepository: GroupRepository = GroupRepository.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository.getInstance(),
    private val settlementRepository: SettlementRepository = SettlementRepository.getInstance(),
    private val accountManager: AccountManager = AccountManager.getInstance()
) {
    suspend fun createGroup(name: String, description: String): Group {
        require(name.isNotBlank()) { "Group name is required" }
        val current = accountManager.getCurrentUser()
            ?: throw IllegalStateException("Not logged in")

        val group = Group(
            name = name.trim(),
            description = description.trim(),
            ownerId = current.uid,
            memberEmails = listOf(current.email),
            memberIds = listOf(current.uid),
            createdAt = System.currentTimeMillis()
        )
        return groupRepository.save(group)
    }

    suspend fun addMemberByEmail(groupId: String, email: String): Group {
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(group.ownerId == currentUid) { "Only the group owner can add members" }

        val normalized = email.trim().lowercase()
        require(normalized.endsWith("@usc.edu")) { "Member email must be a @usc.edu address" }

        val user = userRepository.getByEmail(normalized)
            ?: throw IllegalArgumentException("No registered user with that USC email")

        require(!group.memberIds.contains(user.uid)) { "User is already a member" }

        val updated = group.copy(
            memberEmails = group.memberEmails + user.email,
            memberIds = group.memberIds + user.uid
        )
        return groupRepository.save(updated)
    }

    suspend fun editGroup(groupId: String, name: String, description: String): Group {
        require(name.isNotBlank()) { "Group name is required" }
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(group.ownerId == currentUid) { "Only the group owner can edit the group" }

        return groupRepository.save(
            group.copy(name = name.trim(), description = description.trim())
        )
    }

    suspend fun deleteGroup(groupId: String) {
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(group.ownerId == currentUid) { "Only the group owner can delete the group" }

        expenseRepository.deleteByGroup(groupId)
        settlementRepository.deleteByGroup(groupId)
        groupRepository.delete(groupId)
    }

    /**
     * Owner removes another member from the group. Cannot remove the owner.
     */
    suspend fun removeMember(groupId: String, memberUid: String): Group {
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(group.ownerId == currentUid) { "Only the group owner can remove members" }
        require(memberUid != group.ownerId) { "Cannot remove the group owner" }
        require(memberUid in group.memberIds) { "User is not a member of this group" }

        val member = userRepository.getById(memberUid)
        val updated = group.copy(
            memberIds = group.memberIds.filter { it != memberUid },
            memberEmails = group.memberEmails.filter { it != member?.email }
        )
        return groupRepository.save(updated)
    }

    /**
     * Non-owner leaves the group. Owners must delete the group instead.
     */
    suspend fun leaveGroup(groupId: String) {
        val group = groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
        val currentUid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        require(currentUid in group.memberIds) { "You are not a member of this group" }
        require(group.ownerId != currentUid) {
            "Owners cannot leave. Edit the group and delete it, or transfer ownership first."
        }

        val current = accountManager.getCurrentUser()
        val updated = group.copy(
            memberIds = group.memberIds.filter { it != currentUid },
            memberEmails = group.memberEmails.filter { it != current?.email }
        )
        groupRepository.save(updated)
    }

    suspend fun listGroupsForCurrentUser(): List<Group> {
        val uid = accountManager.currentUid()
            ?: throw IllegalStateException("Not logged in")
        return groupRepository.getGroupsForUser(uid)
    }

    suspend fun getGroup(groupId: String): Group {
        return groupRepository.getById(groupId)
            ?: throw IllegalArgumentException("Group not found")
    }

    companion object {
        @Volatile
        private var instance: GroupManager? = null

        fun getInstance(): GroupManager {
            return instance ?: synchronized(this) {
                instance ?: GroupManager().also { instance = it }
            }
        }
    }
}
