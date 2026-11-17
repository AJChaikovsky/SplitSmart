package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.view.View;
import android.widget.CheckBox;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.model.Group;
import com.splitsmart.app.ui.CreateExpenseActivity;
import com.splitsmart.app.ui.UiUtils;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box tests for the Add Expense form (owner: Rakshay, BB-10 to BB-14).
 * Needs the Espresso test account (signed in or via testEmail/testPassword).
 */
@RunWith(AndroidJUnit4.class)
public class ExpenseFormTest extends EspressoTestBase {
    private String groupId;

    @Before
    public void openReady() {
        Group group = EspressoGroup.requireGroup();
        groupId = group.getId();
    }

    private ActivityScenario<CreateExpenseActivity> launchForm() {
        return ActivityScenario.launch(EspressoGroup.addExpenseIntent(groupId));
    }

    private void fillAmountAndSave(String amount) {
        onView(withId(R.id.etTitle)).perform(scrollTo(), replaceText("Espresso lunch"));
        onView(withId(R.id.etAmount)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard());
        onView(withId(R.id.btnSave)).perform(scrollTo(), click());
    }

    private void assertStillOnForm() {
        onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        assertNoReviewDialog();
    }

    private static void assertNoReviewDialog() {
        try {
            onView(withText("Review shares"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            throw new AssertionError("Review shares dialog should not appear");
        } catch (androidx.test.espresso.NoMatchingRootException
                 | androidx.test.espresso.NoMatchingViewException expected) {
            // Expected: validation failed before the confirm dialog.
        }
    }

    /**
     * BB-10: category defaults to Other and lists the five required categories.
     * Rationale: requirement check for Feature 2 categories.
     */
    @Test
    public void categoryDefaultsToOther() {
        try (ActivityScenario<CreateExpenseActivity> ignored = launchForm()) {
            onView(withId(R.id.spCategory)).check(matches(allOf(
                    isDisplayed(), hasDescendant(withText("Other")))));

            onView(withId(R.id.spCategory)).perform(scrollTo(), click());
            onView(withText("Food")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            onView(withText("Housing")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            onView(withText("Transportation")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            onView(withText("Entertainment")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            onView(withText("Other")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            // Dismiss the popup by re-selecting Other.
            onData(allOf(is(instanceOf(String.class)), is("OTHER")))
                    .inRoot(isPlatformPopup())
                    .perform(click());
        }
    }

    /**
     * BB-11: the date is today (YYYY-MM-DD) and tapping it does not take focus.
     * Rationale: requirement that creation date is automatic and read-only.
     */
    @Test
    public void dateIsTodayAndReadOnly() {
        try (ActivityScenario<CreateExpenseActivity> ignored = launchForm()) {
            onView(withId(R.id.etDate)).check(matches(allOf(
                    isDisplayed(), withText(UiUtils.todayIso()))));
            onView(withId(R.id.etDate)).perform(scrollTo(), click());
            onView(withId(R.id.etDate)).check(matches(not(hasFocus())));
        }
    }

    /**
     * BB-12: amount 0 is not saved.
     * Rationale: boundary value just below a valid positive amount. Toast text can't be read on
     * Android 11+, so the observable outcome is staying on the form with no review dialog.
     */
    @Test
    public void zeroAmountNotSaved() {
        try (ActivityScenario<CreateExpenseActivity> ignored = launchForm()) {
            fillAmountAndSave("0");
            assertStillOnForm();
        }
    }

    /**
     * BB-13: an amount with three decimal places is not saved.
     * Rationale: boundary on currency precision (cents only).
     */
    @Test
    public void threeDecimalsNotSaved() {
        try (ActivityScenario<CreateExpenseActivity> ignored = launchForm()) {
            fillAmountAndSave("12.345");
            assertStillOnForm();
        }
    }

    /**
     * BB-14: unchecking the payer’s participant box is refused; it stays checked.
     * Rationale: error guessing — the payer must remain among the participants.
     */
    @Test
    public void payerStaysParticipant() {
        try (ActivityScenario<CreateExpenseActivity> ignored = launchForm()) {
            Matcher<View> payerBox = allOf(
                    isDescendantOfA(withId(R.id.llParticipants)),
                    isAssignableFrom(CheckBox.class),
                    not(isEnabled()));
            onView(payerBox).check(matches(isChecked()));
            onView(payerBox).perform(forceClick());
            onView(payerBox).check(matches(isChecked()));
        }
    }

    /** Clicks even when the view is disabled (payer checkbox is locked). */
    private static ViewAction forceClick() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "force click";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
            }
        };
    }
}
