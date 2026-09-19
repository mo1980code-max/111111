package com.clock.livewallpaper.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Central ad gate. Every format asks this class before it loads or shows anything.
 *
 * <p>The important invariant: <b>no ad of any kind may be requested or shown while the user is
 * inside the Quran section</b>. {@link #enterQuranScreen()} / {@link #exitQuranScreen()} are called
 * from {@code onStart}/{@code onStop} of every Quran screen (index, reader, search, bookmarks), and
 * the counter -- rather than a boolean -- keeps the flag true while the reader slides from one surah
 * to the next, so nothing can slip into the gap between two Quran activities.
 */
public final class AdPolicy {

    private static final String TAG = "AdPolicy";
    private static final String PREFS = "ad_policy_v1";
    private static final String KEY_LAUNCHES = "cold_launches";
    private static final String KEY_APP_OPEN_SHOWN_AT = "last_app_open_at";

    private static int quranScreens;
    private static boolean adInFlight;
    private static long lastRewardedDismissAt;
    private static long lastNativeRequestAt;
    private static long lastLeftQuranAt;
    private static volatile boolean systemHandoffPending;

    private AdPolicy() {
    }

    // --- Quran section state ------------------------------------------------------------------

    public static void enterQuranScreen() {
        quranScreens++;
    }

    public static void exitQuranScreen() {
        if (quranScreens > 0) {
            quranScreens--;
        }
        if (quranScreens == 0) {
            // The user is fully out of the Quran section. Ads may run again, but not *because* of
            // the exit: remember the moment so nothing fires on the very next frame.
            lastLeftQuranAt = System.currentTimeMillis();
        }
    }

    /** True while any Quran screen is part of the visible task. */
    public static boolean isQuranSectionActive() {
        return quranScreens > 0;
    }

    // --- global gates -------------------------------------------------------------------------

    /** Ads are disabled inside the Quran section and while another full screen ad is showing. */
    public static boolean adsAllowed() {
        return AdConfig.ADS_ENABLED && !isQuranSectionActive() && !adInFlight;
    }

    /** Native/blocked-in-Quran check used by list placements. */
    public static boolean nativeAdsAllowed() {
        return AdConfig.ADS_ENABLED && !isQuranSectionActive();
    }

    public static void markAdShowing() {
        adInFlight = true;
    }

    public static void markAdDismissed() {
        adInFlight = false;
        lastRewardedDismissAt = System.currentTimeMillis();
    }

    // --- app open ad decisions ----------------------------------------------------------------

    /**
     * The app is about to hand over to a screen the user asked for: the live wallpaper picker, a share
     * sheet, the gallery. Coming back from it is technically a background to foreground transition, but
     * an ad there would interrupt the action in progress, so the next opportunity is skipped. This is
     * the "never over another screen" rule expressed in one call.
     */
    public static void markSystemHandoff() {
        systemHandoffPending = true;
    }

    /** True (and clears the flag) when the current foreground return must not show an app open ad. */
    public static boolean consumeSystemHandoff() {
        boolean pending = systemHandoffPending;
        systemHandoffPending = false;
        return pending;
    }


    /**
     * May an app open ad be shown now? Requires: ads enabled, not in the Quran section, no other ad
     * showing, a real background -> foreground transition (the caller only asks there), enough
     * launches behind us, and enough time since the last app open / rewarded / Quran exit.
     */
    public static boolean canShowAppOpen(Context context, long now) {
        if (!adsAllowed()) {
            return false;
        }
        if (launches(context) < AdConfig.APP_OPEN_LAUNCHES_BEFORE_FIRST_AD) {
            return false;
        }
        long sinceLastShown = now - lastLong(context, KEY_APP_OPEN_SHOWN_AT);
        if (sinceLastShown < AdConfig.APP_OPEN_MIN_INTERVAL_MS) {
            return false;
        }
        if (now - lastRewardedDismissAt < AdConfig.APP_OPEN_AFTER_REWARDED_MS) {
            return false;
        }
        if (now - lastLeftQuranAt < AdConfig.APP_OPEN_AFTER_QURAN_MS) {
            return false;
        }
        return true;
    }

    public static void recordAppOpenShown(Context context) {
        prefs(context).edit().putLong(KEY_APP_OPEN_SHOWN_AT, System.currentTimeMillis()).apply();
    }

    /** Counts process launches so a brand new install is not greeted by an ad. */
    public static int registerLaunch(Context context) {
        SharedPreferences preferences = prefs(context);
        int launches = preferences.getInt(KEY_LAUNCHES, 0) + 1;
        preferences.edit().putInt(KEY_LAUNCHES, launches).apply();
        return launches;
    }

    public static int launches(Context context) {
        return prefs(context).getInt(KEY_LAUNCHES, 0);
    }

    // --- native ad placement maths ------------------------------------------------------------

    /**
     * Content positions after which a native ad row is inserted: every {@code NATIVE_AD_INTERVAL}
     * rows, and only when the list is long enough to deserve one. Short lists (a wallpaper section
     * with six images) therefore stay completely ad free.
     */
    public static boolean wantsNativeAd(int itemCount) {
        return !nativeSlotPositions(itemCount).isEmpty();
    }

    public static List<Integer> nativeSlotPositions(int itemCount) {
        List<Integer> slots = new ArrayList<>();
        int interval = AdConfig.NATIVE_AD_INTERVAL;
        if (interval <= 0 || itemCount < AdConfig.NATIVE_AD_MIN_ITEMS) {
            return slots;
        }
        for (int position = interval; position < itemCount; position += interval) {
            slots.add(position);
        }
        return slots;
    }

    /** Throttles duplicate requests for a placement (Google asks for >= 60s between requests). */
    public static synchronized boolean allowNativeRequest(long now) {
        if (now - lastNativeRequestAt < AdConfig.NATIVE_AD_REQUEST_COOLDOWN_MS) {
            return false;
        }
        lastNativeRequestAt = now;
        return true;
    }

    // --- environment -------------------------------------------------------------------------

    /**
     * Offline judgement call: without a network we skip ad requests entirely (no empty white slot in
     * the grid) and unlock state still comes from local storage, so free and already-unlocked content
     * keeps working.
     */
    public static boolean hasNetwork(Context context) {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getApplicationContext()
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return true;
            }
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnectedOrConnecting();
        } catch (Exception error) {
            Log.w(TAG, "network check failed, assuming connectivity", error);
            return true;
        }
    }

    /** Portrait: this app is portrait only, and app open ads are requested for that orientation. */
    public static int requestedOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    // --- storage helpers ----------------------------------------------------------------------

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static long lastLong(Context context, String key) {
        return prefs(context).getLong(key, 0L);
    }
}
