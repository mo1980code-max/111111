package com.clockadventure.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clockadventure.domain.model.AppTheme

/** The background theme currently selected in Settings. */
val LocalThemeColors = staticCompositionLocalOf { themeColorsFor(AppTheme.SKY) }

/** True when the child (or parent) asked for less animation. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** True while the UI must mirror itself for Arabic. */
val LocalRtl = staticCompositionLocalOf { false }

object Dimens {
    val screenPadding = 20.dp
    val cardPadding = 16.dp
    val gapSmall = 8.dp
    val gapMedium = 12.dp
    val gapLarge = 20.dp
    val cornerLarge = 28.dp
    val cornerMedium = 20.dp
    val cornerSmall = 14.dp
    /** Large touch targets: the minimum height of every interactive element. */
    val minTouchTarget = 64.dp
    val buttonHeight = 72.dp
    val clockSize = 300.dp
    val hudHeight = 56.dp
}

val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.cornerSmall),
    small = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.cornerMedium),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.cornerLarge),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
)

/**
 * The app stays bright in every situation: a dark wallpaper must never dim a screen made for
 * children, so only the (unused in practice) dark scheme follows the system.
 */
@Composable
fun ClockAdventureTheme(
    theme: AppTheme = AppTheme.SKY,
    reduceMotion: Boolean = false,
    rtl: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = themeColorsFor(theme)

    val lightColors = lightColorScheme(
        primary = colors.accent,
        onPrimary = Color.White,
        primaryContainer = colors.cardTop,
        onPrimaryContainer = colors.content,
        secondary = colors.secondary,
        onSecondary = Color(0xFF2B2D5C),
        secondaryContainer = colors.cardBottom,
        onSecondaryContainer = colors.content,
        background = colors.skyBottom,
        onBackground = colors.content,
        surface = colors.cardTop,
        onSurface = colors.content,
        surfaceVariant = colors.cardBottom,
        onSurfaceVariant = colors.content,
        error = Palette.Coral,
        onError = Color.White
    )

    val darkColors = darkColorScheme(
        primary = colors.accent,
        onPrimary = Color.White,
        secondary = colors.secondary,
        background = colors.skyTop,
        surface = colors.cardTop,
        onSurface = colors.content
    )

    CompositionLocalProvider(
        LocalThemeColors provides colors,
        LocalReduceMotion provides reduceMotion,
        LocalRtl provides rtl
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors else lightColors,
            shapes = AppShapes,
            typography = ClockAdventureTypography,
            content = content
        )
    }
}

/** Shortcut used by every screen to read the active background theme. */
val appColors: ThemeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeColors.current
