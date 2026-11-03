package com.splitsmart.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/** Local appearance preference, independent of the signed-in account. */
object ThemePreferences {
    private const val PREFERENCES = "appearance"
    private const val DARK_MODE = "dark_mode"

    fun applySaved(context: Context) {
        applyMode(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(DARK_MODE, false))
    }

    fun isDark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    fun setDark(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(DARK_MODE, enabled).apply()
        applyMode(enabled)
    }

    private fun applyMode(enabled: Boolean) {
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
