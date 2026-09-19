package com.clock.livewallpaper.unlock;

import android.content.Context;
import android.content.SharedPreferences;

import com.clock.livewallpaper.ads.AdConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Permanent, device-local record of everything the user unlocked with a rewarded ad.
 *
 * <p>Keys are namespaced ({@code clock:analog_03}, {@code wallpaper:mecca_5}) and are written with
 * {@link SharedPreferences.Editor#apply()} the moment the reward is confirmed, so an unlock survives
 * process death and never needs the network again. Nothing is fetched to decide availability: the
 * free items are free by rule, and the rest are open iff their key is in here.
 */
public final class UnlockStore {

    private static final String PREFS = "content_unlocks_v1";
    private static final String PREFIX = "unlocked:";

    private static volatile UnlockStore instance;

    private final SharedPreferences preferences;
    private final Set<String> cache = Collections.synchronizedSet(new HashSet<String>());

    private UnlockStore(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith(PREFIX)) {
                cache.add(key.substring(PREFIX.length()));
            }
        }
    }

    public static UnlockStore get(Context context) {
        UnlockStore local = instance;
        if (local == null) {
            synchronized (UnlockStore.class) {
                if (instance == null) {
                    instance = new UnlockStore(context);
                }
                local = instance;
            }
        }
        return local;
    }

    /**
     * Is this item usable right now? Free items always are, everything else needs a stored unlock.
     */
    public boolean isAvailable(String unlockKey, boolean freeByDefault) {
        // An ad-free build (AdConfig.ADS_ENABLED = false) has no paid tier to gate, so the tiles and
        // this gate have to agree: nothing is drawn with a padlock that would open for free anyway.
        return freeByDefault || !AdConfig.ADS_ENABLED || isUnlocked(unlockKey);
    }

    public boolean isUnlocked(String unlockKey) {
        return unlockKey != null && cache.contains(unlockKey);
    }

    /** Called only after {@code onUserEarnedReward}; the write is permanent. */
    public void unlock(String unlockKey) {
        if (unlockKey == null || unlockKey.isEmpty()) {
            return;
        }
        cache.add(unlockKey);
        preferences.edit().putBoolean(PREFIX + unlockKey, true).apply();
    }

    public int unlockedCount(String namespace) {
        Set<String> copy;
        synchronized (cache) {
            copy = new LinkedHashSet<>(cache);
        }
        int total = 0;
        for (String key : copy) {
            if (namespace == null || key.startsWith(namespace)) {
                total++;
            }
        }
        return total;
    }

    /** Debug helper (used by tests and by a "reset purchases" style action if ever needed). */
    public void clear() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : new LinkedHashSet<>(preferences.getAll().keySet())) {
            if (key.startsWith(PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();
        cache.clear();
    }
}
