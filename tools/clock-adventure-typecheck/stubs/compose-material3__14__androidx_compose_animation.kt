package androidx.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.AnimationSpec

@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = EnterTransition(),
    exit: ExitTransition = ExitTransition(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) = Unit

interface AnimatedVisibilityScope

class EnterTransition {
    operator fun plus(other: EnterTransition): EnterTransition = this
}

class ExitTransition {
    operator fun plus(other: ExitTransition): ExitTransition = this
}

fun fadeIn(animationSpec: AnimationSpec<Float> = androidx.compose.animation.core.spring()): EnterTransition = EnterTransition()
fun fadeOut(animationSpec: AnimationSpec<Float> = androidx.compose.animation.core.spring()): ExitTransition = ExitTransition()
fun slideInVertically(animationSpec: AnimationSpec<androidx.compose.ui.unit.IntOffset> = androidx.compose.animation.core.spring(), initialOffsetY: (Int) -> Int = { it / 3 }): EnterTransition = EnterTransition()
fun slideOutVertically(animationSpec: AnimationSpec<androidx.compose.ui.unit.IntOffset> = androidx.compose.animation.core.spring(), targetOffsetY: (Int) -> Int = { it / 3 }): ExitTransition = ExitTransition()
fun expandVertically(animationSpec: AnimationSpec<androidx.compose.ui.unit.IntSize> = androidx.compose.animation.core.spring(), expandFrom: Any = Any()): EnterTransition = EnterTransition()
fun shrinkVertically(animationSpec: AnimationSpec<androidx.compose.ui.unit.IntSize> = androidx.compose.animation.core.spring(), shrinkTowards: Any = Any()): ExitTransition = ExitTransition()
fun scaleIn(animationSpec: AnimationSpec<Float> = androidx.compose.animation.core.spring(), initialScale: Float = 0.7f): EnterTransition = EnterTransition()
fun scaleOut(animationSpec: AnimationSpec<Float> = androidx.compose.animation.core.spring(), targetScale: Float = 0.7f): ExitTransition = ExitTransition()

@Composable
fun Crossfade(
    targetState: Any?,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = androidx.compose.animation.core.spring(),
    label: String = "Crossfade",
    content: @Composable (Any?) -> Unit
) = Unit
