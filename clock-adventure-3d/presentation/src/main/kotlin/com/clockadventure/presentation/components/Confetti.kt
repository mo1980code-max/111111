package com.clockadventure.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.clockadventure.presentation.theme.LocalReduceMotion
import com.clockadventure.presentation.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawOval
import androidx.compose.ui.graphics.drawscope.drawRect

/**
 * Confetti burst shown when a level or a mini game is finished.
 *
 * Real particle physics on the Canvas: 90 pieces with their own speed, spin and colour, stepped by
 * the frame clock for three seconds. The state is read *outside* the Canvas so the composable
 * recomposes each frame - that is what makes Compose redraw it.
 */
@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 90,
    colors: List<Color> = listOf(
        Palette.SunYellow,
        Palette.CandyPink,
        Palette.Mint,
        Palette.Ocean,
        Palette.CandyPurple
    )
) {
    val reduceMotion = LocalReduceMotion.current
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(visible, reduceMotion) {
        if (!visible || reduceMotion) {
            particles = emptyList()
            return@LaunchedEffect
        }
        particles = List(particleCount) { Particle.random(colors) }
        var last = withFrameNanos { it }
        var elapsed = 0f
        while (elapsed < 3.2f) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            elapsed += dt
            particles = particles.map { it.step(dt) }.filter { it.y < 1.25f }
            frame++
        }
        particles = emptyList()
    }

    // Reading the frame counter here (not inside the canvas) is what schedules the redraw.
    val tick = frame
    if (particles.isNotEmpty() && tick >= 0) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEach { particle ->
                val px = particle.x * w
                val py = particle.y * h
                rotate(degrees = particle.rotation, pivot = Offset(px, py)) {
                    when (particle.shape) {
                        0 -> drawRect(
                            color = particle.color,
                            topLeft = Offset(px, py),
                            size = Size(particle.size, particle.size * 1.6f)
                        )
                        1 -> drawCircle(color = particle.color, radius = particle.size * 0.6f, center = Offset(px, py))
                        else -> drawOval(
                            color = particle.color,
                            topLeft = Offset(px, py),
                            size = Size(particle.size, particle.size * 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val rotation: Float,
    val spin: Float,
    val color: Color,
    val shape: Int
) {
    fun step(dt: Float): Particle = copy(
        x = x + vx * dt,
        y = y + vy * dt,
        vy = vy + GRAVITY * dt,
        vx = vx * (1f - DRAG * dt),
        rotation = rotation + spin * dt
    )

    companion object {
        const val GRAVITY = 0.85f
        const val DRAG = 0.55f

        fun random(colors: List<Color>): Particle {
            val angle = (-PI / 2.0) + (Random.nextDouble() - 0.5) * PI * 0.9
            val speed = 0.35f + Random.nextFloat() * 0.55f
            return Particle(
                x = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                y = 0.52f + (Random.nextFloat() - 0.5f) * 0.1f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat() - 0.35f,
                size = 10f + Random.nextFloat() * 14f,
                rotation = Random.nextFloat() * 360f,
                spin = (Random.nextFloat() - 0.5f) * 720f,
                color = colors.random(),
                shape = Random.nextInt(3)
            )
        }
    }
}
