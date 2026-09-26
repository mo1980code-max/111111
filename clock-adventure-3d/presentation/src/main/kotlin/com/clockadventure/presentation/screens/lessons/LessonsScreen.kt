package com.clockadventure.presentation.screens.lessons

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.presentation.R
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.BookIcon
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.LockIcon
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.StarRow
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

@Composable
fun LessonsRoute(
    onBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LessonsScreen(
        state = state,
        onBack = onBack,
        onOpenLesson = { levelId -> viewModel.onSelectLevel(levelId); onOpenLesson(levelId) },
        modifier = modifier
    )
}

@Composable
internal fun LessonsScreen(
    state: LessonsUiState,
    onBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.lessons_title),
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
                    state.cards.forEach { card ->
                        LessonCard(
                            card = card,
                            title = LevelCatalog.byId(card.levelId).title[lang],
                            subtitle = LevelCatalog.byId(card.levelId).subtitle[lang],
                            onClick = { onOpenLesson(card.levelId) }
                        )
                    }
                    Text(
                        text = stringResource(R.string.lessons_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                }
            }
        
            }
        }
    }
}

@Composable
private fun LessonCard(
    card: LessonCardState,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
        Row(
            modifier = Modifier.padding(Dimens.gapMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .padding(end = Dimens.gapSmall),
                contentAlignment = Alignment.Center
            ) {
                if (card.unlocked) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = card.levelId.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (card.completed) Palette.Mint else appColors.accent
                        )
                    }
                } else {
                    LockIcon(tint = Color(0xFF9AA7C7))
                }
            }
            Spacer(modifier = Modifier.width(Dimens.gapSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                StarRow(stars = card.bestStars, size = 20.dp)
            }
            if (card.unlocked) {
                ArcadeButton(
                    text = if (card.completed) stringResource(R.string.common_retry) else stringResource(R.string.lesson_start),
                    onClick = onClick,
                    height = Dimens.minTouchTarget,
                    modifier = Modifier.width(112.dp),
                    icon = { BookIcon() }
                )
            }
        }
    }
}
