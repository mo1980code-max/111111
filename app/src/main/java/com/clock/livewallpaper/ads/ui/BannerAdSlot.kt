package com.clock.livewallpaper.ads.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ads.AdConfig
import com.clock.livewallpaper.ads.AdLog
import com.clock.livewallpaper.ads.AdsViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * An anchored adaptive banner, sized to the column it sits in.
 *
 * Lifecycle rules of the `AdView`, which Compose does not handle by itself:
 *
 * * it is created once per composition and kept in `remember`, never rebuilt on recomposition;
 * * it is paused and resumed with the host lifecycle;
 * * it is detached from its parent and `destroy()`ed when the slot leaves the screen;
 * * it is loaded once, and only after [AdsViewModel] reports that ads are allowed - before that
 *   nothing is composed at all, so a user without consent never even sees a gap.
 *
 * The slot takes the width of its parent, so it neither widens a screen nor breaks the RTL
 * layout, and it draws nothing while the request is in the air or after it failed.
 */
@Composable
fun BannerAdSlot(
    modifier: Modifier = Modifier,
    viewModel: AdsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.adsReady) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val widthDp = maxWidth.value.toInt()
        val adView = remember { AdView(context) }
        var requested by remember { mutableStateOf(false) }
        var loaded by remember { mutableStateOf(false) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView.destroy()
            }
        }

        LaunchedEffect(widthDp) {
            if (requested || widthDp <= 0) return@LaunchedEffect
            requested = true
            adView.adUnitId = AdConfig.BANNER_UNIT_ID
            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            )
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                    AdLog.d("banner onAdLoaded size=${adView.adSize} response=${adView.responseInfo}")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loaded = false
                    AdLog.loadFailed("banner", error)
                }

                override fun onAdImpression() {
                    AdLog.d("banner onAdImpression")
                }

                override fun onAdClicked() {
                    AdLog.d("banner onAdClicked")
                }
            }
            AdLog.d("banner ad request started unit=${AdConfig.BANNER_UNIT_ID} width=${widthDp}dp")
            adView.loadAd(AdRequest.Builder().build())
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (loaded) {
                Text(
                    text = stringResource(R.string.ad_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
                )
            }
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
