package com.clockadventure.presentation.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockadventure.domain.engine.ClockMath
import com.clockadventure.presentation.R
import com.clockadventure.presentation.clock.MiniClock
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.theme.Dimens
import androidx.compose.ui.unit.times

/**
 * Shown for the split second before the saved settings arrive.
 *
 * Reading the settings is a disk access, so the navigation graph cannot know yet whether this is a
 * first run (onboarding) or a normal start (home). The splash turns that wait into the opening
 * image of the game instead of a blank frame.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash")
    val bob by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashBob"
    )
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                MiniClock(
                    time = ClockMath.now(),
                    size = Dimens.clockSize * 0.6f,
                    showNumbers = true,
                    modifier = Modifier
                        .size(Dimens.clockSize * 0.6f)
                        .offset(y = bob.dp)
                )
                Spacer(modifier = Modifier.height(Dimens.gapLarge))
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
