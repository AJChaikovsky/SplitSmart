package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Max: BB-23, invalid search equivalence class, followed by recovery to normal results. */
@RunWith(AndroidJUnit4.class)
public class HistoryTest extends SeededEspressoTestBase {
    @Test public void noMatchShowsEmptyState() {
        openEspressoGroup();
        selectGroupTab(R.string.history);
        onView(withId(R.id.rvHistory)).check(matches(hasChildCount(1)));
        onView(withId(R.id.etSearch)).perform(replaceText("zzzz"), pressImeActionButton(), closeSoftKeyboard());
        onView(allOf(withId(R.id.tvEmpty), isDisplayed())).check(matches(isDisplayed())).check(matches(withText(R.string.no_transactions)));
        onView(withId(R.id.rvHistory)).check(matches(hasChildCount(0)));
        onView(withId(R.id.etSearch)).perform(replaceText(""), pressImeActionButton(), closeSoftKeyboard());
        onView(withId(R.id.rvHistory)).check(matches(hasChildCount(1)));
        onView(withText("Espresso test dinner")).check(matches(isDisplayed()));
    }
}
