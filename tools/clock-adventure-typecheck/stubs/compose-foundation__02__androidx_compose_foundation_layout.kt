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
