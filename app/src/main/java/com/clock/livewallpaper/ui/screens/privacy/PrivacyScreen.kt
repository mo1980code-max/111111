package com.clock.livewallpaper.ui.screens.privacy

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.IconBadge
import com.clock.livewallpaper.ui.theme.LocalDhikrColors

/** The privacy screen states plainly what the app does not do - because it really does not. */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = listOf(
        R.string.privacy_point_local,
        R.string.privacy_point_backend,
        R.string.privacy_point_account,
        R.string.privacy_point_ads,
        R.string.privacy_point_analytics,
        R.string.privacy_point_overlay,
        R.string.privacy_point_permissions
    )

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.privacy_title), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            DhikrCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        iconRes = R.drawable.ic_privacy_lock,
                        container = MaterialTheme.colorScheme.surface
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_headline),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.privacy_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            DhikrCard {
                points.forEachIndexed { index, point ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    PrivacyPoint(text = stringResource(point))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.privacy_no_internet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PrivacyPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = LocalDhikrColors.current.positive,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
