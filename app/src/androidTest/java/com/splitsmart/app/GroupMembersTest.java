package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.data.RequestRepository;
import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.ui.GroupDetailActivity;
import com.splitsmart.app.ui.HomeActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/**
 * Black-box test for inviting group members (BB-27).
 * Creates one real group owned by the test account and deletes it afterwards.
 */
@RunWith(AndroidJUnit4.class)
public class GroupMembersTest extends EspressoTestBase {
    private Group group;

    @Before
    public void createOwnedGroup() {
        TestAccount.ensureSignedIn();
        group = GroupManager.getInstance().createGroup(
                "Espresso Members " + System.currentTimeMillis(), "", Collections.emptyList());
    }

    @After
    public void deleteGroup() {
        if (group != null && TestAccount.signedIn()) GroupManager.getInstance().deleteGroup(group.getId());
    }

    private int invitesSentForGroup() {
        int count = 0;
        String uid = AccountManager.getInstance().currentUid();
        for (AppRequest request : RequestRepository.getInstance().listOutgoing(uid)) {
            if (group.getId().equals(request.getGroupId())) count++;
        }
        return count;
    }

    /**
     * BB-27: the owner can't invite a non-USC email.
     * Rationale: equivalence class "valid format, wrong domain" for the invite field. On success
     * the screen clears the field, so the email still being there shows the invite was refused.
     * The request list is also checked, since that's where a sent invite would appear.
     */
    @Test
    public void ownerCannotInviteNonUscEmail() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity.class)
                .putExtra(HomeActivity.EXTRA_GROUP_ID, group.getId());
        try (ActivityScenario<GroupDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(GroupDetailActivity::showMembers);

            onView(withId(R.id.etMemberEmail)).perform(replaceText("friend@gmail.com"), closeSoftKeyboard());
            onView(withId(R.id.btnAddMember)).perform(click());

            onView(withId(R.id.etMemberEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.etMemberEmail)).check(matches(withText("friend@gmail.com")));
        }
        assertEquals("No invite was sent", 0, invitesSentForGroup());
    }
}
