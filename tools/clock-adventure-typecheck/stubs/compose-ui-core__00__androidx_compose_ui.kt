package androidx.compose.ui

interface Modifier {
    infix fun then(other: Modifier): Modifier = TODO()

    companion object : Modifier
}
