package com.clockadventure.presentation.theme

import androidx.compose.ui.graphics.Color
import com.clockadventure.domain.model.AppTheme
import com.clockadventure.domain.model.ClockStyle

/**
 * The whole look of the app lives here: bright, saturated, friendly colours with strong contrasts
 * so everything stays readable for a five year old in a sunny room.
 */

object Palette {
    val SkyTop = Color(0xFF7FE7FF)
    val SkyBottom = Color(0xFFDFF6FF)
    val CandyPink = Color(0xFFFF6FA5)
    val CandyPurple = Color(0xFF9B6BFF)
    val SunYellow = Color(0xFFFFC93C)
    val Orange = Color(0xFFFF8A3D)
    val Mint = Color(0xFF3ED9A3)
    val Grass = Color(0xFF5FD35F)
    val Ocean = Color(0xFF2FA8E0)
    val DeepBlue = Color(0xFF1B2A5B)
    val Grape = Color(0xFF6A3CC4)
    val Coral = Color(0xFFFF6B6B)
    val Cream = Color(0xFFFFF6E5)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B2D5C)
    val InkSoft = Color(0xFF6A6E9E)
    val Shadow = Color(0x33000000)
    val Gold = Color(0xFFFFC93C)
    val Silver = Color(0xFFD7DEEA)
    val Locked = Color(0xFFB9C0D4)
}

/** Colours of one background theme. */
data class ThemeColors(
    val skyTop: Color,
    val skyMiddle: Color,
    val skyBottom: Color,
    val accent: Color,
    val accentDark: Color,
    val secondary: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val content: Color,
    val decorative: List<Color>
) {
    val skyColors: List<Color> get() = listOf(skyTop, skyMiddle, skyBottom)
}

fun themeColorsFor(theme: AppTheme): ThemeColors = when (theme) {
    AppTheme.SKY -> ThemeColors(
        skyTop = Color(0xFF63D2FF),
        skyMiddle = Color(0xFFA9E9FF),
        skyBottom = Color(0xFFE8F8FF),
        accent = Palette.CandyPink,
        accentDark = Color(0xFFE0407F),
        secondary = Palette.SunYellow,
        cardTop = Color(0xFFFFFFFF),
        cardBottom = Color(0xFFDCEEFF),
        content = Palette.Ink,
        decorative = listOf(Color(0xFFFFFFFF), Palette.SunYellow, Palette.Mint)
    )
    AppTheme.CANDY -> ThemeColors(
        skyTop = Color(0xFFFF9EC7),
        skyMiddle = Color(0xFFFFC4DE),
        skyBottom = Color(0xFFFFF0F6),
        accent = Palette.CandyPurple,
        accentDark = Color(0xFF7140D6),
        secondary = Palette.SunYellow,
        cardTop = Color(0xFFFFFFFF),
        cardBottom = Color(0xFFFFD9EA),
        content = Color(0xFF4A1E52),
        decorative = listOf(Color(0xFFFFFFFF), Palette.Mint, Palette.SunYellow)
    )
    AppTheme.FOREST -> ThemeColors(
        skyTop = Color(0xFF8FE388),
        skyMiddle = Color(0xFFC7F0B0),
        skyBottom = Color(0xFFF1FBDD),
        accent = Color(0xFF2E9E5B),
        accentDark = Color(0xFF1C7A42),
        secondary = Palette.Orange,
        cardTop = Color(0xFFFFFFFF),
        cardBottom = Color(0xFFD8F2C9),
        content = Color(0xFF1E4426),
        decorative = listOf(Color(0xFFFFFFFF), Palette.Grass, Palette.SunYellow)
    )
    AppTheme.SPACE -> ThemeColors(
        skyTop = Color(0xFF2B2D6B),
        skyMiddle = Color(0xFF4A3E93),
        skyBottom = Color(0xFF8E6FC0),
        accent = Color(0xFFFFD166),
        accentDark = Color(0xFFE8A917),
        secondary = Color(0xFF6FE3FF),
        cardTop = Color(0xFF5A4FA8),
        cardBottom = Color(0xFF3A3377),
        content = Color(0xFFF4F1FF),
        decorative = listOf(Color(0xFFFFFFFF), Color(0xFFFFD166), Color(0xFF6FE3FF))
    )
    AppTheme.DESERT -> ThemeColors(
        skyTop = Color(0xFFFFC46B),
        skyMiddle = Color(0xFFFFE0A8),
        skyBottom = Color(0xFFFFF6E0),
        accent = Color(0xFFE1701A),
        accentDark = Color(0xFFB4530B),
        secondary = Color(0xFF6BD0E8),
        cardTop = Color(0xFFFFFFFF),
        cardBottom = Color(0xFFFFE2B8),
        content = Color(0xFF5A3208),
        decorative = listOf(Color(0xFFFFFFFF), Color(0xFFE1701A), Color(0xFF6BD0E8))
    )
}

