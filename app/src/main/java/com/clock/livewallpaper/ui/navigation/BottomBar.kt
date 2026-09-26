package com.clock.livewallpaper.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ui.components.Hairline

/** The four tabs of the bottom bar, in reading order for an RTL layout. */
enum class BottomDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    HOME(Routes.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    ADHKAR(Routes.ADHKAR, R.string.nav_adhkar, R.drawable.ic_nav_adhkar),
    TASBEEH(Routes.TASBEEH, R.string.nav_tasbeeh, R.drawable.ic_nav_tasbeeh),
    MY_DHIKR(Routes.MY_DHIKR, R.string.nav_my_dhikr, R.drawable.ic_nav_my_dhikr)
}

/**
 * Bottom navigation.
 *
 * Material 3 `NavigationBar` handles the gesture-bar inset and the 48dp targets; the palette and
 * the hairline above it are ours, so it reads as part of the product rather than as a default.
 */
@Composable
fun DhikrBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Hairline()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            BottomDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(destination.route) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
