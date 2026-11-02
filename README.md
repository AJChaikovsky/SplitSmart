# SplitSmart

**Team 10 — SplitSmart Inc.**

Android expense-splitting app for USC students. Split group expenses equally, by exact amounts, or by percentage; track pairwise balances; record settlements; and review history and monthly summaries.

Package: `com.splitsmart.app`  
Min SDK 26 · Target SDK 34 · Compile SDK 36

## Architecture (layered)

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
   Android Studio’s bundled JBR 25 may fail Kotlin compilation.
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

## Main flows

Register → Login → Home (groups) → Create Group → Group Detail (Members, Add Expense, Balances, History, Monthly Summary) → full CRUD for expenses and settlements (creator / owner rules apply).

## License

Course project — Team 10 SplitSmart Inc.
