package com.clock.livewallpaper.unlock;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdsManager;

/**
 * The one and only rewarded-ad entry point in the app.
 *
 * <p>A locked clock or wallpaper is never opened by an ad: tapping a locked card shows this dialog,
 * and only the explicit "watch an ad to unlock" choice reaches the SDK. Declining, dismissing the
 * dialog, a missing network or an empty ad response all leave the item locked and the screen
 * untouched, while every free item and every previously unlocked item keeps working.
 */
public final class UnlockPrompt {

    public interface Result {
        /** Reward earned and stored: show the item to the user now. */
        void onUnlocked();

        /** Nothing changed. {@code message} is user-facing (or null when the user simply declined). */
        void onUnavailable(@Nullable String message);
    }

    private UnlockPrompt() {
    }

    public static void showClock(@NonNull Activity activity, @NonNull String unlockKey,
                                 @NonNull Result result) {
        show(activity, R.string.unlock_clock_title, R.string.unlock_clock_message,
                R.string.unlock_clock_cta, unlockKey, result);
    }

    public static void showWallpaper(@NonNull Activity activity, @NonNull String unlockKey,
                                     @NonNull Result result) {
        show(activity, R.string.unlock_wallpaper_title, R.string.unlock_wallpaper_message,
                R.string.unlock_wallpaper_cta, unlockKey, result);
    }

    public static void show(@NonNull final Activity activity, @StringRes int titleRes,
                             @StringRes int messageRes, @StringRes int ctaRes,
                             @NonNull final String unlockKey, @NonNull final Result result) {
        if (UnlockStore.get(activity).isUnlocked(unlockKey)) {
            result.onUnlocked();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setCancelable(true)
                .setNegativeButton(R.string.unlock_later, (dialog, which) -> {
                    dialog.dismiss();
                    result.onUnavailable(null);
                })
                .setPositiveButton(ctaRes, (dialog, which) -> {
                    dialog.dismiss();
                    AdsManager.get().requestUnlock(activity, unlockKey, new AdsManager.UnlockCallback() {
                        @Override
                        public void onUnlocked() {
                            result.onUnlocked();
                        }

                        @Override
                        public void onUnavailable(@Nullable String message) {
                            result.onUnavailable(message != null ? message
                                    : activity.getString(R.string.ad_unavailable_message));
                        }
                    });
                })
                .show();
    }
}
