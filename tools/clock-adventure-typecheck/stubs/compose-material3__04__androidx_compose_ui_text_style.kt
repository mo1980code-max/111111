package androidx.compose.ui.text.style

enum class TextAlign { Left, Right, Center, Justify, Start, End }

class TextDecoration {
    companion object {
        val None: TextDecoration = TextDecoration()
        val Underline: TextDecoration = TextDecoration()
        val LineThrough: TextDecoration = TextDecoration()
    }
}

enum class TextOverflow { Clip, Ellipsis, Visible }
