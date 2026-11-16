package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Max: BB-18–BB-19. Observe the default destination and persistent tab navigation. */
@RunWith(AndroidJUnit4.class)
public class GroupWorkspaceTest extends SeededEspressoTestBase {
    @Before public void openGroup() { openEspressoGroup(); }

    /** BB-18, happy path: opening a group immediately shows both balance summaries. */
    @Test public void opensOnBalances() {
        assertGroupTabSelected(R.string.balances);
        onView(withText(R.string.you_owe)).check(matches(isDisplayed()));
        onView(withText(R.string.you_are_owed)).check(matches(isDisplayed()));
        onView(withId(R.id.tvToPay)).check(matches(withText("$10.00")));
        onView(withId(R.id.tvToReceive)).check(matches(withText("$0.00")));
    }

    /** BB-19, navigation: each persistent tab selects and reveals its own controls. */
    @Test public void tabsSwitchContent() {
        selectGroupTab(R.string.history);
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
        selectGroupTab(R.string.overview);
        onView(withId(R.id.etYear)).check(matches(isDisplayed()));
        onView(withId(R.id.etMonth)).check(matches(isDisplayed()));
        selectGroupTab(R.string.balances);
        onView(withId(R.id.tvToPay)).check(matches(isDisplayed()));
    }
}
