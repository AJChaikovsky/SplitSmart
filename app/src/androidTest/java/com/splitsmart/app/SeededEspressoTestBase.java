package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.ui.HomeActivity;

import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;

/** Shared UI-test setup. Never falls back to a developer's signed-in personal account. */
public abstract class SeededEspressoTestBase extends EspressoTestBase {
    protected final ActivityScenarioRule<HomeActivity> home = new ActivityScenarioRule<>(HomeActivity.class);

    @Rule
    public final RuleChain sessionAndScreen = RuleChain.outerRule(new ExternalResource() {
        @Override
        protected void before() {
            assertTrue("Supply dedicated testEmail/testPassword arguments (see scripts/run_espresso_tests.py)",
                    TestAccount.supplied() && "espresso.test@usc.edu".equals(TestAccount.email()));
            // Runs on the instrumentation thread, before ActivityScenarioRule opens Home.
            AccountManager.getInstance().login(TestAccount.email(), TestAccount.password());
        }
    }).around(home);

    protected void openEspressoGroup() {
        onView(allOf(withText("Espresso"), isDisplayed())).perform(click());
        onView(withId(R.id.groupTabs)).check(matches(isDisplayed()));
    }

    protected void selectGroupTab(int label) {
        onView(allOf(withText(label), isDescendantOfA(withId(R.id.groupTabs)), isDisplayed())).perform(click());
        assertGroupTabSelected(label);
    }

    protected void assertGroupTabSelected(int label) {
        onView(allOf(withText(label), isDescendantOfA(withId(R.id.groupTabs)), isDisplayed()))
                .check(matches(isSelected()));
    }
}
