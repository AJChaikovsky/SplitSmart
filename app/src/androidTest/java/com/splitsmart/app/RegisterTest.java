package com.splitsmart.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.VerificationModes;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.ui.HomeActivity;
import com.splitsmart.app.ui.RegisterActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box tests for registration (owner: AJ, BB-01 to BB-05).
 *
 * Validation errors appear as Toasts, which Espresso can't read on Android 11+. So each test
 * checks the observable outcome instead: the user stays on the Register screen and Home never
 * opens. None of these inputs pass validation, so no account is created in Firebase.
 */
@RunWith(AndroidJUnit4.class)
public class RegisterTest extends EspressoTestBase {
    @Rule
    public ActivityScenarioRule<RegisterActivity> screen = new ActivityScenarioRule<>(RegisterActivity.class);

    private void submit(String name, String email, String uscId, String password) {
        onView(withId(R.id.etName)).perform(scrollTo(), replaceText(name));
        onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText(email));
        onView(withId(R.id.etUscId)).perform(scrollTo(), replaceText(uscId));
        onView(withId(R.id.etPassword)).perform(scrollTo(), replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btnRegister)).perform(scrollTo(), click());
    }

    private void assertRegistrationRefused() {
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()));
        intended(hasComponent(HomeActivity.class.getName()), VerificationModes.times(0));
    }

    /**
     * BB-01: an email outside @usc.edu is rejected.
     * Rationale: equivalence class "valid format, wrong domain"; gmail.com is the most common
     * non-USC address a student would try. Every other field is valid, so only the email can fail.
     */
    @Test
    public void rejectsNonUscEmail() {
        submit("Eve Tester", "eve@gmail.com", "1234567890", "password1");
        assertRegistrationRefused();
    }

    // AJ: BB-02 to BB-05 go here, reusing submit(...) and assertRegistrationRefused():
    //   BB-02 rejectsNineDigitId       -> uscId "123456789"
    //   BB-03 rejectsElevenDigitId     -> uscId "12345678901"
    //   BB-04 rejectsIdWithLetter      -> uscId "12345678A9"
    //   BB-05 rejectsBlankName         -> name ""
}
