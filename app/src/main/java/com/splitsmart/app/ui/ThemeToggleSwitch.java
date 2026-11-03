package com.splitsmart.app.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.splitsmart.app.R;

/** The same accessible, persistent light/dark toggle in the home and group headers. */
public class ThemeToggleSwitch extends SwitchMaterial {
    public ThemeToggleSwitch(Context context) {
        this(context, null);
    }

    public ThemeToggleSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        setText(context.getString(R.string.dark_mode_short));
        setContentDescription(context.getString(R.string.dark_mode));
        setTextAppearance(R.style.TextAppearance_SplitSmart_Caption);
        setMinHeight(UiUtils.dp(context, 48));
        setMinimumWidth(UiUtils.dp(context, 96));
        setSwitchPadding(UiUtils.dp(context, 8));
        setPadding(UiUtils.dp(context, 8), 0, UiUtils.dp(context, 8), 0);
        setChecked(ThemePreferences.isDark(context));
        setOnCheckedChangeListener((button, enabled) -> ThemePreferences.setDark(context, enabled));
    }
}
