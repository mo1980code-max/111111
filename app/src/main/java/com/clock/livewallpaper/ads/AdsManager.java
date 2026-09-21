package com.clock.livewallpaper.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.unlock.UnlockStore;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns every Google Mobile Ads SDK interaction in the app.
 *
 * <p>Three formats, no others:
 * <ul>
 *   <li><b>App open</b> -- requested in the background, shown only by
 *       {@link AppForegroundWatcher} on a warm foreground return, and only if
 *       {@link AdPolicy#canShowAppOpen(Context, long)} agrees.</li>
 *   <li><b>Rewarded</b> -- strictly opt-in. {@link #requestUnlock} is called from the "watch an ad to
 *       unlock" dialog; nothing is ever shown automatically, and the unlock is only stored when
 *       {@code onUserEarnedReward} fires.</li>
 *   <li><b>Native</b> -- loaded through {@link NativePlacement} for the home screen and the clock /
 *       wallpaper grids. A failed load leaves no empty space.</li>
 * </ul>
 *
 * <p>Interstitial ads were removed on purpose: they used to interrupt the clock and wallpaper lists
 * (and, in older builds, the reading flows), which is what this app's content policy forbids.
 */
public final class AdsManager {

    public interface UnlockCallback {
        /** The reward was earned and stored; open the content now. */
        void onUnlocked();

        /** Nothing was earned: no network, no fill, or the user backed out. Message may be null. */
        void onUnavailable(@Nullable String message);
    }

    private static final String TAG = "AdsManager";
    private static final long AD_EXPIRY_MS = 60L * 60_000L;

    private static volatile AdsManager instance;

    private final AtomicBoolean initializing = new AtomicBoolean(false);
    private volatile boolean initialised;

    @Nullable
    private volatile AppOpenAd appOpenAd;
    private volatile long appOpenLoadedAt;

    @Nullable
    private volatile RewardedAd rewardedAd;
    private volatile long rewardedLoadedAt;
    private volatile boolean rewardedShowing;

    private AdsManager() {
    }

    public static AdsManager get() {
        AdsManager local = instance;
        if (local == null) {
            synchronized (AdsManager.class) {
                if (instance == null) {
                    instance = new AdsManager();
                }
                local = instance;
            }
        }
        return local;
    }

    /**
     * Safe to call from anywhere, any number of times: the SDK itself caches the initialisation and
     * the flag here keeps us from firing it per screen.
     */
    public void initialize(@NonNull Context context) {
        if (!AdConfig.ADS_ENABLED || initialised || !initializing.compareAndSet(false, true)) {
            return;
        }
        try {
            MobileAds.initialize(context.getApplicationContext(), status -> {
                initialised = true;
                initializing.set(false);
            });
        } catch (Exception error) {
            initializing.set(false);
            Log.w(TAG, "MobileAds.initialize failed", error);
        }
    }

    /**
     * Loads an ad into memory without ever showing it. Called when a clock or wallpaper section is
     * opened, so that the (user initiated) unlock dialog responds instantly. Opening a section must
     * never present an ad -- this only warms the cache.
     */
    public void preloadRewarded(@NonNull Context context) {
        if (!shouldLoadRewarded(context)) {
            return;
        }
        loadRewarded(context, null);
    }

    private boolean shouldLoadRewarded(Context context) {
        if (!AdConfig.ADS_ENABLED || AdPolicy.isQuranSectionActive() || rewardedShowing) {
            return false;
        }
        if (!AdPolicy.hasNetwork(context)) {
            return false;
        }
        RewardedAd cached = rewardedAd;
        return cached == null || System.currentTimeMillis() - rewardedLoadedAt > AD_EXPIRY_MS;
    }

    private void loadRewarded(@NonNull final Context context, @Nullable final Runnable then) {
        initialize(context);
        try {
            RewardedAd.load(context.getApplicationContext(), AdConfig.rewardedAdUnitId(),
                    new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        rewardedLoadedAt = System.currentTimeMillis();
                        if (then != null) {
                            then.run();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        Log.i(TAG, "rewarded ad unavailable: " + error.getCode());
                        if (then != null) {
                            then.run();
                        }
                    }
                });
        } catch (Exception error) {
            // Missing / outdated Play services (or any SDK failure) must never crash the unlock flow:
            // report "no ad" and let the caller show its friendly message instead.
            rewardedAd = null;
            Log.w(TAG, "rewarded load failed", error);
            if (then != null) {
                then.run();
            }
        }
    }

    /**
     * Entry point for a locked clock or wallpaper. The caller has already been chosen by the user in
     * a dialog, so this only has to fetch the ad and hand back the reward.
     */
    public void requestUnlock(@NonNull final Activity activity, @NonNull final String unlockKey,
                              @NonNull final UnlockCallback callback) {
        if (!AdConfig.ADS_ENABLED) {
            // Ad-free build: everything the rewarded tier gates is simply free.
            UnlockStore.get(activity).unlock(unlockKey);
            callback.onUnlocked();
            return;
        }
        if (AdPolicy.isQuranSectionActive() || rewardedShowing) {
            callback.onUnavailable(activity.getString(R.string.ad_busy_message));
            return;
        }
        if (!AdPolicy.hasNetwork(activity)) {
            callback.onUnavailable(activity.getString(R.string.ad_offline_message));
            return;
        }
        AdPolicy.markAdShowing();
        RewardedAd cached = rewardedAd;
        if (cached != null && System.currentTimeMillis() - rewardedLoadedAt <= AD_EXPIRY_MS) {
            showRewarded(activity, cached, unlockKey, callback);
        } else {
            loadRewarded(activity, () -> {
                RewardedAd fresh = rewardedAd;
                if (fresh == null) {
                    finishUnlock(false, activity, unlockKey, callback);
                    return;
                }
                showRewarded(activity, fresh, unlockKey, callback);
            });
        }
    }

    private void showRewarded(@NonNull final Activity activity, @NonNull RewardedAd ad,
                              @NonNull final String unlockKey, @NonNull final UnlockCallback callback) {
        rewardedShowing = true;
        ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedShowing = false;
                rewardedAd = null;
                AdPolicy.markAdDismissed();
                // The consumed ad is gone: warm the cache for the next locked item so its dialog
                // answers instantly. This is a single background load, never an automatic show.
                preloadRewarded(activity.getApplicationContext());
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError error) {
                rewardedShowing = false;
                rewardedAd = null;
                AdPolicy.markAdDismissed();
                finishUnlock(false, activity, unlockKey, callback);
            }
        });
        try {
            ad.show(activity, rewardItem -> finishUnlock(true, activity, unlockKey, callback));
        } catch (Exception error) {
            rewardedShowing = false;
            Log.w(TAG, "rewarded show failed", error);
            finishUnlock(false, activity, unlockKey, callback);
        }
    }

    private void finishUnlock(boolean earned, @NonNull Activity activity, @NonNull String unlockKey,
                              @NonNull UnlockCallback callback) {
        if (earned) {
            // The reward is stored locally and permanently: from now on this item never asks for an ad.
            UnlockStore.get(activity).unlock(unlockKey);
            callback.onUnlocked();
        } else {
            callback.onUnavailable(activity.getString(R.string.ad_unavailable_message));
            // One background reload attempt so a transient no-fill does not leave every later tap
            // cold. This cannot loop: the reload carries no callback, so its own failure ends here.
            preloadRewarded(activity.getApplicationContext());
        }
        if (!rewardedShowing) {
            // Only the paths that never reached the screen have to clear the flag here (a failed load);
            // while an ad is showing, its own dismissal callback does it, so one ad can never be
            // followed immediately by another.
            AdPolicy.markAdDismissed();
        }
    }

    // --- app open -----------------------------------------------------------------------------

    /** Loads (or reloads) an app open ad; never shows anything. */
    public void preloadAppOpen(@NonNull Context context) {
        if (!AdConfig.ADS_ENABLED || AdPolicy.isQuranSectionActive() || !AdPolicy.hasNetwork(context)) {
            return;
        }
        AppOpenAd cached = appOpenAd;
        if (cached != null && System.currentTimeMillis() - appOpenLoadedAt <= AdConfig.APP_OPEN_EXPIRY_MS) {
            return;
        }
        initialize(context);
        try {
            AppOpenAd.load(context.getApplicationContext(), AdConfig.appOpenAdUnitId(),
                    new AdRequest.Builder().build(), AdPolicy.requestedOrientation(),
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            appOpenAd = ad;
                            appOpenLoadedAt = System.currentTimeMillis();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            appOpenAd = null;
                            Log.i(TAG, "app open ad unavailable: " + error.getCode());
                        }
                    });
        } catch (Exception error) {
            // Same guarantee as every other format: an SDK failure degrades to "no ad", never a crash.
            appOpenAd = null;
            Log.w(TAG, "app open load failed", error);
        }
    }

    /**
     * Called by the foreground watcher only when the app really came back from the background. Shows
     * nothing if the ad is stale, if a section is protected or if another ad is on screen.
     */
    public void showAppOpenIfAllowed(@NonNull final Activity activity) {
        final AppOpenAd ad = appOpenAd;
        if (ad == null) {
            preloadAppOpen(activity);
            return;
        }
        if (System.currentTimeMillis() - appOpenLoadedAt > AdConfig.APP_OPEN_EXPIRY_MS) {
            appOpenAd = null;
            preloadAppOpen(activity);
            return;
        }
        if (!AdPolicy.canShowAppOpen(activity, System.currentTimeMillis())) {
            return;
        }
        appOpenAd = null;
        AdPolicy.markAdShowing();
        AdPolicy.recordAppOpenShown(activity);
        ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                AdPolicy.markAdDismissed();
                // Ready for the next background/foreground cycle.
                preloadAppOpen(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError error) {
                AdPolicy.markAdDismissed();
            }
        });
        try {
            ad.show(activity);
        } catch (Exception error) {
            AdPolicy.markAdDismissed();
            Log.w(TAG, "app open show failed", error);
        }
    }

    /** True while any full screen ad owns the display (used to block stacking). */
    public boolean isShowingFullScreenAd() {
        return rewardedShowing;
    }

    // --- native -------------------------------------------------------------------------------

    /** Requests one native ad for a list placement. The callback runs on the main thread. */
    public void loadNativeAd(@NonNull final Context context, @NonNull final NativeAdConsumer consumer) {
        if (!AdPolicy.nativeAdsAllowed() || !AdPolicy.hasNetwork(context)
                || !AdPolicy.allowNativeRequest(System.currentTimeMillis())) {
            consumer.onFailed();
            return;
        }
        initialize(context);
        try {
            // AdConfig.NATIVE_AD_UNIT_ID is the official debug id; the accessor swaps it centrally for release.
            AdLoader loader = new AdLoader.Builder(context.getApplicationContext(), AdConfig.nativeAdUnitId())
                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                        @Override
                        public void onNativeAdLoaded(@NonNull NativeAd ad) {
                            consumer.onLoaded(ad);
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.i(TAG, "native ad unavailable: " + error.getCode());
                            consumer.onFailed();
                        }
                    })
                    .build();
            loader.loadAd(new AdRequest.Builder().build());
        } catch (Exception error) {
            Log.w(TAG, "native ad request failed", error);
            consumer.onFailed();
        }
    }

    public interface NativeAdConsumer {
        void onLoaded(@NonNull NativeAd ad);

        void onFailed();
    }
}
