package com.clock.livewallpaper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One corner rhythm for the whole product: chips 14, rows 20, cards 26, hero/sheets 32.
 * The floating card sits inside the same family at 26dp.
 */
val DhikrShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object DhikrRadius {
    val chip = 14.dp
    val row = 20.dp
    val card = 26.dp
    val hero = 32.dp
    val overlayCard = 26.dp
}

object DhikrSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val screen = 20.dp
}
