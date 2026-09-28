// See project-surface.kt: signatures copied verbatim from ui/components/.
package com.clock.livewallpaper.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun DhikrCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(26.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    bordered: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) = Unit

@Composable
fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null
) = Unit

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) = Unit
