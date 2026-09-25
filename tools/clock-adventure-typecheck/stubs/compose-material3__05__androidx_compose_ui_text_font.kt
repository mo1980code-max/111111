package androidx.compose.ui.text.font

import androidx.compose.ui.text.font.FontWeight

class FontFamily

fun FontFamily(vararg fonts: Any): FontFamily = FontFamily()

class FontStyle {
    companion object {
        val Normal: FontStyle = FontStyle()
        val Italic: FontStyle = FontStyle()
    }
}

class FontWeight(val weight: Int) {
    companion object {
        val Thin = FontWeight(100)
        val ExtraLight = FontWeight(200)
        val Light = FontWeight(300)
        val Normal = FontWeight(400)
        val Medium = FontWeight(500)
        val SemiBold = FontWeight(600)
        val Bold = FontWeight(700)
        val ExtraBold = FontWeight(800)
        val Black = FontWeight(900)
        val W100 = Thin
        val W200 = ExtraLight
        val W300 = Light
        val W400 = Normal
        val W500 = Medium
        val W600 = SemiBold
        val W700 = Bold
        val W800 = ExtraBold
        val W900 = Black
    }
}

class Font

fun Font(resId: Int, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font = Font()
