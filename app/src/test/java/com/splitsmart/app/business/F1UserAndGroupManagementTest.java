package com.splitsmart.app.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.splitsmart.app.data.FirestoreManager;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Demo checklist, Feature 1: user and group management. */
public class F1UserAndGroupManagementTest {
    private final TestBackend app = new TestBackend();

    static void assertRejected(String expectedMessagePart, Runnable action) {
        try {
            action.run();
            fail("Expected rejection containing \"" + expectedMessagePart + "\"");
        } catch (IllegalArgumentException | IllegalStateException e) {
            assertTrue("Message was: " + e.getMessage(), e.getMessage().contains(expectedMessagePart));
        }
    }

    // --- Register and validate input ---

    /**
     * WB-01: validateRegistration rejects an email outside @usc.edu.
     * Rationale: covers the true branch of the email-domain check. Name is valid so the earlier
     * blank-name branch is skipped, and the exact error message proves this branch returned.
     */
    @Test
    public void registrationRejectsNonUscEmail() {
        AccountManager.ValidationResult result =
                app.accounts.validateRegistration("Eve", "eve@gmail.com", "password1", "1234567890");
        assertFalse(result.isOk());
        assertEquals("Email must be a @usc.edu address", result.getError());
    }

    /**
     * WB-02: validateRegistration rejects 9- and 11-digit USC IDs.
     * Rationale: the ID regex ^\d{10}$ is checked on both sides of its length boundary, so a
     * regex that only bounds one side (e.g. \d{10,}) would fail this test.
     */
    @Test
    public void registrationRejectsUscIdOfWrongLength() {
        assertFalse(app.accounts.validateRegistration("Eve", "eve@usc.edu", "password1", "123456789").isOk());
        assertFalse(app.accounts.validateRegistration("Eve", "eve@usc.edu", "password1", "12345678901").isOk());
    }

    /**
     * WB-03: validateRegistration rejects a 10-character ID containing a letter.
     * Rationale: the length is right, so only the digits-only part of the regex can fail. Guards
     * the USC ID keyboard bug fix (the field now accepts any text and relies on this check).
     */
    @Test
    public void registrationRejectsTenCharacterIdWithNonDigit() {
        AccountManager.ValidationResult result =
                app.accounts.validateRegistration("Eve", "eve@usc.edu", "password1", "12345678A9");
        assertFalse(result.isOk());
        assertEquals("USC ID must be exactly 10 digits", result.getError());
    }

    @Test
    public void registrationSavesProfileAndSignsIn() {
        User eve = app.accounts.register("Eve", "Eve@USC.edu", "password1", "1112223334");
        assertEquals("eve@usc.edu", eve.getEmail());
        assertEquals(eve.getUid(), app.accounts.currentUid());
        User saved = app.users.getById(eve.getUid());
        assertEquals("Eve", saved.getName());
        assertEquals("1112223334", saved.getUscId());
    }

    // --- Reject duplicate identities ---

    /**
     * WB-04: register maps Firebase's "email already in use" error to a clear message.
     * Rationale: covers the first branch of the catch block around authManager.register. The fake
     * auth throws Firebase's real error text, so the string matching in register is exercised.
     */
    @Test
    public void registrationRejectsExistingEmailWithDifferentId() {
        app.user("Alice", "1000000001");
        assertRejected("An account with this email already exists",
                () -> app.accounts.register("Alice Two", "alice@usc.edu", "password1", "2000000002"));
    }

    /**
     * WB-05: register rejects a USC ID that is already taken, and rolls back the new login.
     * Rationale: the new email passes Firebase, so execution reaches the duplicate-ID check and
     * then the catch block that deletes the orphan Auth account. Asserts both the error and the
     * rollback, and that no profile was saved.
     */
    @Test
    public void registrationRejectsExistingUscIdWithDifferentEmail() {
        app.user("Alice", "1000000001");
        assertRejected("An account with this USC ID already exists",
                () -> app.accounts.register("Eve", "eve@usc.edu", "password1", "1000000001"));
        assertTrue("Orphan login is rolled back", app.auth.deletedCurrentUser);
        assertNull(app.users.getById("uid-eve@usc.edu"));
    }

    // --- Manage the account ---

    @Test
    public void loginUpdateNameAndLogout() {
        User alice = app.user("Alice", "1000000001");
        User loggedIn = app.accounts.login("alice@usc.edu", "password1");
        assertEquals(alice.getUid(), loggedIn.getUid());

        User renamed = app.accounts.updateName("Alicia");
        assertEquals("Alicia", renamed.getName());
        assertEquals("Email cannot change", "alice@usc.edu", renamed.getEmail());
        assertEquals("USC ID cannot change", "1000000001", renamed.getUscId());

        app.accounts.logout();
        assertNull(app.accounts.currentUid());
    }

