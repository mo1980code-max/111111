package com.clock.livewallpaper.azkar;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Persists every Azkar counter in one {@link SharedPreferences} file, so progress survives
 * closing the app, leaving the page and returning later.
 *
 * <p>Keys are {@code "azkar_count_<category>_<id>"} — stable across versions, part of the stored
 * contract. There is intentionally no daily reset: counts stay until the user taps Reset on the
 * item. All writes use {@link SharedPreferences.Editor#apply()}.
 *
 * <p>Process-wide singleton: obtain it through {@link #get(Context)}.
 */
public final class AzkarProgressStore {

    /** Name of the preferences file, stable across versions. */
    public static final String PREFS_NAME = "azkar_prefs";

    private static final String KEY_PREFIX = "azkar_count_";

    private static volatile AzkarProgressStore instance;

    private final SharedPreferences prefs;

    private AzkarProgressStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return the process-wide instance, creating it on first use */
    @NonNull
    public static AzkarProgressStore get(@NonNull Context context) {
        AzkarProgressStore local = instance;
        if (local == null) {
            synchronized (AzkarProgressStore.class) {
                local = instance;
                if (local == null) {
                    local = new AzkarProgressStore(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    private static String key(@NonNull AzkarCategory category, int id) {
        return KEY_PREFIX + category.key() + "_" + id;
    }

    /** @return the saved tap count for the item, or 0 when never counted */
    public int getCount(@NonNull AzkarCategory category, int id) {
        return Math.max(0, prefs.getInt(key(category, id), 0));
    }

    /** Saves the tap count for the item. */
    public void setCount(@NonNull AzkarCategory category, int id, int count) {
        prefs.edit().putInt(key(category, id), Math.max(0, count)).apply();
    }

    /** Resets one item to zero. */
    public void reset(@NonNull AzkarCategory category, int id) {
        prefs.edit().remove(key(category, id)).apply();
    }

    /**
     * Counts completed items in {@code items} from the saved counters, without needing the
     * repository. Used by the Azkar home cards.
     */
    public int completedCount(@NonNull AzkarCategory category, @NonNull List<AzkarItem> items) {
        int done = 0;
        for (AzkarItem item : items) {
            if (getCount(category, item.id()) >= item.repeatCount()) {
                done++;
            }
        }
        return done;
    }
}
