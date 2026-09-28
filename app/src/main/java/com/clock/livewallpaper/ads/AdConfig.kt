package com.clock.livewallpaper.ads

/**
 * Every advert identifier and every timing rule of the ad layer, in one place.
 *
 * The unit ids below are **Google's official test units** (publisher `3940256099942544`). They
 * always return a test creative, they never earn money and they are not attached to any AdMob
 * account, so they are the only ids this repository is allowed to contain. Going live means
 * replacing them here - and only here - once the real units exist. The AdMob application id lives
 * in `AndroidManifest.xml`, because that is the only place the SDK reads it from.
 *
 * The timings below are the product policy: how long the app has to be in use before a full-screen
 * ad may appear at all, how far apart two of them have to be, and how long a cached ad may be
 * reused. [AdsCoordinator] is the only reader of them, so the policy cannot drift between formats.
 */
object AdConfig {

    // ------------------------------------------------------------------ units

    const val BANNER_UNIT_ID: String = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_UNIT_ID: String = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_UNIT_ID: String = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Configured for completeness: the app has no reward-worthy feature today, so no rewarded
     * interstitial is requested. Kept next to its siblings so a future placement cannot invent an
     * id of its own.
     */
    const val REWARDED_INTERSTITIAL_UNIT_ID: String = "ca-app-pub-3940256099942544/5354046379"

    const val NATIVE_UNIT_ID: String = "ca-app-pub-3940256099942544/2247696110"
    const val APP_OPEN_UNIT_ID: String = "ca-app-pub-3940256099942544/9257395921"

    // ----------------------------------------------------------------- policy

    /** No full-screen ad in the first moments of a session: the user came here to read dhikr. */
    const val SESSION_WARM_UP_MS: Long = 45_000L

    /** Minimum distance between any two full-screen ads, whatever their format. */
    const val FULLSCREEN_MIN_GAP_MS: Long = 3 * 60 * 1000L

    /** The first finished reading session is never monetised; the counter starts to pay from here. */
    const val INTERSTITIAL_MIN_SESSIONS: Int = 2

    /** An app-open ad needs a real trip to the background, not a permission dialog or a rotation. */
    const val APP_OPEN_MIN_BACKGROUND_MS: Long = 30_000L

    /** Google expires cached app-open creatives after four hours. */
    const val APP_OPEN_MAX_AGE_MS: Long = 4 * 60 * 60 * 1000L

    /** Interstitial / rewarded caches are re-requested after an hour. */
    const val CACHED_AD_MAX_AGE_MS: Long = 60 * 60 * 1000L

    /**
     * Quiet window after a consent form, a runtime permission prompt or any other system screen:
     * coming back from one of those must never look like "the app just showed me an advert".
     */
    const val SENSITIVE_FLOW_QUIET_MS: Long = 20_000L
}
