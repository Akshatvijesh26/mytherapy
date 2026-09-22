package com.example.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Utility to manage software keyboard (IME) operations safely across Android versions.
 */
public final class KeyboardUtils {

    private KeyboardUtils() {
    }

    /**
     * Smoothly hides the software keyboard and clears focus if a view has focus.
     */
    public static void hideKeyboard(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        View focused = activity.getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
        }

        View decorView = activity.getWindow().getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }
    }

    /**
     * Checks if a touch event fell outside an active EditText and dismisses the keyboard.
     */
    public static void handleTouchOutsideEditText(@NonNull Activity activity, @NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    hideKeyboard(activity);
                }
            }
        }
    }
}
