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
import static org.junit.Assume.assumeTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.VerificationModes;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.ui.HomeActivity;
import com.splitsmart.app.ui.LoginActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box tests for login (owner: AJ, BB-06 and BB-07).
 *
 * The login screen only shows when signed out, so each test signs out first. If the emulator is
 * signed in and no test account is supplied, the tests are skipped rather than leave you signed
 * out of an account they can't sign back into. See {@link TestAccount}.
 */
@RunWith(AndroidJUnit4.class)
public class LoginTest extends EspressoTestBase {
    @Before
    public void startSignedOut() {
        if (TestAccount.signedIn()) {
            assumeTrue("Signed in and no test account supplied to restore the session", TestAccount.supplied());
            AccountManager.getInstance().logout();
        }
    }

    @After
    public void restoreSession() {
        if (!TestAccount.signedIn() && TestAccount.supplied()) {
            AccountManager.getInstance().login(TestAccount.email(), TestAccount.password());
        }
    }

    private void logIn(String email, String password) {
        onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText(email));
        onView(withId(R.id.etPassword)).perform(scrollTo(), replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(scrollTo(), click());
    }

    /**
     * BB-06: a wrong password keeps the user on the login screen.
     * Rationale: error guessing, the most common real login mistake. Uses the test account's email
     * when supplied, so only the password is wrong; otherwise an unregistered USC email.
     */
    @Test
    public void wrongPasswordStaysOnLogin() {
        String email = TestAccount.supplied() ? TestAccount.email() : "nobody.splitsmart@usc.edu";
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            logIn(email, "wrong-password-310");
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
            intended(hasComponent(HomeActivity.class.getName()), VerificationModes.times(0));
        }
    }

    /**
     * BB-07: valid credentials open the home screen with the user's groups.
     * Rationale: the main success path of the login use case.
     */
    @Test
    public void validLoginShowsGroups() {
        assumeTrue("Needs testEmail/testPassword instrumentation arguments", TestAccount.supplied());
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            logIn(TestAccount.email(), TestAccount.password());
            intended(hasComponent(HomeActivity.class.getName()));
            onView(withId(R.id.rvGroups)).check(matches(isDisplayed()));
        }
    }
}
