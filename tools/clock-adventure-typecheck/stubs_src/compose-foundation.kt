package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) = Unit

fun Modifier.background(color: Color, shape: Shape? = null): Modifier = this
fun Modifier.background(brush: Brush, shape: Shape? = null, alpha: Float = 1f): Modifier = this

interface Indication

interface InteractionSource

fun Modifier.clickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Any? = null,
    onClick: () -> Unit
): Modifier = this

fun Modifier.combinedClickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = this

fun Modifier.border(width: Dp, color: Color, shape: Shape? = null): Modifier = this
fun Modifier.border(width: Dp, brush: Brush, shape: Shape? = null): Modifier = this

@Composable
fun isSystemInDarkTheme(): Boolean = false

package androidx.compose.foundation.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.foundation.InteractionSource

class MutableInteractionSource : InteractionSource

@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> = TODO()

@Composable
fun InteractionSource.collectIsFocusedAsState(): State<Boolean> = TODO()

package androidx.compose.foundation.layout
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Density

interface BoxScope {
    fun Modifier.align(alignment: Alignment): Modifier = this
    fun Modifier.matchParentSize(): Modifier = this
}

interface ColumnScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = this
    fun Modifier.align(alignment: Any): Modifier = this
    fun Modifier.alignBy(alignmentLine: Any): Modifier = this
}

interface RowScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = this
    fun Modifier.align(alignment: Any): Modifier = this
    fun Modifier.alignBy(alignmentLine: Any): Modifier = this
}

object Arrangement {
    val Top: Vertical = Vertical()
    val Center: Vertical = Vertical()
    val Bottom: Vertical = Vertical()
    val Start: Horizontal = Horizontal()
    val End: Horizontal = Horizontal()
    val SpaceBetween: Horizontal = Horizontal()
    val SpaceAround: Horizontal = Horizontal()
    val SpaceEvenly: Horizontal = Horizontal()
    fun spacedBy(space: Dp): HorizontalOrVertical = HorizontalOrVertical()
}

class Vertical
class Horizontal
class HorizontalOrVertical

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) = Unit

@Composable
inline fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Any = Arrangement.Top,
    horizontalAlignment: Any = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) = Unit

@Composable
inline fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Any = Arrangement.Start,
    verticalAlignment: Any = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) = Unit

@Composable
fun Spacer(modifier: Modifier) = Unit

class PaddingValues {
    companion object {
        val Zero: PaddingValues = PaddingValues()
    }
}

fun PaddingValues(all: Dp): PaddingValues = PaddingValues()
fun PaddingValues(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues = PaddingValues()
fun PaddingValues(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues = PaddingValues()

fun Modifier.size(size: Dp): Modifier = this
fun Modifier.size(width: Dp, height: Dp): Modifier = this
fun Modifier.sizeIn(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified, maxWidth: Dp = Dp.Unspecified, maxHeight: Dp = Dp.Unspecified): Modifier = this
fun Modifier.requiredSize(size: Dp): Modifier = this
fun Modifier.width(width: Dp): Modifier = this
fun Modifier.height(height: Dp): Modifier = this
fun Modifier.widthIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier = this
fun Modifier.heightIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier = this
fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = this
fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = this
fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = this
fun Modifier.wrapContentWidth(align: Any? = null, unbounded: Boolean = false): Modifier = this
fun Modifier.wrapContentHeight(align: Any? = null, unbounded: Boolean = false): Modifier = this
fun Modifier.wrapContentSize(align: Any? = null, unbounded: Boolean = false): Modifier = this
fun Modifier.padding(all: Dp): Modifier = this
fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier = this
fun Modifier.padding(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): Modifier = this
fun Modifier.padding(paddingValues: PaddingValues): Modifier = this
fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this
fun Modifier.offset(offset: Density.() -> IntOffset): Modifier = this
fun Modifier.absoluteOffset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this
fun Modifier.aspectRatio(ratio: Float, matchHeightConstraintsFirst: Boolean = false): Modifier = this

interface BoxWithConstraintsScope : BoxScope {
    val constraints: Any
    val maxWidth: Dp
    val maxHeight: Dp
    val minWidth: Dp
    val minHeight: Dp
}

@Composable
fun BoxWithConstraints(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) = Unit

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement

interface LazyListScope {
    fun items(count: Int, key: ((index: Int) -> Any)? = null, itemContent: @Composable LazyItemScope.(index: Int) -> Unit)
    fun <T> items(items: List<T>, key: ((item: T) -> Any)? = null, contentType: ((item: T) -> Any?)? = null, itemContent: @Composable LazyItemScope.(item: T) -> Unit)
    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit)
}

interface LazyItemScope

class LazyListState

@Composable
fun rememberLazyListState(initialFirstVisibleItemIndex: Int = 0, initialFirstVisibleItemScrollOffset: Int = 0): LazyListState = TODO()

@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Any = Arrangement.Top,
    horizontalAlignment: Any = androidx.compose.ui.Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) = Unit

@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    horizontalArrangement: Any = Arrangement.Start,
    verticalAlignment: Any = androidx.compose.ui.Alignment.Top,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) = Unit

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

suspend fun PointerInputScope.detectDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) = Unit

suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null
) = Unit

suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) = Unit

package androidx.compose.foundation.shape
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

class RoundedCornerShape : Shape
class CircleShapeType : Shape

fun RoundedCornerShape(size: Dp): RoundedCornerShape = RoundedCornerShape()
fun RoundedCornerShape(percent: Int): RoundedCornerShape = RoundedCornerShape()
fun RoundedCornerShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): RoundedCornerShape = RoundedCornerShape()

val CircleShape: Shape = CircleShapeType()

package androidx.compose.foundation

import androidx.compose.ui.Modifier

class ScrollState(initial: Int = 0) {
    var value: Int = initial
    suspend fun animateScrollTo(value: Int, animationSpec: Any? = null) {}
    suspend fun scrollTo(value: Int) {}
}

fun Modifier.verticalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this.then(Modifier)
fun Modifier.horizontalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this.then(Modifier)
fun rememberScrollState(initial: Int = 0): ScrollState = ScrollState(initial)
