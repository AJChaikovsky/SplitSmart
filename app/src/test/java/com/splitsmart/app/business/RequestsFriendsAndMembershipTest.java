package com.splitsmart.app.business;

import static com.splitsmart.app.business.F1UserAndGroupManagementTest.assertRejected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.Friendship;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.RequestStatus;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.Arrays;

/**
 * Extra white-box tests for friend requests, group membership and profile rules (WB-26 to WB-32).
 * These cover RequestManager, GroupManager and AccountManager branches the demo-checklist tests
 * don't reach.
 */
public class RequestsFriendsAndMembershipTest {
    private final TestBackend app = new TestBackend();

    // --- Friend requests ---

    /**
     * WB-26: sendFriendRequestByEmail rejects each invalid request in turn.
     * Rationale: one case per guard in the method, in the order they run: non-USC email, your own
     * email, an unregistered email, an existing friend, a duplicate pending request, and a request
     * back to someone whose request to you is still pending.
     */
    @Test
    public void friendRequestRejectsEachInvalidCase() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        app.befriend(alice, bob);

        app.signIn(alice);
        assertRejected("Friend email must be a @usc.edu address",
                () -> app.requestManager.sendFriendRequestByEmail("carol@gmail.com"));
        assertRejected("You cannot add yourself as a friend",
                () -> app.requestManager.sendFriendRequestByEmail("ALICE@usc.edu"));
        assertRejected("No registered user with that USC email",
                () -> app.requestManager.sendFriendRequestByEmail("nobody@usc.edu"));
        assertRejected("You're already friends with Bob",
                () -> app.requestManager.sendFriendRequestByEmail(bob.getEmail()));

        app.requestManager.sendFriendRequestByEmail(carol.getEmail());
        assertRejected("You already have a pending friend request to this person",
                () -> app.requestManager.sendFriendRequestByEmail(carol.getEmail()));

