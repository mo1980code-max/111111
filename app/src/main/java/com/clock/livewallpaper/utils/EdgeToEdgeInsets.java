package com.clock.livewallpaper.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.ColorInt;

/**
 * Edge-to-edge setup for the Quran screens on a targetSdk 36 app.
 *
 * <p>From Android 15 the system bars no longer inset the app window, so content draws underneath the
 * status bar, the navigation bar and any display cutout. Rather than opting out (which is ignored at
 * target 35+), each screen hands its root view here: the root keeps its own design padding and gains
 * the current system insets on top.
 *
 * <p>Shared by {@code SurahListActivity} and {@code QuranActivity} so both behave the same on
 * gesture navigation, three-button navigation, cutouts and large-screen resizing.
 */
public final class EdgeToEdgeInsets {

    private EdgeToEdgeInsets() { }

    /**
     * Applies system-bar, cutout and IME insets as padding on {@code root}, and colours the bars to
     * match the screen background.
     *
     * @param activity        the activity owning {@code root}
     * @param root            the screen's outermost view; its existing padding is preserved
     * @param backgroundColor used for the status/navigation bars below API 30
     */
    @SuppressWarnings("deprecation")
    public static void apply(Activity activity, View root, @ColorInt int backgroundColor) {
        if (activity == null || root == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);

            // Captured once: the listener fires repeatedly (rotation, IME, cutout changes) and must
            // always add insets to the ORIGINAL design padding, never to the last applied total.
            final int designLeft = root.getPaddingLeft();
            final int designTop = root.getPaddingTop();
            final int designRight = root.getPaddingRight();
            final int designBottom = root.getPaddingBottom();

            root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets safe = windowInsets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout()
                        | WindowInsets.Type.ime());
                view.setPadding(designLeft + safe.left,
                        designTop + safe.top,
                        designRight + safe.right,
                        designBottom + safe.bottom);
                return WindowInsets.CONSUMED;
            });
            root.requestApplyInsets();

            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                // Paper-coloured backgrounds are light, so the icons must be dark to stay legible.
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars, lightBars);
            }
            if (Build.VERSION.SDK_INT < 35) {
                activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
                activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
            return;
        }

        // API 23-29: no enforced edge to edge, so paint the bars to match the screen instead.
        activity.getWindow().setStatusBarColor(backgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(backgroundColor);
        }
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }
}
