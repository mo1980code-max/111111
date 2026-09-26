package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

class AnimationSpec<T>
class InfiniteRepeatableSpec<T>
class EasingSpec

object Easing {
    val LinearEasing: Any = Any()
}

val LinearEasing: Any = Easing.LinearEasing
val FastOutSlowInEasing: Any = Any()

enum class RepeatMode { Restart, Reverse }

fun <T> tween(
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    easing: Any = FastOutSlowInEasing
): AnimationSpec<T> = AnimationSpec()

fun <T> spring(
    dampingRatio: Float = 0.5f,
    stiffness: Float = 200f,
    visibilityThreshold: Any? = null
): AnimationSpec<T> = AnimationSpec()

fun <T> keyframes(init: Any.() -> Unit): AnimationSpec<T> = AnimationSpec()

fun <T> infiniteRepeatable(
    animation: AnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: Any? = null
): InfiniteRepeatableSpec<T> = InfiniteRepeatableSpec()

fun <T> repeatable(
    iterations: Int,
    animation: AnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
): AnimationSpec<T> = AnimationSpec()

class InfiniteTransition

fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "FloatAnimation"
): State<Float> = TODO()

fun InfiniteTransition.animateColor(
    initialValue: androidx.compose.ui.graphics.Color,
    targetValue: androidx.compose.ui.graphics.Color,
    animationSpec: InfiniteRepeatableSpec<androidx.compose.ui.graphics.Color>,
    label: String = "ColorAnimation"
): State<androidx.compose.ui.graphics.Color> = TODO()

@Composable
fun rememberInfiniteTransition(label: String = "InfiniteTransition"): InfiniteTransition = InfiniteTransition()

@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = spring(),
    visibilityThreshold: Float? = null,
    label: String = "FloatAnimation",
    finishedListener: ((Float) -> Unit)? = null
): State<Float> = TODO()

@Composable
fun animateDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = spring(),
    label: String = "DpAnimation",
    finishedListener: ((Dp) -> Unit)? = null
): State<Dp> = TODO()

@Composable
fun animateColorAsState(
    targetValue: androidx.compose.ui.graphics.Color,
    animationSpec: AnimationSpec<androidx.compose.ui.graphics.Color> = spring(),
    label: String = "ColorAnimation",
    finishedListener: ((androidx.compose.ui.graphics.Color) -> Unit)? = null
): State<androidx.compose.ui.graphics.Color> = TODO()

@Composable
fun animateIntAsState(
    targetValue: Int,
    animationSpec: AnimationSpec<Int> = spring(),
    label: String = "IntAnimation"
): State<Int> = TODO()

class Animatable<T>(initialValue: T) {
    suspend fun animateTo(targetValue: T, animationSpec: AnimationSpec<T> = spring()): Any = Any()
    suspend fun snapTo(targetValue: T) = Unit
    val value: T get() = TODO()
}