        app.signIn(carol);
        assertRejected("This person already sent you a friend request",
                () -> app.requestManager.sendFriendRequestByEmail(alice.getEmail()));
    }

    /**
     * WB-27: accepting a friend request creates one friendship that both people see.
     * Rationale: covers the FRIEND branch of accept (the group-invite branch is already covered).
     * Reads the friendship back by its document ID and checks the request is marked accepted.
     */
    @Test
    public void acceptingFriendRequestCreatesFriendshipForBoth() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        app.signIn(alice);
        AppRequest request = app.requestManager.sendFriendRequestByEmail(bob.getEmail());

        app.signIn(bob);
        assertEquals(1, app.requestManager.listIncomingPending().size());
        app.requestManager.accept(request.getId());

        Friendship friendship = app.friends.getById(Friendship.docIdFor(alice.getUid(), bob.getUid()));
        assertNotNull(friendship);
        assertTrue(friendship.getUserIds().containsAll(Arrays.asList(alice.getUid(), bob.getUid())));
        assertEquals(RequestStatus.ACCEPTED, app.requests.getById(request.getId()).statusEnum());
        assertTrue("No longer pending for Bob", app.requestManager.listIncomingPending().isEmpty());

        assertEquals("Bob sees Alice", alice.getUid(), app.friendManager.listFriends().get(0).getUid());
        app.signIn(alice);
        assertEquals("Alice sees Bob", bob.getUid(), app.friendManager.listFriends().get(0).getUid());
    }

    /**
     * WB-28: only the recipient can answer a request, and only once.
     * Rationale: covers the recipient check in accept and deny, the DENIED update, and the
     * "no longer pending" check for both methods after the request is answered.
     */
    @Test
    public void onlyRecipientAnswersAndOnlyOnce() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        app.signIn(alice);
        AppRequest request = app.requestManager.sendFriendRequestByEmail(bob.getEmail());

        app.signIn(carol);
        assertRejected("Only the recipient can accept this request", () -> app.requestManager.accept(request.getId()));
        assertRejected("Only the recipient can deny this request", () -> app.requestManager.deny(request.getId()));

        app.signIn(bob);
        app.requestManager.deny(request.getId());
        assertEquals(RequestStatus.DENIED, app.requests.getById(request.getId()).statusEnum());
        assertRejected("Request is no longer pending", () -> app.requestManager.accept(request.getId()));
        assertRejected("Request is no longer pending", () -> app.requestManager.deny(request.getId()));
        assertTrue("Denying creates no friendship", app.friendManager.listFriends().isEmpty());
    }

    // --- Removing friends ---

    /**
     * WB-29: removeFriend ends the friendship for both people and rejects removing yourself.
     * Rationale: covers both branches of removeFriend. Checks both sides because the friendship is
     * a single shared document.
     */
    @Test
    public void removeFriendEndsFriendshipForBoth() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        app.befriend(alice, bob);

        app.signIn(alice);
        assertRejected("Invalid friend", () -> app.friendManager.removeFriend(alice.getUid()));
        app.friendManager.removeFriend(bob.getUid());
        assertTrue(app.friendManager.listFriends().isEmpty());
        app.signIn(bob);
        assertTrue(app.friendManager.listFriends().isEmpty());
    }

    // --- Group membership ---

    /**
     * WB-30: only the owner can remove members, the owner can't be removed, and a removed member
     * loses access to the group.
     * Rationale: one case per guard in removeMember (non-owner, owner as target, non-member), then
     * the success path, checking both the member ID list and the member email list.
     */
    @Test
    public void ownerRemovesMemberWithGuards() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        User dave = app.user("Dave", "1000000004");
        Group group = app.group("Roommates", alice, bob, carol);

        app.signIn(bob);
        assertRejected("Only the group owner can remove members",
                () -> app.groupManager.removeMember(group.getId(), carol.getUid()));

        app.signIn(alice);
        assertRejected("Cannot remove the group owner",
                () -> app.groupManager.removeMember(group.getId(), alice.getUid()));
        assertRejected("User is not a member of this group",
                () -> app.groupManager.removeMember(group.getId(), dave.getUid()));

        Group updated = app.groupManager.removeMember(group.getId(), bob.getUid());
        assertEquals(Arrays.asList(alice.getUid(), carol.getUid()), app.groups.getById(group.getId()).getMemberIds());
        assertFalse(updated.getMemberEmails().contains(bob.getEmail()));

        app.signIn(bob);
        assertTrue("Bob no longer sees the group", app.groupManager.listGroupsForCurrentUser().isEmpty());
    }

    /**
     * WB-31: a member can leave a group; the owner can't, and you can't leave twice.
     * Rationale: covers the owner check and the membership check in leaveGroup, plus the success
     * path that saves the group without the member.
     */
    @Test
    public void memberLeavesButOwnerCannot() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        Group group = app.group("Trip", alice, bob);

        app.signIn(alice);
        assertRejected("Owners cannot leave", () -> app.groupManager.leaveGroup(group.getId()));

        app.signIn(bob);
        app.groupManager.leaveGroup(group.getId());
        assertFalse(app.groups.getById(group.getId()).getMemberIds().contains(bob.getUid()));
        assertTrue(app.groupManager.listGroupsForCurrentUser().isEmpty());
        assertRejected("You are not a member of this group", () -> app.groupManager.leaveGroup(group.getId()));
    }

    // --- Registration and profile ---

    /**
     * WB-32: password length boundary, upper-case USC email, and blank name updates.
     * Rationale: 5 and 6 characters sit on either side of the minimum length check. The email
     * check lower-cases before comparing, so "EVE@USC.EDU" must pass. updateName must reject a
     * name of only spaces and leave the saved name unchanged.
     */
    @Test
    public void passwordBoundaryUpperCaseEmailAndBlankName() {
        AccountManager.ValidationResult tooShort =
                app.accounts.validateRegistration("Eve", "eve@usc.edu", "12345", "1234567890");
        assertFalse(tooShort.isOk());
        assertEquals("Password must be at least 6 characters", tooShort.getError());
        assertTrue(app.accounts.validateRegistration("Eve", "eve@usc.edu", "123456", "1234567890").isOk());
        assertTrue(app.accounts.validateRegistration("Eve", "EVE@USC.EDU", "123456", "1234567890").isOk());

        User alice = app.user("Alice", "1000000001");
        app.signIn(alice);
        assertRejected("Name cannot be empty", () -> app.accounts.updateName("   "));
        assertEquals("Alice", app.users.getById(alice.getUid()).getName());
    }
}
