# SplitSmart

**Team 10 — SplitSmart Inc.**

Android expense-splitting app for USC students. Split group expenses equally, by exact amounts, or by percentage; track pairwise balances; record settlements; and review history and monthly summaries.

Package: `com.splitsmart.app`  
Min SDK 26 · Target / Compile SDK 34

## Architecture (layered)

- **Presentation** — Activities under `ui/`
- **Business Logic** — Managers and `SplitCalculator` implementations under `business/`
- **Data / Persistence** — Repositories plus `FirestoreManager` / `FirebaseAuthManager` under `data/`
- **Models** — Firestore-friendly data classes under `model/`

Firestore collections: `users`, `groups`, `expenses`, `settlements`

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Hedgehog or newer recommended).
2. **File → Open** and select this project folder (`Project 2.3`).
3. Let Gradle sync. If prompted, use the bundled JDK  
   (`/Applications/Android Studio.app/Contents/jbr/Contents/Home` on macOS).
4. Connect a device or start an emulator (API 26+).
5. Run the `app` configuration.

### Command-line build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

Unit tests:

```bash
./gradlew test
```

## Firebase setup (required for Auth / Firestore at runtime)

The repo includes a **placeholder** `app/google-services.json` with `project_id` `splitsmart-placeholder` so Gradle sync and `assembleDebug` succeed. **Replace it with a real file before using login, registration, or cloud data.**

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project (e.g. `SplitSmart`).
2. Add an **Android** app with package name `com.splitsmart.app`.
3. Download `google-services.json` and replace `app/google-services.json`.
4. Enable **Authentication → Sign-in method → Email/Password**.
5. Create a **Cloud Firestore** database (start in test mode for development, then tighten rules).
6. Suggested security rules for development only:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

7. Rebuild and run. Register with a `@usc.edu` email and a 10-digit USC ID.

## Main flows

Register → Login → Home (groups) → Create Group → Group Detail (Members, Add Expense, Balances, History, Monthly Summary) → full CRUD for expenses and settlements (creator / owner rules apply).

## License

Course project — Team 10 SplitSmart Inc.
