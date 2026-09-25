package androidx.compose.material3
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Indication
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    overflow: Int = 0,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = TextStyle()
) = Unit

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) = Unit

object MaterialTheme {
    val colorScheme: ColorScheme get() = ColorScheme()
    val typography: Typography get() = Typography()
    val shapes: Shapes get() = Shapes()
}

@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = ColorScheme(),
    shapes: Shapes = Shapes(),
    typography: Typography = Typography(),
    content: @Composable () -> Unit
) = Unit

class ColorScheme {
    val primary: Color = Color.Unspecified
    val onPrimary: Color = Color.Unspecified
    val primaryContainer: Color = Color.Unspecified
    val onPrimaryContainer: Color = Color.Unspecified
    val secondary: Color = Color.Unspecified
    val onSecondary: Color = Color.Unspecified
    val secondaryContainer: Color = Color.Unspecified
    val onSecondaryContainer: Color = Color.Unspecified
    val tertiary: Color = Color.Unspecified
    val onTertiary: Color = Color.Unspecified
    val background: Color = Color.Unspecified
    val onBackground: Color = Color.Unspecified
    val surface: Color = Color.Unspecified
    val onSurface: Color = Color.Unspecified
    val surfaceVariant: Color = Color.Unspecified
    val onSurfaceVariant: Color = Color.Unspecified
    val error: Color = Color.Unspecified
    val onError: Color = Color.Unspecified
    val outline: Color = Color.Unspecified
    val surfaceTint: Color = Color.Unspecified
    val scrim: Color = Color.Unspecified
}

fun lightColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    primaryContainer: Color = Color.Unspecified,
    onPrimaryContainer: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    secondaryContainer: Color = Color.Unspecified,
    onSecondaryContainer: Color = Color.Unspecified,
    tertiary: Color = Color.Unspecified,
    onTertiary: Color = Color.Unspecified,
    tertiaryContainer: Color = Color.Unspecified,
    onTertiaryContainer: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    surfaceVariant: Color = Color.Unspecified,
    onSurfaceVariant: Color = Color.Unspecified,
    surfaceTint: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified,
    outline: Color = Color.Unspecified,
    scrim: Color = Color.Unspecified
): ColorScheme = ColorScheme()

fun darkColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    primaryContainer: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    secondaryContainer: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified
): ColorScheme = ColorScheme()

class Shapes(
    val extraSmall: Shape? = null,
    val small: Shape? = null,
    val medium: Shape? = null,
    val large: Shape? = null,
    val extraLarge: Shape? = null
)

class Typography(
    val displayLarge: TextStyle = TextStyle(),
    val displayMedium: TextStyle = TextStyle(),
    val displaySmall: TextStyle = TextStyle(),
    val headlineLarge: TextStyle = TextStyle(),
    val headlineMedium: TextStyle = TextStyle(),
    val headlineSmall: TextStyle = TextStyle(),
    val titleLarge: TextStyle = TextStyle(),
    val titleMedium: TextStyle = TextStyle(),
    val titleSmall: TextStyle = TextStyle(),
    val bodyLarge: TextStyle = TextStyle(),
    val bodyMedium: TextStyle = TextStyle(),
    val bodySmall: TextStyle = TextStyle(),
    val labelLarge: TextStyle = TextStyle(),
    val labelMedium: TextStyle = TextStyle(),
    val labelSmall: TextStyle = TextStyle()
)

class ButtonColors
class ButtonElevation

object ButtonDefaults {
    val ContentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()
    val shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(20)

    @Composable
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): ButtonColors = ButtonColors()

    @Composable
    fun buttonElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
        disabledElevation: Dp = 0.dp
    ): ButtonElevation = ButtonElevation()
}

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): Any = Any()

    @Composable
    fun cardElevation(defaultElevation: Dp = 0.dp, pressedElevation: Dp = 0.dp): Any = Any()
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: Any? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) = Unit

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: Any? = null,
    content: @Composable () -> Unit
) = Unit

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12),
    colors: Any = CardDefaults.cardColors(),
    elevation: Any = CardDefaults.cardElevation(),
    border: Any? = null,
    content: @Composable () -> Unit
) = Unit

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: Any? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null
) = Unit

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: Any? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null
) = Unit

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null
) = Unit

@Composable
fun RadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) = Unit

@Composable
fun LinearProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified, trackColor: Color = Color.Unspecified) = Unit

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) = Unit

@Composable
fun ripple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified
): Indication = object : Indication {}

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = Color.Unspecified, thickness: Dp = 1.dp) = Unit

package androidx.compose.material3
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) = Unit

package androidx.compose.ui.graphics

