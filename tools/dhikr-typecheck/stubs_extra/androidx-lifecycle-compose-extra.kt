package androidx.lifecycle.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

private object StubLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle = Lifecycle()
}

val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner> =
    compositionLocalOf { StubLifecycleOwner }
