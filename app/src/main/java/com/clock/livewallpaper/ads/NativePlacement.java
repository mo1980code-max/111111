package com.clock.livewallpaper.ads;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clock.livewallpaper.R;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.ArrayList;
import java.util.List;

/**
 * One in-feed native ad, reused by every row of one list.
 *
 * <p>Rendering follows the Google "native ads (advanced)" guide: a {@link NativeAdView} is the root
 * of the card, every asset view is registered on it and the {@link NativeAd} is attached last. The
 * ad badge ("إعلان") is always visible, so the card can never be mistaken for a wallpaper or clock
 * tile. The call to action falls back to "Install" when the ad omits it, which is what the guide prescribes.
 *
 * <p>Load failures are silent and leave no gap: {@link #renderInto} removes the row's content and the
 * row collapses to zero height, so the grid keeps flowing offline exactly as it does with no ads at
 * all.
 */
public final class NativePlacement {

    private Runnable datasetChanged;
    private final List<NativeAdView> renderedViews = new ArrayList<>();

    @Nullable
    private volatile NativeAd ad;
    private volatile long loadedAt;
    private volatile boolean requesting;

    /** Fresh placement with nothing cached yet. */
    public NativePlacement() {
        this(null);
    }

    /**
     * @param datasetChanged invoked on the main thread when an ad becomes available so the host
     *                       adapter can rebind its placeholder rows.
     */
    public NativePlacement(@Nullable Runnable datasetChanged) {
        this.datasetChanged = datasetChanged;
    }

    /** Lets a list adapter be notified when a previously empty slot becomes fillable. */
    public void setDatasetChangedListener(Runnable listener) {
        this.datasetChanged = listener;
    }

    public boolean hasAd() {
        NativeAd local = ad;
        return local != null && System.currentTimeMillis() - loadedAt <= AdConfig.NATIVE_AD_TTL_MS;
    }

    /** Requests an ad unless one is fresh, allowed by policy, or already in flight. */
    public void prepare(@NonNull final Context context) {
        if (hasAd() || requesting || !AdPolicy.nativeAdsAllowed()) {
            return;
        }
        requesting = true;
        AdsManager.get().loadNativeAd(context, new AdsManager.NativeAdConsumer() {
            @Override
            public void onLoaded(@NonNull NativeAd nativeAd) {
                requesting = false;
                NativeAd previous = ad;
                ad = nativeAd;
                loadedAt = System.currentTimeMillis();
                if (previous != null && previous != nativeAd) {
                    previous.destroy();
                }
                if (datasetChanged != null) {
                    datasetChanged.run();
                }
            }

            @Override
            public void onFailed() {
                requesting = false;
                ad = null;
            }
        });
    }

    /**
     * Fills (or empties) a slot. Called from {@code onBindViewHolder}, so recycled rows refresh
     * themselves without any timer of their own. An empty slot collapses instead of leaving a blank
     * strip between cards.
     */
    public void renderInto(@NonNull FrameLayout container) {
        container.removeAllViews();
        NativeAd current = ad;
        if (current == null || System.currentTimeMillis() - loadedAt > AdConfig.NATIVE_AD_TTL_MS) {
            container.setVisibility(View.GONE);
            prepare(container.getContext());
            return;
        }
        NativeAdView view = (NativeAdView) LayoutInflater.from(container.getContext())
                .inflate(R.layout.ad_native, container, false);
        bind(view, current);
        container.addView(view);
        container.setVisibility(View.VISIBLE);
        renderedViews.add(view);
    }

    /** Detaches a rendered card when its row is recycled; the ad object itself stays cached. */
    public void release(@NonNull FrameLayout container) {
        for (int i = renderedViews.size() - 1; i >= 0; i--) {
            NativeAdView view = renderedViews.get(i);
            if (view.getParent() == container) {
                renderedViews.remove(i);
                break;
            }
        }
        container.removeAllViews();
    }

    /** Wires every asset of the ad onto its {@link NativeAdView}. */
    private void bind(@NonNull NativeAdView view, @NonNull NativeAd nativeAd) {
        TextView headline = view.findViewById(R.id.adHeadline);
        TextView body = view.findViewById(R.id.adBody);
        TextView advertiser = view.findViewById(R.id.adAdvertiser);
        TextView callToAction = view.findViewById(R.id.adCallToAction);
        ImageView icon = view.findViewById(R.id.adIcon);
        MediaView media = view.findViewById(R.id.adMedia);

        setText(headline, nativeAd.getHeadline());
        setText(body, nativeAd.getBody());
        setText(advertiser, nativeAd.getAdvertiser());
        String cta = nativeAd.getCallToAction();
        callToAction.setText(TextUtils.isEmpty(cta) ? "Install" : cta);

        // The icon is required by policy when the ad provides one. It is drawn straight from the ad
        // object (a Drawable, or the small content:// Uri the SDK hands out), so no image loader and no
        // network stack is involved in painting an ad asset.
        NativeAd.Image iconAsset = nativeAd.getIcon();
        boolean iconShown = false;
        if (iconAsset != null) {
            if (iconAsset.getDrawable() != null) {
                icon.setImageDrawable(iconAsset.getDrawable());
                iconShown = true;
            } else if (iconAsset.getUri() != null) {
                try {
                    icon.setImageURI(iconAsset.getUri());
                    iconShown = icon.getDrawable() != null;
                } catch (Exception ignored) {
                    iconShown = false;
                }
            }
        }
        icon.setVisibility(iconShown ? View.VISIBLE : View.GONE);
        view.setIconView(iconShown ? icon : null);

        MediaContent content = nativeAd.getMediaContent();
        if (content != null) {
            media.setVisibility(View.VISIBLE);
            media.setMediaContent(content);
            view.setMediaView(media);
        } else {
            media.setVisibility(View.GONE);
        }

        view.setHeadlineView(headline);
        view.setBodyView(body);
        view.setAdvertiserView(advertiser);
        view.setCallToActionView(callToAction);
        view.setNativeAd(nativeAd);
    }

    private void setText(@Nullable TextView view, @Nullable String value) {
        if (view == null) {
            return;
        }
        if (TextUtils.isEmpty(value)) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(value);
        }
    }

    /** Call from {@code Activity.onDestroy}: releases the views and the cached ad. */
    public void destroy() {
        for (NativeAdView view : new ArrayList<>(renderedViews)) {
            view.removeAllViews();
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }
        renderedViews.clear();
        NativeAd local = ad;
        ad = null;
        if (local != null) {
            local.destroy();
        }
    }
}
