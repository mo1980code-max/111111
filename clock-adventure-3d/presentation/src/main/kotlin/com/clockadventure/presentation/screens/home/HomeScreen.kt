package com.clockadventure.presentation.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.engine.ClockMath
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.MascotId
import com.clockadventure.presentation.R
import com.clockadventure.presentation.clock.InteractiveClock
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.BookIcon
import com.clockadventure.presentation.components.ChartIcon
import com.clockadventure.presentation.components.ClockIcon
import com.clockadventure.presentation.components.GamePadIcon
import com.clockadventure.presentation.components.GearIcon
import com.clockadventure.presentation.components.MascotMood
import com.clockadventure.presentation.components.MascotView
import com.clockadventure.presentation.components.PlayIcon
import com.clockadventure.presentation.components.RewardHud
import com.clockadventure.presentation.components.RoundIconButton
import com.clockadventure.presentation.components.ShieldIcon
import com.clockadventure.presentation.components.TrophyIcon
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.times
import androidx.compose.ui.draw.rotate

/**
 * The home screen: a big animated clock, the mascot, the reward hud and one big button per area.
 * Everything here is reachable with a thumb - no toolbar, no web style navigation bar.
 */
@Composable
fun HomeRoute(
    onStartLearning: (Int) -> Unit,
    onLessons: () -> Unit,
    onChallenges: () -> Unit,
    onGames: () -> Unit,
    onProgress: () -> Unit,
    onRewards: () -> Unit,
    onSettings: () -> Unit,
    onParent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onStartLearning = { viewModel.onButtonSound(); onStartLearning(state.nextLevelId()) },
        onLessons = { viewModel.onButtonSound(); onLessons() },
        onChallenges = { viewModel.onButtonSound(); onChallenges() },
        onGames = { viewModel.onButtonSound(); onGames() },
        onProgress = { viewModel.onButtonSound(); onProgress() },
        onRewards = { viewModel.onButtonSound(); onRewards() },
        onSettings = { viewModel.onButtonSound(); onSettings() },
        onParent = { viewModel.onButtonSound(); onParent() },
        onClaimDaily = viewModel::claimDailyReward,
        modifier = modifier
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onStartLearning: () -> Unit,
    onLessons: () -> Unit,
    onChallenges: () -> Unit,
    onGames: () -> Unit,
    onProgress: () -> Unit,
    onRewards: () -> Unit,
    onSettings: () -> Unit,
    onParent: () -> Unit,
    onClaimDaily: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = state.settings
    val lang = settings.language
    val reduceMotion = settings.reduceMotion

    // The clock on the home screen always shows the real time and ticks once a second.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val time = ClockMath.now()

    val transition = rememberInfiniteTransition(label = "home")
    val bob by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val spin by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spin"
    )
    val bobPx = if (reduceMotion) 0f else bob
    val tilt = if (reduceMotion) 0f else spin

    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.gapMedium)
            ) {
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_greeting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    RoundIconButton(onClick = onSettings, size = 48.dp) {
                        GearIcon(tint = appColors.accent)
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                RewardHud(
                    coins = state.progress.coins,
                    stars = state.progress.stars,
                    level = state.progress.playerLevel,
                    levelProgress = state.progress.levelProgress,
                    onSettingsClick = onSettings
                )
                Spacer(modifier = Modifier.height(Dimens.gapSmall))

                // --- the big animated clock -------------------------------------------------
                BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val clockSize: Dp = (maxWidth * 0.72f).coerceAtMost(Dimens.clockSize)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(clockSize)) {
                        InteractiveClock(
                            time = time,
                            style = settings.clockStyle,
                            interactive = false,
                            showDigital = true,
                            use24Hour = settings.use24Hour,
                            language = lang,
                            reduceMotion = reduceMotion,
                            modifier = Modifier
                                .size(clockSize)
                                .offset(y = bobPx.dp)
                                .rotate(tilt)
                        )
                        MascotView(
                            mascot = settings.character,
                            mood = MascotMood.HAPPY,
                            sizeDp = clockSize * 0.3f,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = (-bobPx).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.gapSmall))

                // --- the main action --------------------------------------------------------
                ArcadeButton(
                    text = stringResource(
                        if (state.progress.lessonsCompleted == 0) R.string.home_start_learning else R.string.home_continue
                    ),
                    onClick = onStartLearning,
                    height = Dimens.buttonHeight + 8.dp,
                    topColor = Palette.Mint,
                    bottomColor = Color(0xFF1E9E6E),
                    modifier = Modifier.fillMaxWidth(),
                    icon = { PlayIcon(tint = Color.White) }
                )

                Spacer(modifier = Modifier.height(Dimens.gapSmall))

                MenuGrid(
                    onLessons = onLessons,
                    onChallenges = onChallenges,
                    onGames = onGames,
                    onProgress = onProgress,
                    onRewards = onRewards,
                    onSettings = onSettings,
                    onParent = onParent
                )

                if (!state.dailyClaimed) {
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    ArcadeButton(
                        text = stringResource(R.string.rewards_daily_claim),
                        onClick = onClaimDaily,
                        height = Dimens.minTouchTarget,
                        topColor = Palette.SunYellow,
                        bottomColor = Palette.Orange,
                        modifier = Modifier.fillMaxWidth(),
                        icon = { TrophyIcon(tint = Color.White) }
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.gapMedium))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.gapMedium))
            }
        }
    }
}

