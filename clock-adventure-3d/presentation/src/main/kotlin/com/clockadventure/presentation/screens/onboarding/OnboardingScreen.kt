package com.clockadventure.presentation.screens.onboarding

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
import com.clockadventure.domain.engine.TimeFormatter
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.MascotId
import com.clockadventure.presentation.R
import com.clockadventure.presentation.clock.InteractiveClock
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.CoinIcon
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.MascotBubble
import com.clockadventure.presentation.components.MascotMood
import com.clockadventure.presentation.components.MascotView
import com.clockadventure.presentation.components.Pill
import com.clockadventure.presentation.components.StepDots
import com.clockadventure.presentation.components.StarRow
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import androidx.compose.ui.unit.times

/**
 * The first run introduction: who the mascot is, what the two hands do, what stars and coins are
 * for, and the promise that the game needs no account and no internet.
 *
 * It is shown exactly once - [OnboardingViewModel] flips `hasSeenIntro` when the child finishes.
 */
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()

    OnboardingScreen(
        settings = settings,
        page = page,
        onNext = { viewModel.next(onFinished) },
        onBack = viewModel::back,
        onSkip = { viewModel.skip(onFinished) },
        onSelectCharacter = viewModel::selectCharacter,
        modifier = modifier
    )
}

@Composable
internal fun OnboardingScreen(
    settings: AppSettings,
    page: OnboardingPage,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onSelectCharacter: (MascotId) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.gapMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                StepDots(
                    total = OnboardingPage.entries.size,
                    current = OnboardingPage.entries.indexOf(page),
                    modifier = Modifier.fillMaxWidth()
                )
                if (page != OnboardingPage.WELCOME) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                        ArcadeButton(
                            text = stringResource(R.string.onboarding_back),
                            onClick = onBack,
                            height = 52.dp,
                            topColor = Palette.Silver,
                            bottomColor = Palette.InkSoft,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(52.dp))
                }

                when (page) {
                    OnboardingPage.WELCOME -> WelcomePage(settings = settings, onSelectCharacter = onSelectCharacter)
                    OnboardingPage.HANDS -> HandsPage(settings = settings)
                    OnboardingPage.REWARDS -> RewardsPage(settings = settings)
                    OnboardingPage.OFFLINE -> OfflinePage(settings = settings)
                }

                Spacer(modifier = Modifier.height(Dimens.gapMedium))
                ArcadeButton(
                    text = stringResource(
                        if (page == OnboardingPage.OFFLINE) R.string.onboarding_start else R.string.onboarding_next
                    ),
                    onClick = onNext,
                    topColor = if (page == OnboardingPage.OFFLINE) Palette.Mint else appColors.accent,
                    bottomColor = if (page == OnboardingPage.OFFLINE) Color(0xFF1E9E6E) else appColors.accentDark,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                ArcadeButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
                    height = 52.dp,
                    topColor = Palette.Silver,
                    bottomColor = Palette.InkSoft,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Spacer(modifier = Modifier.height(Dimens.gapMedium))
            }
        
            }
        }
    }
}

@Composable
private fun WelcomePage(settings: AppSettings, onSelectCharacter: (MascotId) -> Unit) {
    val lang = settings.language
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    MascotView(mascot = settings.character, mood = MascotMood.HAPPY, sizeDp = 160.dp)
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    Text(
        text = stringResource(R.string.onboarding_pick_friend),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        MascotId.entries.forEach { character ->
            val selected = character == settings.character
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(if (selected) 78.dp else 66.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MascotView(
                        mascot = character,
                        mood = if (selected) MascotMood.HAPPY else MascotMood.SLEEPY,
                        sizeDp = if (selected) 78.dp else 66.dp
                    )
                }
                Text(
                    text = character.label[lang],
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = if (selected) appColors.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                ArcadeButton(
                    text = if (selected) "★" else "○",
                    onClick = { onSelectCharacter(character) },
                    height = 44.dp,
                    topColor = if (selected) Palette.Mint else Palette.Silver,
                    bottomColor = if (selected) Palette.Ocean else Palette.InkSoft,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
}

@Composable
private fun HandsPage(settings: AppSettings) {
    var time by remember { mutableStateOf(ClockTime.of12(10, 10, pm = false)) }
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val clockSize: Dp = (maxWidth * 0.8f).coerceAtMost(Dimens.clockSize)
        InteractiveClock(
            time = time,
            style = settings.clockStyle,
            interactive = true,
            snapMinutes = 5,
            showDigital = true,
            use24Hour = settings.use24Hour,
            language = settings.language,
            reduceMotion = settings.reduceMotion,
            contentDescription = stringResource(
                R.string.cd_clock_time,
                TimeFormatter.spoken(time, settings.language)
            ),
            onTimeChanged = { time = it },
            modifier = Modifier.size(clockSize)
        )
    }
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    MascotBubble(text = stringResource(R.string.onboarding_hands_body))
    Spacer(modifier = Modifier.height(Dimens.gapSmall))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Pill(
            text = stringResource(R.string.clock_hour_hand) + " — " +
                TimeFormatter.digital(time, settings.use24Hour, settings.language),
            containerColor = Palette.CandyPink
        )
        Pill(text = stringResource(R.string.clock_drag_both), containerColor = Palette.Ocean)
    }
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    Text(
        text = stringResource(R.string.onboarding_hands_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RewardsPage(settings: AppSettings) {
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StarRow(stars = 3, size = 46.dp)
        CoinIcon(tint = Palette.Gold, modifier = Modifier.size(56.dp))
    }
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.onboarding_rewards_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            Text(
                text = stringResource(R.string.onboarding_rewards_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    MascotView(mascot = settings.character, mood = MascotMood.EXCITED, sizeDp = 130.dp)
}

@Composable
private fun OfflinePage(settings: AppSettings) {
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.onboarding_offline_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            Text(
                text = stringResource(R.string.onboarding_offline_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Pill(text = stringResource(R.string.onboarding_no_account), containerColor = Palette.Mint)
                Pill(text = stringResource(R.string.onboarding_no_internet), containerColor = Palette.SunYellow)
            }
        }
    }
    Spacer(modifier = Modifier.height(Dimens.gapMedium))
    MascotView(mascot = settings.character, mood = MascotMood.HAPPY, sizeDp = 150.dp)
}
