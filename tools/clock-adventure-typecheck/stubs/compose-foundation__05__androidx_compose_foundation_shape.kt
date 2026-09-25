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
