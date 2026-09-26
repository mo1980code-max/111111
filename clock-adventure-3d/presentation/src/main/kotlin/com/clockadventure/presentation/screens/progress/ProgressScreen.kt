package com.clockadventure.presentation.screens.progress

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
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.ProgressBar
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.StarRow
import com.clockadventure.presentation.components.StatBar
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import com.clockadventure.presentation.util.DateText
import androidx.compose.foundation.layout.width

@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(state = state, onBack = onBack, modifier = modifier)
}

@Composable
internal fun ProgressScreen(
    state: ProgressUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    val progress = state.progress
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.progress_title),
                    subtitle = stringResource(R.string.progress_subtitle),
                    onBack = onBack
                )
                if (progress.totalAnswers == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.progress_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(Dimens.gapLarge)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.gapMedium),
                        verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                                Text(
                                    text = stringResource(R.string.common_level, progress.playerLevel),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ProgressBar(progress = progress.levelProgress)
                                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    StatBlock(
                                        value = "${progress.accuracyPercent}%",
                                        label = stringResource(R.string.progress_accuracy)
                                    )
                                    StatBlock(
                                        value = progress.totalAnswers.toString(),
                                        label = stringResource(R.string.progress_questions)
                                    )
                                    StatBlock(
                                        value = progress.totalCorrect.toString(),
                                        label = stringResource(R.string.progress_correct)
                                    )
                                    StatBlock(
                                        value = progress.totalWrong.toString(),
                                        label = stringResource(R.string.progress_wrong)
                                    )
                                }
                                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                Text(
                                    text = timeText(progress.totalPlaySeconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = appColors.accent,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.progress_time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (state.daily.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                                    Text(
                                        text = stringResource(R.string.parent_daily),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                    val max = state.daily.maxOf { it.secondsLearned }.coerceAtLeast(60)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        state.daily.forEach { stat ->
                                            StatBar(
                                                value = stat.secondsLearned.toFloat(),
                                                label = DateText.shortDay(stat.dateKey),
                                                maxValue = max.toFloat(),
                                                modifier = Modifier.size(width = 34.dp, height = 96.dp),
                                                color = appColors.accent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.progress_per_level),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        state.lessons.filter { it.answers > 0 }.forEach { lesson ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
                                Row(
                                    modifier = Modifier.padding(Dimens.gapMedium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = LevelCatalog.byId(lesson.levelId).title[lang],
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ProgressBar(
                                            progress = lesson.accuracy,
                                            height = 14.dp,
                                            fillTop = if (lesson.accuracy >= 0.8f) Palette.Mint else Palette.SunYellow,
                                            fillBottom = if (lesson.accuracy >= 0.8f) Color(0xFF1E9E6E) else Palette.Orange
                                        )
                                        Text(
                                            text = "${(lesson.accuracy * 100).toInt()}% • ${lesson.correct}/${lesson.answers}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Dimens.gapSmall))
                                    StarRow(stars = lesson.bestStars, size = 22.dp)
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
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun timeText(seconds: Int): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${minutes % 60}min" else "$minutes min"
}
