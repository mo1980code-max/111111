package androidx.lifecycle.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    initialValue: T? = null
): State<T> = collectAsState(initialValue ?: value)
