package com.clock.livewallpaper.ui.screens.adhkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.IconBadge
import com.clock.livewallpaper.ui.components.PrimaryButton

/** The categories screen: only the sections that really carry local content are listed. */
@Composable
fun AdhkarScreen(
    onOpenCategory: (DhikrCategory) -> Unit,
    onAddDhikr: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdhkarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "title") {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.adhkar_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.adhkar_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = DhikrCategory.READING,
            key = { category -> category.key }
        ) { category ->
            CategoryRow(
                category = category,
                count = state.counts[category.key] ?: 0,
                onClick = { onOpenCategory(category) }
            )
        }

        item(key = "add") {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = stringResource(R.string.my_dhikr_add),
                onClick = onAddDhikr,
                iconRes = R.drawable.ic_add,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: DhikrCategory,
    count: Int,
    onClick: () -> Unit
) {
    val title = stringResource(category.titleRes)
    DhikrCard(
        onClick = onClick,
        onClickLabel = title,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconRes = category.iconRes, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.category_item_count, ArabicText.digits(count)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
