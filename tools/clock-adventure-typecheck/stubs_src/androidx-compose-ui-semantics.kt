package androidx.compose.ui.semantics

import androidx.compose.ui.Modifier

/** Stub of the Compose semantics receiver: only the properties this project sets. */
interface SemanticsPropertyReceiver {
    var contentDescription: String
    var stateDescription: String
    var testTag: String
}

fun Modifier.semantics(
    mergeDescendants: Boolean = false,
    properties: SemanticsPropertyReceiver.() -> Unit
): Modifier = this

fun Modifier.clearAndSetSemantics(
    properties: SemanticsPropertyReceiver.() -> Unit
): Modifier = this
