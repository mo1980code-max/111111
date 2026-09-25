package com.clockadventure.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.clockadventure.presentation.R
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

/**
 * The celebration sheet shown after a finished lesson or game.
 *
 * It is a native Android dialog with a confetti burst behind it, the earned stars and the rewards,
 * and only positive wording - there is no "you failed" state anywhere in this app.
 */
@Composable
fun CelebrationDialog(
    title: String,
    subtitle: String? = null,
    stars: Int = 0,
    showConfetti: Boolean = true,
    rewards: (@Composable () -> Unit)? = null,
    primaryText: String = stringResource(id = R.string.common_continue),
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onPrimary) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            if (showConfetti) {
                ConfettiOverlay(visible = true, modifier = Modifier.size(360.dp))
            }
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = Dimens.cornerLarge
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapMedium)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = appColors.accentDark,
                        textAlign = TextAlign.Center
                    )
                    if (stars > 0) {
                        StarRow(stars = stars, size = 40.dp)
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = appColors.content,
                            textAlign = TextAlign.Center
                        )
                    }
                    rewards?.invoke()
                    Spacer(modifier = Modifier.height(4.dp))
                    ArcadeButton(
                        text = primaryText,
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (secondaryText != null && onSecondary != null) {
                        ArcadeButton(
                            text = secondaryText,
                            onClick = onSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            height = 60.dp,
                            topColor = Color(0xFF6FC3FF),
                            bottomColor = Color(0xFF2E8BD6)
                        )
                    }
                    if (tertiaryText != null && onTertiary != null) {
                        ArcadeButton(
                            text = tertiaryText,
                            onClick = onTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            height = 60.dp,
                            topColor = Color(0xFFB9C7DE),
                            bottomColor = Color(0xFF8B9BB8)
                        )
                    }
                }
            }
        }
    }
}

/** Yes / no dialog used for "leave the lesson" and "reset progress". */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(id = R.string.common_ok),
    dismissText: String = stringResource(id = R.string.common_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.gapMedium)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = appColors.accentDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.content,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapMedium)) {
                    ArcadeButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        topColor = Color(0xFFB9C7DE),
                        bottomColor = Color(0xFF8B9BB8)
                    )
                    ArcadeButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Small toast-like bubble with the mascot's tip. */
@Composable
fun MascotBubble(text: String, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.padding(horizontal = Dimens.gapMedium),
        cornerRadius = Dimens.cornerMedium,
        elevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.gapMedium)
        ) {
            BulbIcon(modifier = Modifier.size(28.dp), tint = Palette.SunYellow)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.content,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
