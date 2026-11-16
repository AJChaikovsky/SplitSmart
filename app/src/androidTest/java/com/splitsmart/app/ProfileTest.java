package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.ui.ProfileActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Black-box test for the profile screen (owner: AJ, BB-08). */
@RunWith(AndroidJUnit4.class)
public class ProfileTest extends EspressoTestBase {
    @Before
    public void signIn() {
        TestAccount.ensureSignedIn();
    }

    /**
     * BB-08: USC email and USC ID are shown but can't be edited; the name can.
     * Rationale: requirement that identity fields are fixed after registration. Checking the view
     * type (plain text vs. an input field) proves there is no way to type into them.
     */
    @Test
    public void emailAndUscIdAreReadOnlyNameIsEditable() {
        try (ActivityScenario<ProfileActivity> ignored = ActivityScenario.launch(ProfileActivity.class)) {
            onView(withId(R.id.tvEmail)).check(matches(allOf(
                    isDisplayed(), withText(endsWith("@usc.edu")), not(isAssignableFrom(EditText.class)))));
            onView(withId(R.id.tvUscId)).check(matches(allOf(
                    isDisplayed(), withText(startsWith("USC ID: ")), not(isAssignableFrom(EditText.class)))));
            onView(withId(R.id.etName)).check(matches(allOf(isAssignableFrom(EditText.class), isEnabled())));
        }
    }
}
