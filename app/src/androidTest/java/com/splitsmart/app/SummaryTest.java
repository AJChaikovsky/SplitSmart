package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.splitsmart.app.ui.UiUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Calendar;

/** Max: BB-24, invalid month boundary followed by a valid current-month request. */
@RunWith(AndroidJUnit4.class)
public class SummaryTest extends SeededEspressoTestBase {
    @Test public void monthRange() {
        openEspressoGroup();
        selectGroupTab(R.string.overview);
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        onView(withId(R.id.tvTotalSpending)).check(matches(withText("$30.00")));
        onView(withId(R.id.etMonth)).perform(scrollTo(), replaceText("13"), closeSoftKeyboard());
        onView(withId(R.id.btnLoad)).perform(click());
        onView(withId(R.id.etMonth)).check(matches(isDisplayed())).check(matches(withText("13")));
        // Toasts are not portable across Android versions. Rejection leaves the last valid result intact.
        onView(withId(R.id.tvSummary)).check(matches(withText(String.format(java.util.Locale.US,
                "%d-%02d · By category", year, month))));
        onView(withId(R.id.tvTotalSpending)).check(matches(withText("$30.00")));
        onView(withId(R.id.etMonth)).perform(replaceText(String.valueOf(month)), closeSoftKeyboard());
        onView(withId(R.id.btnLoad)).perform(click());
        onView(withId(R.id.tvExpenseCount)).check(matches(withText("1 expense")));
        onView(withId(R.id.tvTotalSpending)).check(matches(withText(UiUtils.formatMoney(30.0))));
    }
}
