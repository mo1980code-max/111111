package com.clock.livewallpaper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.clock.livewallpaper.data.prefs.ThemeMode

/** Abstract hero artwork stops; drawn with Compose gradients, never a bitmap. */
@Immutable
data class HeroPalette(
    val top: Color,
    val bottom: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color
)

/** Product colours Material 3 has no slot for. */
@Immutable
data class DhikrExtraColors(
    val gold: Color,
    val goldSoft: Color,
    val hairline: Color,
    val positive: Color,
    val morningHero: HeroPalette,
    val eveningHero: HeroPalette
)

private val LightExtras = DhikrExtraColors(
    gold = GoldAccent,
    goldSoft = GoldSoft,
    hairline = HairlineLight,
    positive = EmeraldPrimary,
    morningHero = HeroPalette(
        top = MorningGlowTop,
        bottom = MorningGlowBottom,
        ink = CharcoalInk,
        muted = OliveMuted,
        accent = GoldAccent
    ),
    eveningHero = HeroPalette(
        top = EveningGlowTop,
        bottom = EveningGlowBottom,
        ink = Color(0xFFF3EFE4),
        muted = Color(0xFFB6C7BD),
        accent = GoldSoft
    )
)

private val DarkExtras = DhikrExtraColors(
    gold = NightGold,
    goldSoft = NightGoldSoft,
    hairline = HairlineDark,
    positive = SoftEmerald,
    morningHero = HeroPalette(
        top = NightMorningGlowTop,
        bottom = NightMorningGlowBottom,
        ink = WarmOffWhite,
        muted = NightMuted,
        accent = NightGold
    ),
    eveningHero = HeroPalette(
        top = NightEveningGlowTop,
        bottom = NightEveningGlowBottom,
        ink = WarmOffWhite,
        muted = NightMuted,
        accent = NightGold
    )
)

val LocalDhikrColors = staticCompositionLocalOf { LightExtras }

private val LightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOn,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldPrimaryDeep,
    secondary = SageSecondary,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = EmeraldPrimaryDeep,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = Color(0xFF4A3A18),
    background = IvoryBackground,
    onBackground = CharcoalInk,
    surface = CreamSurface,
    onSurface = CharcoalInk,
    surfaceVariant = SandSurface,
    onSurfaceVariant = OliveMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CreamSurface,
    surfaceContainer = Color(0xFFF7F1E6),
    surfaceContainerHigh = SandSurface,
    surfaceContainerHighest = SandSurfaceHigh,
    surfaceTint = EmeraldPrimary,
    inverseSurface = Color(0xFF1C2B25),
    inverseOnSurface = Color(0xFFF2EDE3),
    outline = Color(0xFFB9AE99),
    outlineVariant = HairlineLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFF6DEDA),
    onErrorContainer = Color(0xFF4A1A17),
    scrim = Color(0xFF0A1410)
)

private val DarkScheme = darkColorScheme(
    primary = SoftEmerald,
    onPrimary = Color(0xFF06251C),
    primaryContainer = NightEmeraldContainer,
    onPrimaryContainer = Color(0xFFCDEADC),
    secondary = NightSage,
    onSecondary = Color(0xFF0B1B15),
    secondaryContainer = Color(0xFF223229),
    onSecondaryContainer = Color(0xFFD5E2D8),
    tertiary = NightGold,
    onTertiary = Color(0xFF241A08),
    tertiaryContainer = NightGoldSoft,
    onTertiaryContainer = Color(0xFFF0DCB4),
    background = NightBackground,
    onBackground = WarmOffWhite,
    surface = NightSurface,
    onSurface = WarmOffWhite,
    surfaceVariant = NightSurfaceAlt,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF0B1512),
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceAlt,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF284439),
    surfaceTint = SoftEmerald,
    inverseSurface = Color(0xFFE9E5DB),
    inverseOnSurface = Color(0xFF17241F),
    outline = Color(0xFF63776D),
    outlineVariant = HairlineDark,
    error = ErrorDark,
    onError = Color(0xFF3A100D),
    errorContainer = Color(0xFF5A211D),
    onErrorContainer = Color(0xFFF6DEDA),
    scrim = Color(0xFF05100C)
)

/**
 * Applies the palette, the Arabic type scale and true RTL.
 *
 * Layout direction is forced to RTL for the whole tree: the product is Arabic-first, so the UI must
 * read right-to-left even if the device language is not Arabic. Numerals stay LTR inside their own
 * runs, which is the correct bidi behaviour.
 */
@Composable
fun DhikrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalDhikrColors provides if (dark) DarkExtras else LightExtras,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = DhikrTypography,
            shapes = DhikrShapes,
            content = content
        )
    }
}

/** True when the resolved theme is the dark one - used for system-bar icon contrast. */
@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}