    @Test
    public void loginRejectsWrongPassword() {
        app.user("Alice", "1000000001");
        assertRejected("Incorrect email or password", () -> app.accounts.login("alice@usc.edu", "wrong"));
    }

    // --- Create a group ---

    /**
     * WB-06: createGroup makes the creator the owner and only member, and saves the group.
     * Rationale: reads the group back from the repository instead of trusting the returned object,
     * so a missing save would fail. Checks every field createGroup sets.
     */
    @Test
    public void creatorBecomesOwnerAndGroupIsSaved() {
        User alice = app.user("Alice", "1000000001");
        app.signIn(alice);
        Group created = app.groupManager.createGroup("Study Group", "CSCI 310", Collections.emptyList());

        Group saved = app.groupManager.getGroup(created.getId());
        assertEquals(alice.getUid(), saved.getOwnerId());
        assertEquals(Collections.singletonList(alice.getUid()), saved.getMemberIds());
        assertEquals("CSCI 310", saved.getDescription());
        assertEquals(1, app.groupManager.listGroupsForCurrentUser().size());
    }

    // --- Add and view members ---

    @Test
    public void inviteRejectsUnregisteredEmail() {
        User alice = app.user("Alice", "1000000001");
        Group group = app.group("Study Group", alice);
        app.signIn(alice);
        assertRejected("No registered user with that USC email",
                () -> app.requestManager.sendGroupInviteByEmail(group.getId(), "nobody@usc.edu"));
    }

    @Test
    public void inviteRejectsExistingMember() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        Group group = app.group("Study Group", alice, bob);
        app.signIn(alice);
        assertRejected("User is already a member",
                () -> app.requestManager.sendGroupInviteByEmail(group.getId(), bob.getEmail()));
    }

    /**
     * WB-07: a member who isn't the owner can't invite someone to the group.
     * Rationale: Carol is a registered USC user, so the email checks pass and only the owner check
     * in sendGroupInvite can reject the request.
     */
    @Test
    public void nonOwnerCannotAddMembers() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        User carol = app.user("Carol", "1000000003");
        Group group = app.group("Study Group", alice, bob);
        app.signIn(bob);
        assertRejected("Only the group owner can invite members",
                () -> app.requestManager.sendGroupInviteByEmail(group.getId(), carol.getEmail()));
    }

    @Test
    public void userCanBelongToSeveralGroupsAndMembersSeeMemberList() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        Group first = app.group("Roommates", alice, bob);
        app.group("Trip", bob, alice);

        app.signIn(bob);
        assertEquals(2, app.groupManager.listGroupsForCurrentUser().size());
        Group seenByBob = app.groupManager.getGroup(first.getId());
        assertTrue(seenByBob.getMemberIds().containsAll(Arrays.asList(alice.getUid(), bob.getUid())));
    }

    // --- Group permissions and deletion ---

    @Test
    public void ownerRenamesGroupButMembersCannotEditOrDelete() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        Group group = app.group("Study Group", alice, bob);

        app.signIn(alice);
        assertEquals("Study Group 310", app.groupManager.editGroup(group.getId(), "Study Group 310", "").getName());

        app.signIn(bob);
        assertRejected("Only the group owner can edit the group",
                () -> app.groupManager.editGroup(group.getId(), "Hacked", ""));
        assertRejected("Only the group owner can delete the group",
                () -> app.groupManager.deleteGroup(group.getId()));
        assertNotNull(app.groups.getById(group.getId()));
    }

    /**
     * WB-08: deleteGroup removes the group with its expenses and settlements, and nothing else.
     * Rationale: covers all three delete calls in deleteGroup. A second group with its own expense
     * proves the cascade is limited to the deleted group's ID.
     */
    @Test
    public void deletingGroupRemovesItsExpensesAndSettlementsOnly() {
        User alice = app.user("Alice", "1000000001");
        User bob = app.user("Bob", "1000000002");
        Group doomed = app.group("Delete Me", alice, bob);
        Group kept = app.group("Keep", alice, bob);
        app.equalExpense(doomed, alice, "Pizza", 12.0, alice, alice, bob);
        app.signIn(bob);
        app.settlementManager.createSettlement(doomed.getId(), bob.getUid(), alice.getUid(), 6.0, TestBackend.TODAY, "");
        app.equalExpense(kept, alice, "Coffee", 8.0, alice, alice, bob);

        app.signIn(alice);
        app.groupManager.deleteGroup(doomed.getId());

        assertNull(app.groups.getById(doomed.getId()));
        assertTrue(app.expenses.getByGroup(doomed.getId()).isEmpty());
        assertTrue(app.settlements.getByGroup(doomed.getId()).isEmpty());
        assertEquals(1, app.expenses.getByGroup(kept.getId()).size());
        assertEquals(1, app.db.count(FirestoreManager.COLLECTION_EXPENSES));
    }
}
