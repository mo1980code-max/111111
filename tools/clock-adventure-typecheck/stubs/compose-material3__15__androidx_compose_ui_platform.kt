package androidx.compose.ui.platform

import androidx.compose.ui.Modifier

fun Modifier.testTag(tag: String): Modifier = this.then(Modifier)
