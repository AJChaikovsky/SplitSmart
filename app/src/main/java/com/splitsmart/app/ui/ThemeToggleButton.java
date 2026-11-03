package com.splitsmart.app.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.google.android.material.button.MaterialButton;
import com.splitsmart.app.R;

/**
 * Tap to switch between light and dark mode. Shows a moon in light mode and a sun in dark mode.
 * Icon-only in the home header; given any android:text it shows a matching label instead.
 */
public class ThemeToggleButton extends MaterialButton {
    public ThemeToggleButton(Context context) {
        this(context, null);
    }

    public ThemeToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialButtonStyle);
    }

    public ThemeToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        boolean dark = ThemePreferences.isDark(context);
        String label = context.getString(dark ? R.string.light_mode_action : R.string.dark_mode_action);
        setIconResource(dark ? R.drawable.ic_sun : R.drawable.ic_moon);
        setContentDescription(label);
        setTooltipText(label);
        if (!TextUtils.isEmpty(getText())) setText(label);
        setOnClickListener(v -> ThemePreferences.setDark(context, !dark));
    }
}
