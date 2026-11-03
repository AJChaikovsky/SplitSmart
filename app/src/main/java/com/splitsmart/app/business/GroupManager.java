package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.isBlank;
import static com.splitsmart.app.business.Preconditions.require;
import static com.splitsmart.app.business.Preconditions.requireFound;
import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.ExpenseRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class GroupManager {
    private static volatile GroupManager instance;

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final AccountManager accountManager;
    private final RequestManager requestManager;

    public GroupManager() {
        this(GroupRepository.getInstance(), UserRepository.getInstance(), ExpenseRepository.getInstance(),
                SettlementRepository.getInstance(), AccountManager.getInstance(), RequestManager.getInstance());
    }

    public GroupManager(GroupRepository groupRepository, UserRepository userRepository,
                        ExpenseRepository expenseRepository, SettlementRepository settlementRepository,
                        AccountManager accountManager, RequestManager requestManager) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.accountManager = accountManager;
        this.requestManager = requestManager;
    }

    public static GroupManager getInstance() {
        if (instance == null) {
            synchronized (GroupManager.class) {
                if (instance == null) instance = new GroupManager();
            }
        }
        return instance;
    }

    /**
     * Creates a group with the current user as sole member, then sends invites
     * to any selected friends (they join after accepting).
     */
    public Group createGroup(String name, String description, List<String> additionalMemberIds) {
        require(!isBlank(name), "Group name is required");
        User current = requireState(accountManager.getCurrentUser(), "Not logged in");

        Group group = groupRepository.save(new Group(
                "",
                name.trim(),
                description.trim(),
                current.getUid(),
                Collections.singletonList(current.getEmail()),
                Collections.singletonList(current.getUid()),
                System.currentTimeMillis()
        ));

        for (String uid : new LinkedHashSet<>(additionalMemberIds)) {
            if (uid.equals(current.getUid())) continue;
            try {
                requestManager.sendGroupInvite(group.getId(), uid);
            } catch (RuntimeException ignored) {
                // Group is created even if one invite cannot be sent (e.g. already pending).
            }
        }
        return group;
    }

    public void inviteMemberByUid(String groupId, String memberUid) {
        requestManager.sendGroupInvite(groupId, memberUid);
    }

    public void inviteMemberByEmail(String groupId, String email) {
        requestManager.sendGroupInviteByEmail(groupId, email);
    }

    /** @deprecated Prefer invite flows; kept for accept-side joins only via RequestManager. */
    @Deprecated
    public Group addMemberByUid(String groupId, String memberUid) {
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getOwnerId().equals(currentUid), "Only the group owner can add members");
        require(!group.getMemberIds().contains(memberUid), "User is already a member");

        User user = requireFound(userRepository.getById(memberUid), "User not found");

        List<String> emails = new ArrayList<>(group.getMemberEmails());
        emails.add(user.getEmail());
        List<String> ids = new ArrayList<>(group.getMemberIds());
        ids.add(user.getUid());
        group.setMemberEmails(emails);
        group.setMemberIds(ids);
        return groupRepository.save(group);
    }

    public Group addMemberByEmail(String groupId, String email) {
        inviteMemberByEmail(groupId, email);
        return getGroup(groupId);
    }

    public Group editGroup(String groupId, String name, String description) {
        require(!isBlank(name), "Group name is required");
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getOwnerId().equals(currentUid), "Only the group owner can edit the group");

        group.setName(name.trim());
        group.setDescription(description.trim());
        return groupRepository.save(group);
    }

    public void deleteGroup(String groupId) {
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getOwnerId().equals(currentUid), "Only the group owner can delete the group");

        expenseRepository.deleteByGroup(groupId);
        settlementRepository.deleteByGroup(groupId);
        groupRepository.delete(groupId);
    }

    public Group removeMember(String groupId, String memberUid) {
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getOwnerId().equals(currentUid), "Only the group owner can remove members");
        require(!memberUid.equals(group.getOwnerId()), "Cannot remove the group owner");
        require(group.getMemberIds().contains(memberUid), "User is not a member of this group");

        User member = userRepository.getById(memberUid);
        removeFromGroup(group, memberUid, member);
        return groupRepository.save(group);
    }

    public void leaveGroup(String groupId) {
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(group.getMemberIds().contains(currentUid), "You are not a member of this group");
        require(!group.getOwnerId().equals(currentUid),
                "Owners cannot leave. Edit the group and delete it, or transfer ownership first.");

        removeFromGroup(group, currentUid, accountManager.getCurrentUser());
        groupRepository.save(group);
    }

    private static void removeFromGroup(Group group, String uid, User user) {
        List<String> ids = new ArrayList<>(group.getMemberIds());
        ids.remove(uid);
        List<String> emails = new ArrayList<>(group.getMemberEmails());
        if (user != null) emails.removeIf(email -> email.equals(user.getEmail()));
        group.setMemberIds(ids);
        group.setMemberEmails(emails);
    }

    public List<Group> listGroupsForCurrentUser() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        return groupRepository.getGroupsForUser(uid);
    }

    public Group getGroup(String groupId) {
        return requireFound(groupRepository.getById(groupId), "Group not found");
    }
}