/** Colours of one interactive clock style. */
data class ClockPalette(
    val bezelLight: Color,
    val bezelDark: Color,
    val faceTop: Color,
    val faceBottom: Color,
    val rim: Color,
    val number: Color,
    val tick: Color,
    val tickMajor: Color,
    val hourHand: Color,
    val minuteHand: Color,
    val center: Color,
    val glass: Color,
    val handShadow: Color
)

fun clockPaletteFor(style: ClockStyle): ClockPalette = when (style) {
    ClockStyle.CANDY -> ClockPalette(
        bezelLight = Color(0xFFFF9EC7),
        bezelDark = Color(0xFFE0407F),
        faceTop = Color(0xFFFFFFFF),
        faceBottom = Color(0xFFFFE3EF),
        rim = Color(0xFFFFC93C),
        number = Color(0xFFB32E63),
        tick = Color(0xFFF2A0C0),
        tickMajor = Color(0xFFE0407F),
        hourHand = Color(0xFFFF5C8A),
        minuteHand = Color(0xFF7B4DE0),
        center = Color(0xFFFFC93C),
        glass = Color(0x33FFFFFF),
        handShadow = Color(0x33000000)
    )
    ClockStyle.OCEAN -> ClockPalette(
        bezelLight = Color(0xFF7FE7FF),
        bezelDark = Color(0xFF1E88C7),
        faceTop = Color(0xFFF4FDFF),
        faceBottom = Color(0xFFD3F1FF),
        rim = Color(0xFF2FA8E0),
        number = Color(0xFF0B5C8C),
        tick = Color(0xFF9BDCF5),
        tickMajor = Color(0xFF1E88C7),
        hourHand = Color(0xFF0E6FB8),
        minuteHand = Color(0xFF00C2A8),
        center = Color(0xFFFFD166),
        glass = Color(0x33FFFFFF),
        handShadow = Color(0x33000000)
    )
    ClockStyle.JUNGLE -> ClockPalette(
        bezelLight = Color(0xFFB6F08A),
        bezelDark = Color(0xFF3E8E2E),
        faceTop = Color(0xFFF8FFF0),
        faceBottom = Color(0xFFDFF5C9),
        rim = Color(0xFF5FD35F),
        number = Color(0xFF2D6B1F),
        tick = Color(0xFFA8DC8A),
        tickMajor = Color(0xFF3E8E2E),
        hourHand = Color(0xFF2E7D32),
        minuteHand = Color(0xFFFF8A3D),
        center = Color(0xFFFFC93C),
        glass = Color(0x33FFFFFF),
        handShadow = Color(0x33000000)
    )
    ClockStyle.SUNSET -> ClockPalette(
        bezelLight = Color(0xFFFFC16B),
        bezelDark = Color(0xFFE1701A),
        faceTop = Color(0xFFFFFBF2),
        faceBottom = Color(0xFFFFE0B8),
        rim = Color(0xFFFF8A3D),
        number = Color(0xFF8A3B00),
        tick = Color(0xFFFFC9AA),
        tickMajor = Color(0xFFE1701A),
        hourHand = Color(0xFFD94F00),
        minuteHand = Color(0xFF6A3CC4),
        center = Color(0xFFFFD166),
        glass = Color(0x33FFFFFF),
        handShadow = Color(0x33000000)
    )
    ClockStyle.SPACE -> ClockPalette(
        bezelLight = Color(0xFF6A5ACD),
        bezelDark = Color(0xFF241B5E),
        faceTop = Color(0xFF3B2F8C),
        faceBottom = Color(0xFF1B1447),
        rim = Color(0xFF8E7BFF),
        number = Color(0xFFF2EEFF),
        tick = Color(0xFF8E7BFF),
        tickMajor = Color(0xFFFFD166),
        hourHand = Color(0xFFFFD166),
        minuteHand = Color(0xFF6FE3FF),
        center = Color(0xFFFFD166),
        glass = Color(0x22FFFFFF),
        handShadow = Color(0x55000000)
    )
    ClockStyle.GALAXY -> ClockPalette(
        bezelLight = Color(0xFFFF8ED8),
        bezelDark = Color(0xFF5B21B6),
        faceTop = Color(0xFF2B1B5E),
        faceBottom = Color(0xFF120A33),
        rim = Color(0xFF00E5C0),
        number = Color(0xFFE9E2FF),
        tick = Color(0xFFB388FF),
        tickMajor = Color(0xFF00E5C0),
        hourHand = Color(0xFF00E5C0),
        minuteHand = Color(0xFFFF8ED8),
        center = Color(0xFFFFD166),
        glass = Color(0x22FFFFFF),
        handShadow = Color(0x55000000)
    )
}
