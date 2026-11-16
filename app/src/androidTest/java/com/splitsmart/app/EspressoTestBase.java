package com.splitsmart.app;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.Intents;

import com.splitsmart.app.ui.UiUtils;

import org.junit.After;
import org.junit.Before;

/**
 * Shared setup for black-box (Espresso) tests.
 *
 * <ul>
 *   <li>Registers {@link UiUtils#IDLING} so Espresso waits for background work (Firebase calls)
 *       to finish and reach the screen before checking anything. No sleeps needed.</li>
 *   <li>Records launched screens with Espresso-Intents, so a test can check that an invalid
 *       action did NOT open the next screen, e.g. {@code intended(hasComponent(...), times(0))}.</li>
 * </ul>
 *
 * Extend this class and launch the screen under test with an {@code ActivityScenarioRule}.
 */
public abstract class EspressoTestBase {
    @Before
    public void registerIdlingAndIntents() {
        IdlingRegistry.getInstance().register(UiUtils.IDLING);
        Intents.init();
    }

    @After
    public void releaseIdlingAndIntents() {
        Intents.release();
        IdlingRegistry.getInstance().unregister(UiUtils.IDLING);
    }
}
