package com.clock.livewallpaper.ads.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ads.AdsViewModel
import com.clock.livewallpaper.ui.components.SettingsDivider
import com.clock.livewallpaper.ui.components.SettingsNavRow

/**
 * The only advertising control in settings: Google's own privacy options form.
 *
 * It appears solely when the consent SDK reports that the user's region requires a permanent way
 * back into their choice - and it disappears with that requirement. There is no debug section, no
 * status readout and no test button anywhere in the app; what the row opens is Google's form, not
 * ours, so the recorded choice is the one the SDK itself keeps.
 */
@Composable
fun PrivacyOptionsRow(
    modifier: Modifier = Modifier,
    viewModel: AdsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.privacyOptionsRequired) return

    val context = LocalContext.current
    Column(modifier = modifier) {
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_ads_privacy_options),
            onClick = {
                val activity = context.findActivity()
                if (activity != null) viewModel.showPrivacyOptions(activity)
            },
            subtitle = stringResource(R.string.settings_ads_privacy_options_subtitle),
            iconRes = R.drawable.ic_shield
        )
    }
}

/** The form needs the Activity behind the Compose context; nothing else keeps a reference. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
