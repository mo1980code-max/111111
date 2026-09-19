package com.clock.livewallpaper.catalog;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clock.livewallpaper.unlock.UnlockPrompt;
import com.clock.livewallpaper.unlock.UnlockStore;

/**
 * One place that decides whether a piece of content can be opened right now.
 *
 * <p>Free items open immediately. Locked items show the "watch an ad to unlock" dialog; if the reward
 * is earned the item is unlocked permanently, the list is refreshed and the caller continues as if it
 * had been free all along. Everything else -- no network, no fill, user declined -- leaves the item
 * locked and the UI exactly as it was.
 */
public final class ContentAccess {

    public interface Listener {
        /** The item may be opened now (it was free, already unlocked, or just unlocked). */
        void onReady(@NonNull Unlockable item);

        /** Nothing happened. {@code message} is a user-facing reason, or null when the user declined. */
        void onBlocked(@Nullable String message);
    }

    private ContentAccess() {
    }

    public static boolean isAvailable(@NonNull android.content.Context context, @NonNull Unlockable item) {
        return UnlockStore.get(context).isAvailable(item.getUnlockId(), item.isFreeByDefault());
    }

    public static void open(@NonNull Activity activity, @NonNull final Unlockable item,
                            @Nullable final Runnable refresh, @NonNull final Listener listener) {
        if (isAvailable(activity, item)) {
            listener.onReady(item);
            return;
        }
        UnlockPrompt.Result result = new UnlockPrompt.Result() {
            @Override
            public void onUnlocked() {
                if (refresh != null) {
                    refresh.run();
                }
                listener.onReady(item);
            }

            @Override
            public void onUnavailable(@Nullable String message) {
                listener.onBlocked(message);
            }
        };
        if (item.isClockContent()) {
            UnlockPrompt.showClock(activity, item.getUnlockId(), result);
        } else {
            UnlockPrompt.showWallpaper(activity, item.getUnlockId(), result);
        }
    }
}
