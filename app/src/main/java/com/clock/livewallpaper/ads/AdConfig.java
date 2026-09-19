package com.clock.livewallpaper.ads;

/**
 * The only place where monetisation is configured.
 *
 * <p>Ad unit IDs are Google's official <b>demo</b> IDs, so nothing in this build can generate real
 * impressions by accident. Swap the three constants below for the IDs created in the AdMob console
 * before release, keep {@link #ADMOB_APP_ID} in sync with the
 * {@code com.google.android.gms.ads.APPLICATION_ID} meta-data in AndroidManifest.xml, and keep
 * testing on a registered test device.
 *
 * <p>Policy notes (reviewed against the AdMob documentation at the time of writing):
 * <ul>
 *   <li>Native ads are rendered with the official {@code NativeAdView}: headline, body, advertiser,
 *       icon, media and call to action are all registered with the SDK, the ad badge is
 *       always visible and no app content overlaps the ad view.</li>
 *   <li>A loaded native ad is reused until it expires (60 minutes) and never re-requested more than
 *       once a minute, so scrolling back and forth does not spam requests.</li>
 *   <li>App open ads are only shown on a warm foreground return (never on a cold start, never on
 *       top of another ad, never inside the Quran section, never right after a rewarded ad) and are
 *       dropped once older than four hours.</li>
 *   <li>Rewarded ads are strictly opt-in: they are never shown automatically, only after the user
 *       confirms the "watch an ad to unlock" dialog.</li>
 *   <li>Interstitial ads are not used anywhere in this app: they interrupted Quran and Azkar reading
 *       in the legacy build, which is exactly what the content policy for this app forbids.</li>
 * </ul>
 */
public final class AdConfig {

    private AdConfig() {
    }

    /** Master switch: set to false to ship a completely ad-free build. */
    public static final boolean ADS_ENABLED = true;

    // --- Google demo (test) ad unit IDs -------------------------------------------------------
    public static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";

    // --- Native ads in the wallpaper / clock / home lists -------------------------------------
    /**
     * How many content rows sit between two native ads. Change this one number to make native ads
     * rarer or more frequent; the recommendation from Google is "not after every item", and the app
     * uses 8 (the middle of the 6..10 range agreed for this app).
     */
    public static final int NATIVE_AD_INTERVAL = 8;
    /** Lists shorter than this get no native ad at all (a 6 wallpaper section stays ad free). */
    public static final int NATIVE_AD_MIN_ITEMS = 10;
    /** Minimum spacing between two requests for the same placement (guidance: 60s or more). */
    public static final long NATIVE_AD_REQUEST_COOLDOWN_MS = 60_000L;
    /** A loaded native ad is stale after this long and must be reloaded. */
    public static final long NATIVE_AD_TTL_MS = 60L * 60_000L;

    // --- App open ads -------------------------------------------------------------------------
    /** Never show two app open ads closer together than this. */
    public static final long APP_OPEN_MIN_INTERVAL_MS = 5L * 60_000L;
    /** Silence after a rewarded ad finished, so one ad never stacks on another. */
    public static final long APP_OPEN_AFTER_REWARDED_MS = 60_000L;
    /** Grace period after leaving the Quran section; no ad pops up on the way out. */
    public static final long APP_OPEN_AFTER_QURAN_MS = 20_000L;
    /** Google: discard an app open ad loaded more than four hours ago. */
    public static final long APP_OPEN_EXPIRY_MS = 4L * 60L * 60_000L;
    /** Best practice: let a new user reach the content a few times before the first app open ad. */
    public static final int APP_OPEN_LAUNCHES_BEFORE_FIRST_AD = 3;

    // --- Free content -------------------------------------------------------------------------
    /** First N clocks of every clock section are free forever, no ad required. */
    public static final int FREE_CLOCKS_PER_SECTION = 9;
    /** First N wallpapers of every wallpaper section are free forever, no ad required. */
    public static final int FREE_WALLPAPERS_PER_SECTION = 3;
}
