package com.clock.livewallpaper.ui.screens.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.prefs.OverlayPositionOption
import com.clock.livewallpaper.data.prefs.OverlaySettings
import com.clock.livewallpaper.data.prefs.OverlayStyleOption
import com.clock.livewallpaper.overlay.OverlayPermission
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.ChipFlowRow
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.InlineNote
import com.clock.livewallpaper.ui.components.OverlayPreviewCard
import com.clock.livewallpaper.ui.components.PermissionCard
import com.clock.livewallpaper.ui.components.PrimaryButton
import com.clock.livewallpaper.ui.components.SectionHeader
import com.clock.livewallpaper.ui.components.SelectableChip
import com.clock.livewallpaper.ui.components.SettingsFootnote
import com.clock.livewallpaper.ui.components.SettingsSection
import com.clock.livewallpaper.ui.components.SettingsSliderRow
import com.clock.livewallpaper.ui.components.SettingsSwitchRow
import kotlinx.coroutines.launch

/**
 * "مظهر بطاقة الذكر".
 *
 * The preview at the top is the real card composable with the real settings object, so style,
 * position, size, opacity and the dismissal hint are exactly what will appear over other apps.
 */
@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverlaySettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var overlayGranted by remember { mutableStateOf(OverlayPermission.canDraw(context)) }

    LifecycleResumeEffect(Unit) {
        overlayGranted = OverlayPermission.canDraw(context)
        onPauseOrDispose { }
    }

    val unavailableMessage = stringResource(R.string.overlay_unavailable)
    val needsPermissionMessage = stringResource(R.string.overlay_test_needs_permission)
    val previewText = state.previewText.ifBlank { stringResource(R.string.widget_empty) }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.overlay_settings_title), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionHeader(title = stringResource(R.string.overlay_preview_label))
            Spacer(Modifier.height(10.dp))
            OverlayPreviewCard(
                text = previewText,
                settings = state.overlay,
                description = stringResource(R.string.overlay_card_description)
            )

            Spacer(Modifier.height(16.dp))

            PrimaryButton(
                text = stringResource(R.string.overlay_test),
                iconRes = R.drawable.ic_card,
                onClick = {
                    if (!overlayGranted) {
                        scope.launch { snackbarHostState.showSnackbar(needsPermissionMessage) }
                    } else {
                        viewModel.showTestCard { shown ->
                            if (!shown) {
                                scope.launch { snackbarHostState.showSnackbar(unavailableMessage) }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            if (!overlayGranted) {
                PermissionCard(
                    iconRes = R.drawable.ic_shield,
                    title = stringResource(R.string.overlay_permission_title),
                    text = stringResource(R.string.overlay_permission_text),
                    note = stringResource(R.string.overlay_permission_privacy),
                    ctaText = stringResource(R.string.overlay_permission_cta),
                    onCta = {
                        runCatching {
                            context.startActivity(OverlayPermission.settingsIntent(context))
                        }
                    }
                )
                Spacer(Modifier.height(18.dp))
            }

            DhikrCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_overlay_enabled),
                    subtitle = stringResource(R.string.settings_overlay_enabled_desc),
                    iconRes = R.drawable.ic_card,
                    checked = state.overlay.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.setEnabled(enabled)
                        if (enabled && !overlayGranted) {
                            runCatching {
                                context.startActivity(OverlayPermission.settingsIntent(context))
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = stringResource(R.string.overlay_style)) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    ChipFlowRow {
                        OverlayStyleOption.entries.forEach { style ->
                            SelectableChip(
                                label = stringResource(style.labelRes),
                                selected = state.overlay.style == style,
                                onClick = { viewModel.setStyle(style) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = stringResource(R.string.overlay_position)) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    ChipFlowRow {
                        OverlayPositionOption.entries.forEach { position ->
                            SelectableChip(
                                label = stringResource(position.labelRes),
                                selected = state.overlay.position == position,
                                onClick = { viewModel.setPosition(position) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = stringResource(R.string.overlay_font_size)) {
                SettingsSliderRow(
                    title = stringResource(R.string.overlay_font_size),
                    iconRes = R.drawable.ic_text_size,
                    value = state.overlay.fontScale,
                    valueLabel = ArabicText.digits((state.overlay.fontScale * 100).toInt()),
                    valueRange = OverlaySettings.MIN_FONT_SCALE..OverlaySettings.MAX_FONT_SCALE,
                    steps = 9,
                    onValueChange = viewModel::setFontScale
                )
                SettingsSliderRow(
                    title = stringResource(R.string.overlay_opacity),
                    iconRes = R.drawable.ic_opacity,
                    value = state.overlay.opacity,
                    valueLabel = ArabicText.digits((state.overlay.opacity * 100).toInt()),
                    valueRange = OverlaySettings.MIN_OPACITY..OverlaySettings.MAX_OPACITY,
                    steps = 7,
                    onValueChange = viewModel::setOpacity
                )
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = stringResource(R.string.overlay_duration)) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    ChipFlowRow {
                        OverlaySettings.AUTO_DISMISS_OPTIONS.forEach { seconds ->
                            SelectableChip(
                                label = if (seconds == 0) {
                                    stringResource(R.string.overlay_duration_touch)
                                } else {
                                    stringResource(
                                        R.string.overlay_duration_seconds,
                                        ArabicText.digits(seconds)
                                    )
                                },
                                selected = state.overlay.autoDismissSeconds == seconds,
                                onClick = { viewModel.setAutoDismiss(seconds) }
                            )
                        }
                    }
                }
                SettingsFootnote(text = stringResource(R.string.overlay_dismiss_hint))
            }

            Spacer(Modifier.height(18.dp))

            DhikrCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.overlay_haptic),
                    iconRes = R.drawable.ic_vibration,
                    checked = state.overlay.haptic,
                    onCheckedChange = viewModel::setHaptic
                )
            }

            Spacer(Modifier.height(16.dp))

            InlineNote(text = stringResource(R.string.overlay_permission_privacy))

            Spacer(Modifier.height(32.dp))
        }
    }
}
