package com.clockadventure.presentation.screens.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockadventure.domain.catalog.ChallengeCatalog
import com.clockadventure.presentation.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.ClockIcon
import com.clockadventure.presentation.components.GamePadIcon
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.PlayIcon
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.ShieldIcon
import com.clockadventure.presentation.components.TrophyIcon
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

/** Optional harder modes: they never block the ten lessons, they simply add spice. */
@Composable
fun ChallengesRoute(
    onBack: () -> Unit,
    onOpenChallenge: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: com.clockadventure.presentation.screens.games.GamesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChallengesScreen(
        language = state.settings.language,
        onBack = onBack,
        onOpenChallenge = { challengeId -> viewModel.onButtonSound(); onOpenChallenge(challengeId) },
        modifier = modifier
    )
}

@Composable
internal fun ChallengesScreen(
    language: com.clockadventure.domain.model.AppLanguage,
    onBack: () -> Unit,
    onOpenChallenge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.home_challenges),
                    subtitle = stringResource(R.string.lessons_subtitle),
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                ) {
                    ChallengeCatalog.items.forEach { challenge ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
                            Row(
                                modifier = Modifier.padding(Dimens.gapMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                                    when (challenge.icon) {
                                        com.clockadventure.domain.catalog.GameIcon.RACE -> GamePadIcon(tint = appColors.accent)
                                        com.clockadventure.domain.catalog.GameIcon.TRUE_FALSE -> ShieldIcon(tint = appColors.accent)
                                        com.clockadventure.domain.catalog.GameIcon.HANDS -> ClockIcon(tint = appColors.accent)
                                        else -> TrophyIcon(tint = Palette.Gold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(Dimens.gapSmall))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = challenge.title[language],
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = challenge.subtitle[language],
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                                ArcadeButton(
                                    text = stringResource(R.string.game_start),
                                    onClick = { onOpenChallenge(challenge.id) },
                                    height = Dimens.minTouchTarget,
                                    modifier = Modifier.width(120.dp),
                                    topColor = Palette.CandyPurple,
                                    bottomColor = Color(0xFF5B2EA8),
                                    icon = { PlayIcon() }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                }
            }
        
            }
        }
    }
}
