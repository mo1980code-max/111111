package com.clockadventure.presentation.screens.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.AppTheme
import com.clockadventure.domain.model.ClockStyle
import com.clockadventure.domain.model.DifficultyMode
import com.clockadventure.domain.model.MascotId
import com.clockadventure.presentation.R
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(settings = state.settings, onBack = onBack, actions = viewModel, modifier = modifier)
}

@Composable
internal fun SettingsScreen(
    settings: com.clockadventure.domain.model.AppSettings,
    onBack: () -> Unit,
    actions: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.settings_title),
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                ) {
                    SettingsCard(title = stringResource(R.string.settings_language)) {
                        ChoiceRow(
                            options = listOf(
                                AppLanguage.ENGLISH to stringResource(R.string.settings_language_en),
                                AppLanguage.ARABIC to stringResource(R.string.settings_language_ar)
                            ),
                            selected = settings.language,
                            onSelected = actions::setLanguage
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_difficulty)) {
                        ChoiceRow(
                            options = listOf(
                                DifficultyMode.AUTO to stringResource(R.string.settings_difficulty_auto),
                                DifficultyMode.EASY to stringResource(R.string.settings_difficulty_easy),
                                DifficultyMode.MEDIUM to stringResource(R.string.settings_difficulty_medium),
                                DifficultyMode.HARD to stringResource(R.string.settings_difficulty_hard)
                            ),
                            selected = settings.difficultyMode,
                            onSelected = actions::setDifficulty
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_sound)) {
                        ToggleRow(
                            label = stringResource(R.string.settings_music),
                            checked = settings.musicEnabled,
                            onCheckedChange = actions::setMusic
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_sound),
                            checked = settings.soundEnabled,
                            onCheckedChange = actions::setSound
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_voice),
                            checked = settings.voiceEnabled,
                            onCheckedChange = actions::setVoice
                        )
                        Text(
                            text = stringResource(R.string.settings_voice_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_gameplay)) {
                        ToggleRow(
                            label = stringResource(R.string.settings_24h),
                            checked = settings.use24Hour,
                            onCheckedChange = actions::set24Hour
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_hints),
                            checked = settings.hintsEnabled,
                            onCheckedChange = actions::setHints
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_reduce_motion),
                            checked = settings.reduceMotion,
                            onCheckedChange = actions::setReduceMotion
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_notifications),
                            checked = settings.notificationsEnabled,
                            onCheckedChange = actions::setNotifications
                        )
                        Text(
                            text = stringResource(R.string.settings_notifications_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_clock_style)) {
                        ChoiceRow(
                            options = ClockStyle.entries.map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                            selected = settings.clockStyle,
                            onSelected = actions::setClockStyle
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_theme)) {
                        ChoiceRow(
                            options = AppTheme.entries.map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                            selected = settings.theme,
                            onSelected = actions::setTheme
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_character)) {
                        ChoiceRow(
                            options = MascotId.entries.map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                            selected = settings.character,
                            onSelected = actions::setCharacter
                        )
                    }

                    SettingsCard(title = stringResource(R.string.settings_about)) {
                        Text(
                            text = stringResource(R.string.settings_about_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                }
            }
        
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
        Column(modifier = Modifier.padding(Dimens.gapMedium)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (value, label) ->
                    val isSelected = value == selected
                    ArcadeButton(
                        text = label,
                        onClick = { onSelected(value) },
                        height = 52.dp,
                        modifier = Modifier.weight(1f),
                        topColor = if (isSelected) Palette.Mint else Palette.Silver,
                        bottomColor = if (isSelected) Palette.Ocean else Palette.InkSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(Dimens.gapSmall))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val MascotId.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
