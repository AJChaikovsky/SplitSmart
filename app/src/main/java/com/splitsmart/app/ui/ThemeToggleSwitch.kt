package com.splitsmart.app.ui

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.switchmaterial.SwitchMaterial
import com.splitsmart.app.R

/** The same accessible, persistent light/dark toggle in the home and group headers. */
class ThemeToggleSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchMaterial(context, attrs) {
    init {
        text = context.getString(R.string.dark_mode_short)
        contentDescription = context.getString(R.string.dark_mode)
        setTextAppearance(R.style.TextAppearance_SplitSmart_Caption)
        minHeight = context.dp(48)
        minimumWidth = context.dp(96)
        switchPadding = context.dp(8)
        setPadding(context.dp(8), 0, context.dp(8), 0)
        isChecked = ThemePreferences.isDark(context)
        setOnCheckedChangeListener { _, enabled -> ThemePreferences.setDark(context, enabled) }
    }
}