interface Shape

package androidx.compose.ui.text
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

class TextStyle(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit(Float.NaN),
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val letterSpacing: TextUnit = TextUnit(Float.NaN),
    val textAlign: TextAlign? = null,
    val lineHeight: TextUnit = TextUnit(Float.NaN),
    val background: Color = Color.Unspecified,
    val shadow: Shadow? = null
) {
    fun copy(
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontFamily: FontFamily? = this.fontFamily,
        letterSpacing: TextUnit = this.letterSpacing,
        textAlign: TextAlign? = this.textAlign,
        lineHeight: TextUnit = this.lineHeight,
        background: Color = this.background,
        shadow: Shadow? = this.shadow
    ): TextStyle = TextStyle()
}

class Shadow(
    val color: Color = Color(0xFF000000),
    val offset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val blurRadius: Float = 0f
)

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

package androidx.compose.ui.text.font

import androidx.compose.ui.text.font.FontWeight

class FontFamily

fun FontFamily(vararg fonts: Any): FontFamily = FontFamily()

class FontStyle {
    companion object {
        val Normal: FontStyle = FontStyle()
        val Italic: FontStyle = FontStyle()
    }
}

class FontWeight(val weight: Int) {
    companion object {
        val Thin = FontWeight(100)
        val ExtraLight = FontWeight(200)
        val Light = FontWeight(300)
        val Normal = FontWeight(400)
        val Medium = FontWeight(500)
        val SemiBold = FontWeight(600)
        val Bold = FontWeight(700)
        val ExtraBold = FontWeight(800)
        val Black = FontWeight(900)
        val W100 = Thin
        val W200 = ExtraLight
        val W300 = Light
        val W400 = Normal
        val W500 = Medium
        val W600 = SemiBold
        val W700 = Bold
        val W800 = ExtraBold
        val W900 = Black
    }
}

class Font

fun Font(resId: Int, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font = Font()

package androidx.compose.ui.res

import androidx.compose.runtime.Composable

@Composable
fun stringResource(id: Int): String = ""

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String = ""

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

package androidx.compose.ui.unit

val LocalLayoutDirection: androidx.compose.runtime.ProvidableCompositionLocal<LayoutDirection> =
    androidx.compose.runtime.staticCompositionLocalOf { LayoutDirection.Ltr }

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

interface PointerInputScope {
    val size: androidx.compose.ui.geometry.Size
    val viewConfiguration: Any
}

class PointerInputChange {
    val position: Offset get() = Offset.Zero
    val previousPosition: Offset get() = Offset.Zero
}

fun Modifier.pointerInput(vararg keys: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = Modifier
fun Modifier.pointerInput(key1: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = Modifier

package androidx.compose.ui.window

import androidx.compose.runtime.Composable

class DialogProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
    val securePolicy: Int = 0,
    val usePlatformDefaultWidth: Boolean = true,
    val decorFitsSystemWindows: Boolean = true
)

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) = Unit

package androidx.compose.ui.draw
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp

fun Modifier.shadow(
    elevation: Dp,
    shape: Shape? = null,
    clip: Boolean = true,
    ambientColor: Color = Color(0xFF000000),
    spotColor: Color = Color(0xFF000000)
): Modifier = this

fun Modifier.clip(shape: Shape): Modifier = this
fun Modifier.alpha(alpha: Float): Modifier = this
fun Modifier.rotate(degrees: Float): Modifier = this
fun Modifier.scale(scaleX: Float, scaleY: Float = scaleX): Modifier = this
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit): Modifier = this
fun Modifier.drawBehind(onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit): Modifier = this
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult): Modifier = this

class CacheDrawScope
class DrawResult

fun Modifier.blur(radius: Dp, edgeTreatment: Int = 0): Modifier = this

package androidx.compose.ui

import androidx.compose.ui.unit.LayoutDirection

class Alignment(val value: Int) {
    companion object {
        val TopStart: Alignment = Alignment(0)
        val TopCenter: Alignment = Alignment(1)
        val TopEnd: Alignment = Alignment(2)
        val CenterStart: Alignment = Alignment(3)
        val Center: Alignment = Alignment(4)
        val CenterEnd: Alignment = Alignment(5)
        val BottomStart: Alignment = Alignment(6)
        val BottomCenter: Alignment = Alignment(7)
        val BottomEnd: Alignment = Alignment(8)
        val Start: Any = Alignment(9)
        val End: Any = Alignment(10)
        val Top: Any = Alignment(11)
        val Bottom: Any = Alignment(12)
        val CenterHorizontally: Any = Alignment(13)
        val CenterVertically: Any = Alignment(14)
    }
}

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
