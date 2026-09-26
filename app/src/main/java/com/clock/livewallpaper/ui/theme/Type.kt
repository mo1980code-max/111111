package com.clock.livewallpaper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.clock.livewallpaper.R

/**
 * Three bundled, offline, SIL OFL Arabic faces - no remote font is ever fetched:
 *
 *  - [DhikrDisplayFont]  Tajawal   headings, numerals, chips
 *  - [DhikrUiFont]       Cairo     body copy and controls
 *  - [DhikrScriptFont]   Amiri Quran - the dhikr text itself: it carries full diacritics and
 *                        Quranic marks, which is exactly what the verified content contains.
 *
 * Arabic needs more leading than Latin, so every style pairs a generous lineHeight with the size.
 */
val DhikrDisplayFont = FontFamily(Font(R.font.tajawal_regular, FontWeight.Normal))
val DhikrUiFont = FontFamily(Font(R.font.cairo_regular, FontWeight.Normal))
val DhikrScriptFont = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

val DhikrTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DhikrDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DhikrDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DhikrDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DhikrDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DhikrDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DhikrUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 18.sp
    )
)

/** The dhikr body style: the most important text in the product, so it gets the most air. */
fun dhikrBodyStyle(fontScale: Float = 1f, centered: Boolean = true): TextStyle = TextStyle(
    fontFamily = DhikrScriptFont,
    fontWeight = FontWeight.Normal,
    fontSize = (21 * fontScale).sp,
    lineHeight = (40 * fontScale).sp,
    textAlign = if (centered) TextAlign.Center else TextAlign.Start
)
