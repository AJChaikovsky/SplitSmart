# SplitSmart

**Team 10 — SplitSmart Inc.**

Android expense-splitting app for USC students, written in Java. Split group expenses equally, by exact amounts, or by percentage; track pairwise balances; record settlements; and review history and monthly summaries.

Package: `com.splitsmart.app`  
Min SDK 26 · Target SDK 34 · Compile SDK 36

## Architecture (layered)

All app code is Java. Managers and repositories are synchronous (they block on Firebase `Task`s), so the UI always calls them through `UiUtils.launchIo` / `UiUtils.loadData`, which run the work on a background thread and deliver the result on the main thread.

- **Presentation** — Activities under `ui/`
- **Business Logic** — Managers and `SplitCalculator` implementations under `business/`
- **Data / Persistence** — Repositories plus `FirestoreManager` / `FirebaseAuthManager` under `data/`
- **Models** — Firestore-friendly data classes under `model/`

Firestore collections: `users`, `groups`, `expenses`, `settlements`

## Firebase (already configured)

| Item | Value |
|------|--------|
| Project ID | `splitsmart-team10` |
| Android package | `com.splitsmart.app` |
| Auth | Email/Password enabled |
| Firestore | `(default)` in `nam5` with security rules in `firestore.rules` |
| Config file | `app/google-services.json` (real, not placeholder) |

Console: https://console.firebase.google.com/project/splitsmart-team10/overview

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Hedgehog or newer recommended).
2. **File → Open** and select this project folder (`Project 2.3`).
3. Let Gradle sync. Prefer **JDK 21** for Gradle  
   (`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` on this machine).  
   Android Studio’s bundled JBR 25 may fail the Gradle build (AGP 8.7).
4. Connect a device or start an emulator (API 26+).
5. Run the `app` configuration.

### Command-line build

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
./gradlew assembleDebug
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Register with a `@usc.edu` email and a 10-digit USC ID.

## Running the tests

**White-box (JUnit, `app/src/test`)** — no emulator needed:

```bash
./gradlew testDebugUnitTest
```

In Android Studio: right-click `app/src/test` → Run Tests (or Run with Coverage for the coverage report).

**Black-box (Espresso, `app/src/androidTest`)** — start an emulator first, then:

```bash
./gradlew connectedDebugAndroidTest
```

In Android Studio: right-click `app/src/androidTest` → Run Tests. Note that the Gradle task uninstalls the app when it finishes, so you'll need to log in again afterwards. Running from Android Studio keeps the app installed.

**Test account for black-box tests that sign in.** Tests that need a login (LoginTest, ProfileTest, GroupTest, and the group/expense/settlement tests) read a test account from instrumentation arguments, so no password is stored in the repo. AJ's tests skip login-dependent cases when no credentials or existing session are available. Max's seeded screen tests require the dedicated test account and fail with a setup message if its credentials are missing.

- Android Studio: Run → Edit Configurations → your Android Instrumented Tests configuration → Instrumentation arguments → add `testEmail` and `testPassword`.
- Gradle:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.testEmail=espresso.test@usc.edu -Pandroid.testInstrumentationRunnerArguments.testPassword=YOUR_TEST_PASSWORD
```

For the local seeded account, run `python3 scripts/run_espresso_tests.py` to read credentials from the ignored `.gradle/espresso-test-credentials.json` file without putting the password in shell arguments. See [Max's test instructions and results](docs/testing-max.md) for the fixture and BB-18–BB-25 mapping.

Business-layer JUnit coverage: `./gradlew :app:businessCoverageReport`. The HTML report is generated at `app/build/reports/jacoco/businessCoverageReport/html/index.html`.

Writing a new Espresso test: extend `EspressoTestBase` (it makes Espresso wait for Firebase calls via `UiUtils.IDLING` and records which screens open). See `RegisterTest` for the pattern. Validation errors are Toasts, which Espresso can't read on Android 11+, so check that the user stays on the screen and the next screen never opens.


## Main flows

Register → Login → Home (groups) → Create Group → Group Detail (Members, Add Expense, Balances, History, Monthly Summary) → full CRUD for expenses and settlements (creator / owner rules apply).

## License

Course project — Team 10 SplitSmart Inc.
