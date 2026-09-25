package com.clockadventure.presentation.screens.games

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.catalog.GameCatalog
import com.clockadventure.domain.catalog.GameIcon
import com.clockadventure.presentation.R
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.ClockIcon
import com.clockadventure.presentation.components.GamePadIcon
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.PlayIcon
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.ShieldIcon
import com.clockadventure.presentation.components.TrophyIcon
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

@Composable
fun GamesRoute(
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GamesScreen(
        language = state.settings.language,
        scores = state.scores,
        onBack = onBack,
        onOpenGame = { gameId -> viewModel.onButtonSound(); onOpenGame(gameId) },
        modifier = modifier
    )
}

@Composable
internal fun GamesScreen(
    language: com.clockadventure.domain.model.AppLanguage,
    scores: Map<String, GameCardState>,
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.games_title),
                    subtitle = stringResource(R.string.games_subtitle),
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                ) {
                    GameCatalog.games.forEach { game ->
                        val score = scores[game.id.name]
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
                            Row(
                                modifier = Modifier.padding(Dimens.gapMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                                    GameIconView(icon = game.icon)
                                }
                                Spacer(modifier = Modifier.width(Dimens.gapSmall))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title[language],
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = game.subtitle[language],
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                    if (score != null && score.plays > 0) {
                                        Text(
                                            text = stringResource(R.string.game_best, score.bestScore),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Palette.Gold
                                        )
                                    }
                                }
                                ArcadeButton(
                                    text = stringResource(R.string.game_start),
                                    onClick = { onOpenGame(game.id.name) },
                                    height = Dimens.minTouchTarget,
                                    modifier = Modifier.width(120.dp),
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

@Composable
private fun GameIconView(icon: GameIcon) {
    when (icon) {
        GameIcon.QUESTION -> ClockIcon(tint = appColors.accent)
        GameIcon.HANDS -> ClockIcon(tint = Palette.CandyPurple)
        GameIcon.MATCH -> GamePadIcon(tint = Palette.Ocean)
        GameIcon.RACE -> TrophyIcon(tint = Palette.Gold)
        GameIcon.TRUE_FALSE -> ShieldIcon(tint = Palette.Mint)
        GameIcon.ROUTINE -> GamePadIcon(tint = Palette.CandyPink)
    }
}
