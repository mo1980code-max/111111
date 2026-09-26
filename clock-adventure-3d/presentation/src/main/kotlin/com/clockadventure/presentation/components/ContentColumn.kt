package com.clockadventure.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clockadventure.presentation.theme.Dimens

/**
 * The content column every screen lives in.
 *
 * On a phone it simply fills the screen. On a tablet or a foldable it stops at [Dimens.maxContentWidth]
 * and stays centred, so buttons never stretch into unreadable ribbons and the game keeps the same
 * proportions the layouts were designed for.
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = Dimens.maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}
