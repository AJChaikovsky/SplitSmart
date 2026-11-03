package com.splitsmart.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.splitsmart.app.ui.ThemePreferences;

public class SplitSmartApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySaved(this);
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception ignored) {
            // Placeholder google-services.json may lack a real project; runtime Auth/Firestore needs a real file.
        }
    }
}
