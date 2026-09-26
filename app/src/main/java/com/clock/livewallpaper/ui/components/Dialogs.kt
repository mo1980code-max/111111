package com.clock.livewallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.prefs.ReminderSettings

/** Destructive or plain confirmation. The confirm action is always the explicit Arabic verb. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = stringResource(R.string.action_cancel)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * Numeric prompt used for the custom tasbeeh target.
 *
 * Accepts Arabic-Indic and ASCII digits, validates while typing and keeps the confirm action
 * disabled until the value is inside the allowed range.
 */
@Composable
fun NumberPromptDialog(
    title: String,
    hint: String,
    initialValue: Int,
    min: Int,
    max: Int,
    errorText: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(ArabicText.digits(initialValue)) }
    val parsed = ArabicText.parseNumber(input)
    val valid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    isError = input.isNotEmpty() && !valid,
                    label = { Text(hint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (input.isNotEmpty() && !valid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { parsed?.let(onConfirm) }
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** Custom reminder interval: hours and minutes, clamped to the scheduler's real limits. */
@Composable
fun IntervalPromptDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hours by remember { mutableIntStateOf(initialMinutes / 60) }
    var minutes by remember { mutableIntStateOf(initialMinutes % 60) }
    val total = hours * 60 + minutes
    val valid = total in ReminderSettings.MIN_MINUTES..ReminderSettings.MAX_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.settings_reminder_custom_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                NumberStepper(
                    label = stringResource(R.string.settings_reminder_custom_hours),
                    valueText = ArabicText.digits(hours),
                    onDecrease = { hours = (hours - 1).coerceAtLeast(0) },
                    onIncrease = { hours = (hours + 1).coerceAtMost(12) }
                )
                Spacer(Modifier.height(10.dp))
                NumberStepper(
                    label = stringResource(R.string.settings_reminder_custom_minutes),
                    valueText = ArabicText.digits(minutes),
                    onDecrease = { minutes = if (minutes - 5 < 0) 55 else minutes - 5 },
                    onIncrease = { minutes = if (minutes + 5 > 55) 0 else minutes + 5 }
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (valid) {
                        stringResource(R.string.home_reminder_every, ArabicText.duration(context, total))
                    } else {
                        stringResource(
                            R.string.settings_reminder_custom_error,
                            ArabicText.duration(context, ReminderSettings.MIN_MINUTES),
                            ArabicText.duration(context, ReminderSettings.MAX_MINUTES)
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(total) }) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** Time of day picker built from steppers - hours wrap 0..23, minutes move in five minute steps. */
@Composable
fun TimePromptDialog(
    title: String,
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val start = ((initialMinuteOfDay % ArabicText.MINUTES_PER_DAY) + ArabicText.MINUTES_PER_DAY) %
        ArabicText.MINUTES_PER_DAY
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(start / 60) }
    var minute by remember { mutableIntStateOf((start % 60) / 5 * 5) }
    val minuteOfDay = hour * 60 + minute

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ArabicText.time(context, minuteOfDay),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(16.dp))
                NumberStepper(
                    label = stringResource(R.string.settings_reminder_custom_hours),
                    valueText = ArabicText.digits(hour),
                    onDecrease = { hour = if (hour == 0) 23 else hour - 1 },
                    onIncrease = { hour = if (hour == 23) 0 else hour + 1 }
                )
                Spacer(Modifier.height(10.dp))
                NumberStepper(
                    label = stringResource(R.string.settings_reminder_custom_minutes),
                    valueText = ArabicText.digits(minute),
                    onDecrease = { minute = if (minute - 5 < 0) 55 else minute - 5 },
                    onIncrease = { minute = if (minute + 5 > 55) 0 else minute + 5 }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minuteOfDay) }) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** Minus / value / plus row. Both buttons are full 48dp targets and are labelled for TalkBack. */
@Composable
fun NumberStepper(
    label: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconActionButton(
                iconRes = R.drawable.ic_minus,
                contentDescription = stringResource(R.string.cd_decrease),
                onClick = onDecrease
            )
            Box(
                modifier = Modifier.width(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconActionButton(
                iconRes = R.drawable.ic_add,
                contentDescription = stringResource(R.string.cd_increase),
                onClick = onIncrease
            )
        }
    }
}
