package androidx.compose.ui.text
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

class TextStyle(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit(Float.NaN),
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val letterSpacing: TextUnit = TextUnit(Float.NaN),
    val textAlign: TextAlign? = null,
    val lineHeight: TextUnit = TextUnit(Float.NaN),
    val background: Color = Color.Unspecified,
    val shadow: Shadow? = null
) {
    fun copy(
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontFamily: FontFamily? = this.fontFamily,
        letterSpacing: TextUnit = this.letterSpacing,
        textAlign: TextAlign? = this.textAlign,
        lineHeight: TextUnit = this.lineHeight,
        background: Color = this.background,
        shadow: Shadow? = this.shadow
    ): TextStyle = TextStyle()
}

class Shadow(
    val color: Color = Color(0xFF000000),
    val offset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val blurRadius: Float = 0f
)
