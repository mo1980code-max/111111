package androidx.compose.runtime

import kotlinx.coroutines.CoroutineScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class Composable

interface State<out T> {
    val value: T
}

interface MutableState<T> : State<T> {
    override var value: T
}

fun <T> mutableStateOf(value: T): MutableState<T> = TODO()
fun mutableLongStateOf(value: Long): MutableState<Long> = TODO()
fun mutableIntStateOf(value: Int): MutableState<Int> = TODO()
fun mutableFloatStateOf(value: Float): MutableState<Float> = TODO()

inline operator fun <T> State<T>.getValue(thisObj: Any?, property: kotlin.reflect.KProperty<*>): T = value
inline operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
    this.value = value
}

fun <T> remember(calculation: @DisallowComposableCalls () -> T): T = TODO()
fun <T, V1> remember(v1: V1, calculation: @DisallowComposableCalls () -> T): T = TODO()
fun <T, V1, V2> remember(v1: V1, v2: V2, calculation: @DisallowComposableCalls () -> T): T = TODO()
fun <T, V1, V2, V3> remember(v1: V1, v2: V2, v3: V3, calculation: @DisallowComposableCalls () -> T): T = TODO()
fun <T> remember(vararg keys: Any?, calculation: @DisallowComposableCalls () -> T): T = TODO()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class DisallowComposableCalls

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ReadOnlyComposable

@Composable
fun <T> rememberUpdatedState(newValue: T): State<T> = TODO()

fun <T> derivedStateOf(calculation: () -> T): State<T> = TODO()

@Composable
fun LaunchedEffect(key1: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = Unit

@Composable
fun LaunchedEffect(key1: Any?, key2: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = Unit

@Composable
fun LaunchedEffect(vararg keys: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = Unit

@Composable
fun DisposableEffect(key1: Any?, effect: DisposableEffectScope.() -> DisposableEffectResult) = Unit

@Composable
fun DisposableEffect(vararg keys: Any?, effect: DisposableEffectScope.() -> DisposableEffectResult) = Unit

class DisposableEffectScope {
    fun onDispose(onDisposeEffect: () -> Unit): DisposableEffectResult = TODO()
}

interface DisposableEffectResult

@Composable
fun SideEffect(effect: () -> Unit) = Unit

@Composable
fun CompositionLocalProvider(vararg values: ProvidedValue<*>, content: @Composable () -> Unit) = Unit

fun <T> compositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> = TODO()
fun <T> staticCompositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> = TODO()

interface ProvidableCompositionLocal<T> {
    infix fun provides(value: T): ProvidedValue<T>
    val current: T
}

class ProvidedValue<T>

class CompositionLocal<T>

suspend fun withFrameNanos(onFrame: (Long) -> Unit): Long = TODO()

class SnapshotMutationPolicy<T>

@Target(AnnotationTarget.FUNCTION)
annotation class NonRestartableComposable

fun neverEqualPolicy(): SnapshotMutationPolicy<Any?> = TODO()

@Composable
fun collectAsStateCommon(): Nothing = TODO()
