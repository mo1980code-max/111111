package com.clock.livewallpaper.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.clock.livewallpaper.data.prefs.OverlayStyleOption

/**
 * The four floating-card palettes. The real overlay and the live preview in
 * "مظهر بطاقة الذكر" both resolve their colours here, so what the user previews is exactly what
 * appears above other apps.
 */
@Immutable
data class OverlayPalette(
    val surface: Color,
    val innerTint: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val border: Color,
    val shadow: Color
)

fun overlayPalette(style: OverlayStyleOption): OverlayPalette = when (style) {
    // نور - warm ivory, deep emerald text, muted gold detail
    OverlayStyleOption.NOOR -> OverlayPalette(
        surface = Color(0xFFFBF6EC),
        innerTint = Color(0xFFF2E9D8),
        text = Color(0xFF0B4634),
        muted = Color(0xFF6C7C71),
        accent = Color(0xFFB08D51),
        border = Color(0x33B08D51),
        shadow = Color(0x33101A16)
    )
    // زمرد - deep emerald, warm ivory text, muted gold
    OverlayStyleOption.EMERALD -> OverlayPalette(
        surface = Color(0xFF0A3D2F),
        innerTint = Color(0xFF0E4A39),
        text = Color(0xFFF6F1E4),
        muted = Color(0xFFB4C7BC),
        accent = Color(0xFFC6A464),
        border = Color(0x3DC6A464),
        shadow = Color(0x4D05130E)
    )
    // ليل - charcoal with an emerald undertone, warm off-white text
    OverlayStyleOption.NIGHT -> OverlayPalette(
        surface = Color(0xFF14211D),
        innerTint = Color(0xFF1B2C26),
        text = Color(0xFFF1EEE5),
        muted = Color(0xFF9DAFA6),
        accent = Color(0xFFC9A96A),
        border = Color(0x33C9A96A),
        shadow = Color(0x59040B08)
    )
    // صفاء - soft translucency, kept readable with a firm border and high-contrast ink
    OverlayStyleOption.CLEAR -> OverlayPalette(
        surface = Color(0xD6FCF9F3),
        innerTint = Color(0x40B08D51),
        text = Color(0xFF12241D),
        muted = Color(0xFF4F5F57),
        accent = Color(0xFFA8873F),
        border = Color(0x3D12241D),
        shadow = Color(0x26101A16)
    )
}
