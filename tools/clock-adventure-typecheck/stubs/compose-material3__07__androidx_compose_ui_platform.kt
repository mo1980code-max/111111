package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val LocalDensity: androidx.compose.runtime.ProvidableCompositionLocal<Density> = compositionLocalStub()
val LocalConfiguration: androidx.compose.runtime.ProvidableCompositionLocal<android.content.res.Configuration> = compositionLocalStub()
val LocalContext: androidx.compose.runtime.ProvidableCompositionLocal<android.content.Context> = compositionLocalStub()
val LocalView: androidx.compose.runtime.ProvidableCompositionLocal<android.view.View> = compositionLocalStub()
val LocalLayoutDirection: androidx.compose.runtime.ProvidableCompositionLocal<LayoutDirection> = compositionLocalStub()

private fun <T> compositionLocalStub(): androidx.compose.runtime.ProvidableCompositionLocal<T> =
    androidx.compose.runtime.staticCompositionLocalOf { TODO() }
