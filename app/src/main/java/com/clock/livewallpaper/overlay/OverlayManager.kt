package com.clock.livewallpaper.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.clock.livewallpaper.core.Vibrations
import com.clock.livewallpaper.data.prefs.OverlayPositionOption
import com.clock.livewallpaper.data.prefs.OverlaySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Owns the floating dhikr card: one window at a time, added to and removed from WindowManager with
 * the application context.
 *
 * Product contract enforced here:
 *  - touching the card dismisses it and does NOTHING else - no Activity is started, no task is
 *    switched, the app the user is in stays exactly where it was;
 *  - a second show() while a card is up replaces it instead of stacking a second window;
 *  - every WindowManager call is guarded, so a revoked permission, a dead window token or a
 *    double dismiss can never crash the process;
 *  - the pending auto-dismiss callback is cancelled as soon as the user dismisses manually.
 *
 * No Activity, no Service and no foreground notification are needed for any of this.
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var session: Session? = null

    private class Session(
        val view: ComposeView,
        val owner: OverlayViewOwner,
        val visible: MutableState<Boolean>,
        var dismissing: Boolean = false
    )

    private val autoDismissRunnable = Runnable { hideDhikr() }
    private val removeRunnable = Runnable { removeImmediately() }

    fun isShowing(): Boolean = session != null

    /** Shows the card from any thread; returns true when the window was actually added. */
    suspend fun showDhikrAsync(text: String, settings: OverlaySettings): Boolean =
        withContext(Dispatchers.Main.immediate) { showDhikr(text, settings) }

    @MainThread
    fun showDhikr(text: String, settings: OverlaySettings): Boolean {
        if (text.isBlank()) return false
        if (!OverlayPermission.canDraw(context)) return false
        val windowManager = windowManager() ?: return false

        // Never two cards at once, and never an old auto-dismiss firing on a new card.
        removeImmediately()

        val owner = OverlayViewOwner().apply { attach() }
        val visible = mutableStateOf(false)
        val view = ComposeView(context)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                OverlayHost(
                    text = text,
                    settings = settings,
                    visible = visible.value,
                    onDismiss = { hideDhikr() }
                )
            }
        }

        return try {
            windowManager.addView(view, buildLayoutParams(settings))
            session = Session(view, owner, visible)
            owner.resume()
            visible.value = true
            if (settings.haptic) Vibrations.tick(context)
            if (settings.autoDismissSeconds > 0) {
                mainHandler.postDelayed(
                    autoDismissRunnable,
                    settings.autoDismissSeconds * 1000L
                )
            }
            true
        } catch (error: Throwable) {
            // BadTokenException (permission revoked), IllegalArgumentException, SecurityException,
            // OEM specific failures: the reminder simply falls back to a notification.
            runCatching { owner.destroy() }
            session = null
            false
        }
    }

    /** Fades the card out and removes the window. Safe to call twice, or with nothing showing. */
    fun hideDhikr() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { hideDhikr() }
            return
        }
        val current = session ?: return
        if (current.dismissing) return
        current.dismissing = true
        mainHandler.removeCallbacks(autoDismissRunnable)
        current.visible.value = false
        mainHandler.postDelayed(removeRunnable, EXIT_ANIMATION_MS)
    }

    @MainThread
    private fun removeImmediately() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.removeCallbacks(removeRunnable)
        val current = session ?: return
        session = null
        val manager = windowManager()
        runCatching {
            if (current.view.isAttachedToWindow) {
                manager?.removeViewImmediate(current.view)
            }
        }
        runCatching { current.view.disposeComposition() }
        runCatching { current.owner.destroy() }
    }

    private fun windowManager(): WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private fun buildLayoutParams(settings: OverlaySettings): WindowManager.LayoutParams {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val width = min(
            (metrics.widthPixels * WIDTH_FRACTION).toInt(),
            (MAX_WIDTH_DP * density).toInt()
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            // NOT_FOCUSABLE keeps the app underneath focused (its keyboard stays open);
            // the card is still touchable, which is what dismisses it.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = when (settings.position) {
            OverlayPositionOption.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            OverlayPositionOption.CENTER -> Gravity.CENTER
            OverlayPositionOption.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        params.y = when (settings.position) {
            OverlayPositionOption.TOP -> (TOP_MARGIN_DP * density).toInt()
            OverlayPositionOption.CENTER -> 0
            OverlayPositionOption.BOTTOM -> (BOTTOM_MARGIN_DP * density).toInt()
        }
        // The system animates nothing for us; the card animates itself in Compose.
        params.windowAnimations = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // DEFAULT keeps the window out of a display cutout instead of under it.
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        return params
    }

    companion object {
        private const val WIDTH_FRACTION = 0.90f
        private const val MAX_WIDTH_DP = 520
        private const val TOP_MARGIN_DP = 28
        private const val BOTTOM_MARGIN_DP = 40

        /** Must stay slightly longer than the exit animation below. */
        const val EXIT_ANIMATION_MS = 170L
    }
}

/**
 * Entry / exit animation wrapper: fade + a small slide and a 0.96 -> 1 scale on the way in, a quick
 * fade and shrink on the way out. Restrained on purpose - this appears over someone else's app.
 */
@Composable
private fun OverlayHost(
    text: String,
    settings: OverlaySettings,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 220 else 140),
        label = "overlayAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = tween(durationMillis = if (visible) 240 else 140),
        label = "overlayScale"
    )
    val slide by animateFloatAsState(
        targetValue = if (visible) 0f else -14f,
        animationSpec = tween(durationMillis = 240),
        label = "overlaySlide"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationY = slide * density
            }
    ) {
        DhikrOverlayCard(
            text = text,
            settings = settings,
            onDismiss = onDismiss
        )
    }
}
