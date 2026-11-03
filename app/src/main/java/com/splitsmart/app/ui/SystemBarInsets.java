package com.splitsmart.app.ui;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15+ draws every app edge-to-edge, so screens would sit under the status bar and the
 * action bar would cover the top of each screen. This keeps every activity laid out the classic
 * way in one place: the window is padded by the system bars (and keyboard), and screen content
 * is pushed below the action bar whenever the action bar would overlap it.
 */
public final class SystemBarInsets implements Application.ActivityLifecycleCallbacks {

    public static void install(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            application.registerActivityLifecycleCallbacks(new SystemBarInsets());
        }
    }

    @Override
    public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup) || ((ViewGroup) decor).getChildCount() == 0) return;

        // The decor's first child holds the action bar and the screen content.
        View root = ((ViewGroup) decor).getChildAt(0);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);

        View actionBar = activity.findViewById(androidx.appcompat.R.id.action_bar);
        View content = activity.findViewById(android.R.id.content);
        if (actionBar != null && content != null) {
            actionBar.addOnLayoutChangeListener((bar, left, top, right, bottom,
                                                 oldLeft, oldTop, oldRight, oldBottom) ->
                    keepContentBelow(bar, content));
        }
    }

    /** Pads the content by however much the action bar overlaps it (zero if it doesn't). */
    private static void keepContentBelow(View actionBar, View content) {
        View container = (View) actionBar.getParent();
        boolean visible = actionBar.getVisibility() == View.VISIBLE
                && container.getVisibility() == View.VISIBLE;
        int overlap = visible ? container.getTop() + actionBar.getBottom() - content.getTop() : 0;
        int padding = Math.max(0, overlap);
        if (content.getPaddingTop() != padding) {
            content.post(() -> content.setPadding(
                    content.getPaddingLeft(), padding, content.getPaddingRight(), content.getPaddingBottom()));
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) { }
    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
