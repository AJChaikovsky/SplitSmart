package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.isBlank;
import static com.splitsmart.app.business.Preconditions.require;
import static com.splitsmart.app.business.Preconditions.requireFound;
import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.FriendRepository;
import com.splitsmart.app.data.GroupRepository;
import com.splitsmart.app.data.RequestRepository;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.Friendship;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.IncomingRequest;
import com.splitsmart.app.model.RequestStatus;
import com.splitsmart.app.model.RequestType;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RequestManager {
    private static volatile RequestManager instance;

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final GroupRepository groupRepository;
    private final AccountManager accountManager;

    public RequestManager() {
        this(RequestRepository.getInstance(), UserRepository.getInstance(), FriendRepository.getInstance(),
                GroupRepository.getInstance(), AccountManager.getInstance());
    }

    public RequestManager(RequestRepository requestRepository, UserRepository userRepository,
                          FriendRepository friendRepository, GroupRepository groupRepository,
                          AccountManager accountManager) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.groupRepository = groupRepository;
        this.accountManager = accountManager;
    }

    public static RequestManager getInstance() {
        if (instance == null) {
            synchronized (RequestManager.class) {
                if (instance == null) instance = new RequestManager();
            }
        }
        return instance;
    }

    public AppRequest sendFriendRequestByEmail(String email) {
        User current = requireState(accountManager.getCurrentUser(), "Not logged in");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        require(normalized.endsWith("@usc.edu"), "Friend email must be a @usc.edu address");
        require(!normalized.equals(current.getEmail()), "You cannot add yourself as a friend");

        User other = requireFound(userRepository.getByEmail(normalized), "No registered user with that USC email");

        boolean alreadyFriends = false;
        for (Friendship friendship : friendRepository.listForUser(current.getUid())) {
            if (friendship.getUserIds().contains(other.getUid())) alreadyFriends = true;
        }
        require(!alreadyFriends, "You're already friends with " + other.getName());

        assertNoPending(current.getUid(), other.getUid(), RequestType.FRIEND, "");

        AppRequest request = new AppRequest();
        request.setType(RequestType.FRIEND.name());
        request.setStatus(RequestStatus.PENDING.name());
        request.setFromUserId(current.getUid());
        request.setToUserId(other.getUid());
        request.setCreatedAt(System.currentTimeMillis());
        return requestRepository.save(request);
    }

    public AppRequest sendGroupInvite(String groupId, String toUserId) {
        User current = requireState(accountManager.getCurrentUser(), "Not logged in");
        Group group = requireFound(groupRepository.getById(groupId), "Group not found");
        require(group.getOwnerId().equals(current.getUid()), "Only the group owner can invite members");
        require(!toUserId.equals(current.getUid()), "You are already in this group");
        require(!group.getMemberIds().contains(toUserId), "User is already a member");

        requireFound(userRepository.getById(toUserId), "User not found");

        assertNoPending(current.getUid(), toUserId, RequestType.GROUP_INVITE, groupId);

        AppRequest request = new AppRequest();
        request.setType(RequestType.GROUP_INVITE.name());
        request.setStatus(RequestStatus.PENDING.name());
        request.setFromUserId(current.getUid());
        request.setToUserId(toUserId);
        request.setGroupId(group.getId());
        request.setGroupName(group.getName());
        request.setCreatedAt(System.currentTimeMillis());
        return requestRepository.save(request);
    }

    public AppRequest sendGroupInviteByEmail(String groupId, String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        require(normalized.endsWith("@usc.edu"), "Member email must be a @usc.edu address");
        User user = requireFound(userRepository.getByEmail(normalized), "No registered user with that USC email");
        return sendGroupInvite(groupId, user.getUid());
    }

    public List<IncomingRequest> listIncomingPending() {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        List<IncomingRequest> result = new ArrayList<>();
        for (AppRequest request : requestRepository.listIncoming(uid)) {
            if (request.statusEnum() != RequestStatus.PENDING) continue;
            User from = userRepository.getById(request.getFromUserId());
            result.add(new IncomingRequest(
                    request,
                    from != null ? from.getName() : SummaryManager.shortId(request.getFromUserId()),
                    from != null ? from.getEmail() : ""
            ));
        }
        return result;
    }

    public void accept(String requestId) {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        User current = requireState(accountManager.getCurrentUser(), "Not logged in");
        AppRequest request = requireFound(requestRepository.getById(requestId), "Request not found");
        require(request.getToUserId().equals(uid), "Only the recipient can accept this request");
        require(request.statusEnum() == RequestStatus.PENDING, "Request is no longer pending");

        if (request.typeEnum() == RequestType.FRIEND) {
            List<String> userIds = new ArrayList<>(Arrays.asList(request.getFromUserId(), request.getToUserId()));
            Collections.sort(userIds);
            friendRepository.save(new Friendship(
                    Friendship.docIdFor(request.getFromUserId(), request.getToUserId()),
                    userIds,
                    uid,
                    System.currentTimeMillis()
            ));
        } else {
            require(!isBlank(request.getGroupId()), "Group invite is missing a group");
            // Invitee may not be able to read the group yet; array-union self-join.
            groupRepository.addMemberFields(request.getGroupId(), uid, current.getEmail());
        }

        requestRepository.updateStatus(requestId, RequestStatus.ACCEPTED);
    }

    public void deny(String requestId) {
        String uid = requireState(accountManager.currentUid(), "Not logged in");
        AppRequest request = requireFound(requestRepository.getById(requestId), "Request not found");
        require(request.getToUserId().equals(uid), "Only the recipient can deny this request");
        require(request.statusEnum() == RequestStatus.PENDING, "Request is no longer pending");
        requestRepository.updateStatus(requestId, RequestStatus.DENIED);
    }

    private void assertNoPending(String fromUserId, String toUserId, RequestType type, String groupId) {
        boolean outgoingPending = false;
        for (AppRequest request : requestRepository.listOutgoing(fromUserId)) {
            if (matches(request, fromUserId, toUserId, type, groupId)) outgoingPending = true;
        }
        require(!outgoingPending, type == RequestType.FRIEND
                ? "You already have a pending friend request to this person"
                : "You already have a pending invite to this person for this group");

        // Also block the reverse friend request while theirs is still pending.
        if (type == RequestType.FRIEND) {
            boolean incomingPending = false;
            for (AppRequest request : requestRepository.listIncoming(fromUserId)) {
                if (matches(request, toUserId, fromUserId, type, groupId)) incomingPending = true;
            }
            require(!incomingPending, "This person already sent you a friend request — check Requests");
        }
    }

    private static boolean matches(AppRequest request, String from, String to, RequestType type, String groupId) {
        return request.statusEnum() == RequestStatus.PENDING
                && request.getFromUserId().equals(from)
                && request.getToUserId().equals(to)
                && request.typeEnum() == type
                && (type != RequestType.GROUP_INVITE || request.getGroupId().equals(groupId));
    }
}
