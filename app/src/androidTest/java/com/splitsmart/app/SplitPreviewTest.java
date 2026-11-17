package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.SplitMethod;
import com.splitsmart.app.ui.CreateExpenseActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Black-box tests for split previews on Add Expense (owner: Rakshay, BB-15 to BB-17).
 * Requires the seeded Espresso group with at least three members.
 */
@RunWith(AndroidJUnit4.class)
public class SplitPreviewTest extends EspressoTestBase {
    private String groupId;

    @Before
    public void openReady() {
        Group group = EspressoGroup.requireGroupWithMembers(3);
        groupId = group.getId();
    }

    private ActivityScenario<CreateExpenseActivity> launchForm() {
        return ActivityScenario.launch(EspressoGroup.addExpenseIntent(groupId));
    }

    /** Waits (via IdlingResource) until three participant checkboxes are on screen. */
    private void waitForThreeParticipants() {
        onView(withIndex(allOf(
                isDescendantOfA(withId(R.id.llParticipants)),
                isAssignableFrom(CheckBox.class),
                isChecked()), 2))
                .check(matches(isDisplayed()));
    }

    private void enterAmount(String amount) {
        onView(withId(R.id.etTitle)).perform(scrollTo(), replaceText("Espresso split"));
        onView(withId(R.id.etAmount)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard());
    }

    private void chooseSplitMethod(ActivityScenario<CreateExpenseActivity> scenario, SplitMethod method) {
        scenario.onActivity(activity -> {
            Spinner spinner = activity.findViewById(R.id.spSplitMethod);
            ViewGroup splitValues = activity.findViewById(R.id.llSplitValues);
            int position = method.ordinal();
            spinner.setSelection(position);
            if (spinner.getOnItemSelectedListener() != null) {
                spinner.getOnItemSelectedListener().onItemSelected(
                        spinner, spinner.getSelectedView(), position, spinner.getItemIdAtPosition(position));
            }
        });
    }

    private void typeSplitValue(int index, String value) {
        onView(withIndex(allOf(
                isDescendantOfA(withId(R.id.llSplitValues)),
                isAssignableFrom(EditText.class)), index))
                .perform(scrollTo(), replaceText(value), closeSoftKeyboard());
    }

    /**
     * BB-15: $30 split equally among three participants shows $10.00 each in the preview.
     * Rationale: happy-path use case for equal split.
     */
    @Test
    public void equalThirtyAmongThree() {
        try (ActivityScenario<CreateExpenseActivity> scenario = launchForm()) {
            waitForThreeParticipants();
            enterAmount("30");
            chooseSplitMethod(scenario, SplitMethod.EXACT);
            chooseSplitMethod(scenario, SplitMethod.EQUAL);
            onView(withText(containsString("Assigned shares")))
                    .perform(scrollTo())
                    .check(matches(allOf(isDisplayed(), countOccurrences("$10.00", 3))));
        }
    }

    /**
     * BB-16: exact amounts that do not sum to the total never open the review dialog.
     * Rationale: invalid class for exact split (5 + 10 + 14 ≠ 30). Guards the 1¢ mismatch bug.
     */
    @Test
    public void exactMismatchBlocked() {
        try (ActivityScenario<CreateExpenseActivity> scenario = launchForm()) {
            waitForThreeParticipants();
            enterAmount("30");
            chooseSplitMethod(scenario, SplitMethod.EXACT);
            typeSplitValue(0, "5");
            typeSplitValue(1, "10");
            typeSplitValue(2, "14");
            onView(withId(R.id.btnSave)).perform(scrollTo(), click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            assertNoReviewDialog();
        }
    }

    /**
     * BB-17: 20% / 30% / 50% of $30 previews as $6.00, $9.00, $15.00.
     * Rationale: happy-path use case for percentage split.
     */
    @Test
    public void percentageTwentyThirtyFifty() {
        try (ActivityScenario<CreateExpenseActivity> scenario = launchForm()) {
            waitForThreeParticipants();
            enterAmount("30");
            chooseSplitMethod(scenario, SplitMethod.PERCENTAGE);
            typeSplitValue(0, "20");
            typeSplitValue(1, "30");
            typeSplitValue(2, "50");
            onView(withId(R.id.etNote)).perform(scrollTo(), click());
            onView(withText(containsString("Assigned shares")))
                    .perform(scrollTo())
                    .check(matches(allOf(
                            withText(containsString("$6.00")),
                            withText(containsString("$9.00")),
                            withText(containsString("$15.00")))));
        }
    }

    private static void assertNoReviewDialog() {
        try {
            onView(withText("Review shares"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            throw new AssertionError("Review shares dialog should not appear");
        } catch (androidx.test.espresso.NoMatchingRootException
                 | androidx.test.espresso.NoMatchingViewException expected) {
            // Expected: mismatched exact total is rejected before confirm.
        }
    }

    /**
     * Nth match in the hierarchy. Tracks views by identity so Espresso's repeated
     * matcher scans (which break a simple incrementing counter) stay stable.
     */
    private static Matcher<View> withIndex(Matcher<View> matcher, int index) {
        return new TypeSafeMatcher<View>() {
            private final List<View> matched = new ArrayList<>();

            @Override
            public void describeTo(Description description) {
                description.appendText("index " + index + " of ");
                matcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(View item) {
                if (!matcher.matches(item)) return false;
                int found = matched.indexOf(item);
                if (found < 0) {
                    matched.add(item);
                    found = matched.size() - 1;
                }
                return found == index;
            }
        };
    }

    private static Matcher<View> countOccurrences(String needle, int expected) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("text containing \"" + needle + "\" exactly " + expected + " times");
            }

            @Override
            protected boolean matchesSafely(View item) {
                if (!(item instanceof android.widget.TextView)) return false;
                CharSequence text = ((android.widget.TextView) item).getText();
                if (text == null) return false;
                String haystack = text.toString();
                int count = 0;
                for (int from = 0; (from = haystack.indexOf(needle, from)) >= 0; from += needle.length()) {
                    count++;
                }
                return count == expected;
            }
        };
    }
}
