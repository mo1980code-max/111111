package com.clock.livewallpaper.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.BuildConfig
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.BrandMark
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.SecondaryButton

/** About: what the app is, where its content comes from and which fonts are bundled. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.about_title), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            BrandMark(size = 64.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.about_version,
                    ArabicText.digits(BuildConfig.VERSION_NAME)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.about_purpose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            AboutCard(
                title = stringResource(R.string.about_content_title),
                body = stringResource(R.string.about_content_note)
            )
            Spacer(Modifier.height(14.dp))
            AboutCard(
                title = stringResource(R.string.about_fonts_title),
                body = stringResource(R.string.about_fonts_note)
            )
            Spacer(Modifier.height(14.dp))
            AboutCard(
                title = stringResource(R.string.about_offline_title),
                body = stringResource(R.string.about_offline_note)
            )

            Spacer(Modifier.height(22.dp))

            SecondaryButton(
                text = stringResource(R.string.privacy_title),
                onClick = onOpenPrivacy,
                iconRes = R.drawable.ic_privacy_lock,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AboutCard(title: String, body: String) {
    DhikrCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
