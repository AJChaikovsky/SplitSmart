package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Max: BB-25. Android recreates an Activity on rotation or a theme change. */
@RunWith(AndroidJUnit4.class)
public class HomeTest extends SeededEspressoTestBase {
    @Test public void tabSurvivesRecreate() {
        onView(withId(R.id.nav_activity)).perform(click()).check(matches(isSelected()));
        onView(withId(R.id.panelActivity)).check(matches(isDisplayed()));
        home.getScenario().recreate();
        onView(withId(R.id.nav_activity)).check(matches(isSelected()));
        onView(withId(R.id.panelActivity)).check(matches(isDisplayed()));
        onView(withId(R.id.tvHeaderTitle)).check(matches(withText(R.string.your_activity)));
    }
}
