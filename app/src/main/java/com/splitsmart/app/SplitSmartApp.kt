package com.splitsmart.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.splitsmart.app.ui.ThemePreferences

class SplitSmartApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferences.applySaved(this)
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {
            // Placeholder google-services.json may lack a real project; runtime Auth/Firestore needs a real file.
        }
    }
}
