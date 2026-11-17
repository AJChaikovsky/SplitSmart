package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.VerificationModes;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.ui.CreateGroupActivity;
import com.splitsmart.app.ui.GroupDetailActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box test for creating a group (owner: AJ, BB-09).
 * Creates one real group in Firestore and deletes it afterwards.
 */
@RunWith(AndroidJUnit4.class)
public class GroupTest extends EspressoTestBase {
    private final String groupName = "Espresso Study " + System.currentTimeMillis();

    @Before
    public void signIn() {
        TestAccount.ensureSignedIn();
    }

    @After
    public void deleteCreatedGroup() {
        if (!TestAccount.signedIn()) return;
        GroupManager groups = GroupManager.getInstance();
        for (Group group : groups.listGroupsForCurrentUser()) {
            if (group.getName().equals(groupName)) groups.deleteGroup(group.getId());
        }
    }

    private void submit(String name) {
        onView(withId(R.id.etName)).perform(scrollTo(), replaceText(name), closeSoftKeyboard());
        onView(withId(R.id.btnSave)).perform(scrollTo(), click());
    }

    /**
     * BB-09: a blank group name is rejected; a valid name creates the group, opens it, and makes
     * the creator its owner.
     * Rationale: one invalid and one valid equivalence class for the only required field. The
     * name includes a timestamp so repeated runs never collide and cleanup finds only this group.
     */
    @Test
    public void blankNameRejectedValidNameCreatesOwnedGroup() {
        try (ActivityScenario<CreateGroupActivity> ignored = ActivityScenario.launch(CreateGroupActivity.class)) {
            submit("");
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            intended(hasComponent(GroupDetailActivity.class.getName()), VerificationModes.times(0));

            submit(groupName);
            intended(hasComponent(GroupDetailActivity.class.getName()));
        }

        Group created = null;
        for (Group group : GroupManager.getInstance().listGroupsForCurrentUser()) {
            if (group.getName().equals(groupName)) created = group;
        }
        assertEquals("Group was saved", groupName, created == null ? null : created.getName());
        assertEquals("Creator is the owner", AccountManager.getInstance().currentUid(), created.getOwnerId());
    }

    /**
     * BB-31: a group name typed on Create Group survives a screen rotation.
     * Rationale: rotation recreates the screen; state preservation was a fixed bug. recreate()
     * goes through the same save/restore path as a real rotation. Nothing is saved to Firebase.
     */
    @Test
    public void typedNameSurvivesRotation() {
        try (ActivityScenario<CreateGroupActivity> scenario = ActivityScenario.launch(CreateGroupActivity.class)) {
            onView(withId(R.id.etName)).perform(scrollTo(), replaceText("Rotation Check"), closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.etName)).check(matches(withText("Rotation Check")));
        }
    }
}
