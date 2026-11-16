package com.splitsmart.app;

import static org.junit.Assume.assumeTrue;

import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.splitsmart.app.business.AccountManager;

/**
 * The Espresso test account, supplied when the tests are run so no password lives in the code.
 *
 * Android Studio: Run → Edit Configurations → your test configuration → Instrumentation arguments,
 * add testEmail and testPassword. Gradle:
 * ./gradlew connectedDebugAndroidTest
 *     -Pandroid.testInstrumentationRunnerArguments.testEmail=espresso.test@usc.edu
 *     -Pandroid.testInstrumentationRunnerArguments.testPassword=...
 *
 * Tests that need an account are skipped (not failed) when it isn't supplied and the emulator
 * isn't already signed in.
 */
final class TestAccount {
    private TestAccount() {
    }

    private static String argument(String key) {
        Bundle arguments = InstrumentationRegistry.getArguments();
        String value = arguments.getString(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    static String email() {
        return argument("testEmail");
    }

    static String password() {
        return argument("testPassword");
    }

    static boolean supplied() {
        return email() != null && password() != null;
    }

    static boolean signedIn() {
        return AccountManager.getInstance().isLoggedIn();
    }

    /** Signs in with the supplied account if needed; skips the test if that isn't possible. */
    static void ensureSignedIn() {
        if (signedIn()) return;
        assumeTrue("Needs testEmail/testPassword instrumentation arguments", supplied());
        // Runs on the instrumentation thread, so the blocking Firebase call is allowed here.
        AccountManager.getInstance().login(email(), password());
    }
}