@Composable
private fun MenuGrid(
    onLessons: () -> Unit,
    onChallenges: () -> Unit,
    onGames: () -> Unit,
    onProgress: () -> Unit,
    onRewards: () -> Unit,
    onSettings: () -> Unit,
    onParent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
            MenuButton(
                text = stringResource(R.string.home_lessons),
                onClick = onLessons,
                topColor = Palette.Ocean,
                bottomColor = Color(0xFF1560C0),
                icon = { BookIcon() },
                modifier = Modifier.weight(1f)
            )
            MenuButton(
                text = stringResource(R.string.home_challenges),
                onClick = onChallenges,
                topColor = Palette.CandyPurple,
                bottomColor = Color(0xFF5B2EA8),
                icon = { ClockIcon() },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
            MenuButton(
                text = stringResource(R.string.home_games),
                onClick = onGames,
                topColor = Palette.CandyPink,
                bottomColor = Color(0xFFE0407F),
                icon = { GamePadIcon() },
                modifier = Modifier.weight(1f)
            )
            MenuButton(
                text = stringResource(R.string.home_progress),
                onClick = onProgress,
                topColor = Color(0xFF3EC8B0),
                bottomColor = Color(0xFF1E9E8A),
                icon = { ChartIcon() },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
            MenuButton(
                text = stringResource(R.string.home_rewards),
                onClick = onRewards,
                topColor = Palette.SunYellow,
                bottomColor = Palette.Orange,
                icon = { TrophyIcon() },
                modifier = Modifier.weight(1f)
            )
            MenuButton(
                text = stringResource(R.string.home_settings),
                onClick = onSettings,
                topColor = Color(0xFF7C8AA5),
                bottomColor = Color(0xFF4A5875),
                icon = { GearIcon() },
                modifier = Modifier.weight(1f)
            )
        }
        MenuButton(
            text = stringResource(R.string.home_parent),
            onClick = onParent,
            topColor = Color(0xFF9AA7C7),
            bottomColor = Color(0xFF5F6C91),
            icon = { ShieldIcon() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit,
    topColor: Color,
    bottomColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    ArcadeButton(
        text = text,
        onClick = onClick,
        topColor = topColor,
        bottomColor = bottomColor,
        height = Dimens.minTouchTarget,
        modifier = modifier,
        icon = icon
    )
}
