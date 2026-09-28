package com.clock.livewallpaper.ads.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ads.AdsViewModel
import com.clock.livewallpaper.ui.components.DhikrCard
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** The colours the native layout borrows from the app theme, already converted for Views. */
private data class NativeAdPalette(
    val headline: Int,
    val body: Int,
    val label: Int,
    val labelBackground: Int,
    val action: Int,
    val onAction: Int
)

/** The asset views of one native layout, kept together so binding never searches the tree. */
private class NativeAdViews(
    val badge: TextView,
    val icon: ImageView,
    val headline: TextView,
    val advertiser: TextView,
    val store: TextView,
    val media: MediaView,
    val body: TextView,
    val action: Button
) {
    /** What this layout currently shows, so a recomposition does not re-register the same ad. */
    var boundAd: NativeAd? = null
    var boundPalette: NativeAdPalette? = null
}

/**
 * A native advert dressed as one of the app's cards - same radius, same hairline, same rhythm -
 * and marked with the "إعلان" badge so it can never be mistaken for the app's own content.
 *
 * Ownership is explicit, because a leaked `NativeAd` is the classic memory bug of this format:
 * [AdsViewModel] holds the one ad of the screen and destroys it when the screen is gone, the
 * `NativeAdView` is released with the composition, and a second ad would destroy the first.
 * Nothing is composed until consent and initialisation are done, and nothing is composed while
 * the request is in the air, so the layout never reserves empty space for an ad that may not come.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    viewModel: AdsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nativeAd by viewModel.nativeAd.collectAsStateWithLifecycle()

    LaunchedEffect(state.adsReady) {
        if (state.adsReady) viewModel.loadNativeAd()
    }

    val ad = nativeAd ?: return
    val label = stringResource(R.string.ad_label)
    val palette = NativeAdPalette(
        headline = MaterialTheme.colorScheme.onSurface.toArgb(),
        body = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        label = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
        labelBackground = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
        action = MaterialTheme.colorScheme.primary.toArgb(),
        onAction = MaterialTheme.colorScheme.onPrimary.toArgb()
    )

    DhikrCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(16.dp)
    ) {
        AndroidView(
            factory = { context -> buildNativeAdView(context, label) },
            modifier = Modifier.fillMaxWidth(),
            onRelease = { view -> view.destroy() },
            update = { view -> bindNativeAd(view, ad, palette) }
        )
    }
}

private fun dpToPx(context: Context, value: Int): Int =
    (value * context.resources.displayMetrics.density).toInt()

/**
 * Builds the layout once. Every asset is registered on the `NativeAdView` here, so binding later
 * is a matter of filling in text and images - the SDK needs those registrations to report
 * impressions and to handle clicks itself.
 */
private fun buildNativeAdView(context: Context, label: String): NativeAdView {
    val adView = NativeAdView(context)
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val badge = TextView(context).apply {
        text = label
        textSize = 10f
        setTypeface(null, Typeface.BOLD)
        setPadding(dpToPx(context, 8), dpToPx(context, 2), dpToPx(context, 8), dpToPx(context, 2))
        background = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 6).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    root.addView(badge)

    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dpToPx(context, 12) }
    }
    val icon = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(context, 42),
            dpToPx(context, 42)
        ).apply { marginEnd = dpToPx(context, 10) }
    }
    val titles = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
    val headline = TextView(context).apply {
        textSize = 15f
        maxLines = 2
        setTypeface(null, Typeface.BOLD)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
    val advertiser = TextView(context).apply {
        textSize = 12f
        maxLines = 1
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
    val store = TextView(context).apply {
        textSize = 12f
        maxLines = 1
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
    titles.addView(headline)
    titles.addView(advertiser)
    titles.addView(store)
    header.addView(icon)
    header.addView(titles)
    root.addView(header)

    val media = MediaView(context).apply {
        setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 150)
        ).apply { topMargin = dpToPx(context, 12) }
    }
    root.addView(media)

    val body = TextView(context).apply {
        textSize = 13f
        maxLines = 3
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dpToPx(context, 10) }
    }
    root.addView(body)

    val action = Button(context).apply {
        textSize = 14f
        isAllCaps = false
        gravity = Gravity.CENTER
        minHeight = dpToPx(context, 46)
        background = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 18).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dpToPx(context, 14) }
    }
    root.addView(action)

    adView.addView(root)
    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = action
    adView.iconView = icon
    adView.advertiserView = advertiser
    adView.storeView = store
    adView.mediaView = media
    adView.tag = NativeAdViews(badge, icon, headline, advertiser, store, media, body, action)
    return adView
}

/** Fills the layout with one ad. Assets an ad does not carry are hidden, never invented. */
private fun bindNativeAd(adView: NativeAdView, ad: NativeAd, palette: NativeAdPalette) {
    val views = adView.tag as NativeAdViews
    if (views.boundAd === ad && views.boundPalette == palette) return
    views.boundAd = ad
    views.boundPalette = palette

    views.badge.setTextColor(palette.label)
    (views.badge.background as GradientDrawable).setColor(palette.labelBackground)

    views.headline.text = ad.headline
    views.headline.setTextColor(palette.headline)

    // Each line shows the one asset it is registered for, so the SDK can attribute it correctly.
    views.advertiser.text = ad.advertiser
    views.advertiser.setTextColor(palette.body)
    views.advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE

    views.store.text = ad.store
    views.store.setTextColor(palette.body)
    views.store.visibility = if (ad.store.isNullOrBlank()) View.GONE else View.VISIBLE

    val icon = ad.icon
    views.icon.setImageDrawable(icon?.drawable)
    views.icon.visibility = if (icon?.drawable == null) View.GONE else View.VISIBLE

    val mediaContent = ad.mediaContent
    views.media.mediaContent = mediaContent
    views.media.visibility = if (mediaContent == null) View.GONE else View.VISIBLE

    views.body.text = ad.body
    views.body.setTextColor(palette.body)
    views.body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE

    val callToAction = ad.callToAction
    views.action.text = callToAction
    views.action.setTextColor(palette.onAction)
    (views.action.background as GradientDrawable).setColor(palette.action)
    views.action.visibility = if (callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    adView.setNativeAd(ad)
}
