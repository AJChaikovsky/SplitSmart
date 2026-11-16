package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Max: BB-20–BB-22. Rejected submissions must retain the form and the original $10 debt. */
@RunWith(AndroidJUnit4.class)
public class SettlementFormTest extends SeededEspressoTestBase {
    @Before public void openForm() {
        openEspressoGroup();
        onView(withId(R.id.btnCreateSettlement)).perform(click());
        onView(withId(R.id.etAmount)).perform(scrollTo()).check(matches(withText("10.00")));
    }

    /** BB-20, requirement check: receiving a payment changes the counterparty label. */
    @Test public void directionToggleChangesLabel() {
        onView(withId(R.id.tvPersonLabel)).perform(scrollTo()).check(matches(withText(R.string.settlement_paid_to)));
        onView(withId(R.id.btnIWasPaid)).perform(scrollTo(), click()).check(matches(isChecked()));
        onView(withId(R.id.tvPersonLabel)).check(matches(withText(R.string.settlement_received_from)));
        onView(withId(R.id.btnIPaid)).perform(click()).check(matches(isChecked()));
        onView(withId(R.id.tvPersonLabel)).check(matches(withText(R.string.settlement_paid_to)));
    }

    private void assertRejected(String amount) {
        onView(withId(R.id.etAmount)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard());
        onView(withId(R.id.btnSave)).perform(scrollTo(), click()).check(matches(isDisplayed()));
        onView(withId(R.id.etAmount)).perform(scrollTo()).check(matches(withText(amount)));
        // Navigate back through the app's toolbar: a rejected payment must not reduce the debt.
        onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(
                androidx.appcompat.R.string.abc_action_bar_up_description)).perform(click());
        onView(withId(R.id.tvToPay)).check(matches(withText("$10.00")));
    }

    /** BB-21, lower boundary: a zero-dollar payment cannot be recorded. */
    @Test public void zeroAmountRejected() { assertRejected("0"); }

    /** BB-22, upper boundary: $999 exceeds the seeded $10 debt and cannot be recorded. */
    @Test public void aboveDebtRejected() { assertRejected("999"); }
}
