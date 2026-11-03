package com.splitsmart.app.ui;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/** Local appearance preference, independent of the signed-in account. */
public final class ThemePreferences {
    private static final String PREFERENCES = "appearance";
    private static final String DARK_MODE = "dark_mode";

    private ThemePreferences() {
    }

    public static void applySaved(Context context) {
        applyMode(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(DARK_MODE, false));
    }

    public static boolean isDark(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void setDark(Context context, boolean enabled) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(DARK_MODE, enabled).apply();
        applyMode(enabled);
    }

    private static void applyMode(boolean enabled) {
        int mode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }
}
