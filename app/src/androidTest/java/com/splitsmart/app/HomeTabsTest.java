package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.widget.EditText;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.RequestManager;
import com.splitsmart.app.data.RequestRepository;
import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.RequestStatus;
import com.splitsmart.app.model.RequestType;
import com.splitsmart.app.ui.HomeActivity;
import com.splitsmart.app.ui.ThemePreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Black-box tests for the home screen's Friends and Requests tabs and theme button (BB-28 to BB-30). */
@RunWith(AndroidJUnit4.class)
public class HomeTabsTest extends EspressoTestBase {
    private int startNightMode;

    @Before
    public void signIn() {
        TestAccount.ensureSignedIn();
        startNightMode = AppCompatDelegate.getDefaultNightMode();
    }

    @After
    public void restoreTheme() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (AppCompatDelegate.getDefaultNightMode() != startNightMode) {
                ThemePreferences.setDark(ApplicationProvider.getApplicationContext(),
                        startNightMode == AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
    }

    private static int pendingFriendRequestsSent() {
        int count = 0;
        String uid = AccountManager.getInstance().currentUid();
        for (AppRequest request : RequestRepository.getInstance().listOutgoing(uid)) {
            if (request.typeEnum() == RequestType.FRIEND && request.statusEnum() == RequestStatus.PENDING) count++;
        }
        return count;
    }

    /**
     * BB-28: adding your own email as a friend is refused.
     * Rationale: error guessing; a user testing the feature often types their own address. Upper
     * case checks the comparison ignores case. No new friend request may be created.
     */
    @Test
    public void addingYourselfAsFriendIsRefused() {
        int before = pendingFriendRequestsSent();
        String ownEmail = AccountManager.getInstance().getCurrentUser().getEmail().toUpperCase();
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.nav_friends)).perform(click());
            onView(withId(R.id.fabCreate)).perform(click());
            onView(isAssignableFrom(EditText.class)).inRoot(isDialog()).perform(replaceText(ownEmail));
            onView(withText(R.string.add_short)).inRoot(isDialog()).perform(click());

            onView(withId(R.id.tvHeaderTitle)).check(matches(withText(R.string.your_friends)));
        }
        assertEquals("No friend request was created", before, pendingFriendRequestsSent());
    }

    /**
     * BB-29: the Requests tab hides the + button and shows either the empty message or Accept and
     * Deny on each pending request.
     * Rationale: requirement check for the Requests screen. The expected screen depends on whether
     * the test account has pending requests, so the test reads that first and checks the matching
     * state.
     */
    @Test
    public void requestsTabShowsEmptyStateOrAnswerButtons() {
        boolean hasPending = !RequestManager.getInstance().listIncomingPending().isEmpty();
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.nav_requests)).perform(click());
            onView(withId(R.id.tvHeaderTitle)).check(matches(withText(R.string.your_requests)));
            onView(withId(R.id.fabCreate)).check(matches(not(isDisplayed())));

            if (hasPending) {
                onView(withId(R.id.rvRequests)).check(matches(hasDescendant(allOf(withId(R.id.btnAccept), isDisplayed()))));
                onView(withId(R.id.rvRequests)).check(matches(hasDescendant(allOf(withId(R.id.btnDeny), isDisplayed()))));
            } else {
                onView(withId(R.id.tvEmptyRequests)).check(matches(isDisplayed()));
            }
        }
    }

    /**
     * BB-30: the theme button switches light/dark and keeps you on the same tab.
     * Rationale: the theme change recreates the screen, which used to send users back to Groups.
     * Starts on Friends so that a reset to the default tab would be caught. The original theme is
     * restored afterwards.
     */
    @Test
    public void themeButtonSwitchesModeAndKeepsTab() {
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.nav_friends)).perform(click());
            int before = AppCompatDelegate.getDefaultNightMode();

            onView(withId(R.id.btnTheme)).perform(click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            assertNotEquals("Theme changed", before, AppCompatDelegate.getDefaultNightMode());
            onView(withId(R.id.tvHeaderTitle)).check(matches(withText(R.string.your_friends)));
            onView(withId(R.id.panelFriends)).check(matches(isDisplayed()));
        }
    }
}
