package com.clock.livewallpaper.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.ui.components.BrandMark
import com.clock.livewallpaper.ui.components.PrimaryButton
import com.clock.livewallpaper.ui.components.TextActionButton
import com.clock.livewallpaper.ui.theme.LocalDhikrColors
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val subtitleRes: Int
)

private val PAGES = listOf(
    OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_subtitle_1),
    OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_subtitle_2),
    OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_subtitle_3)
)

/**
 * Three quiet screens: what the app is, how the floating card behaves, and that everything stays
 * on the device. No permission is requested here - each one is asked for in the place where it is
 * actually used.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == PAGES.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextActionButton(
                text = stringResource(R.string.action_skip),
                iconRes = R.drawable.ic_chevron,
                onClick = { viewModel.complete(onFinished) },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(page = PAGES[page], index = page)
        }

        PageIndicator(
            current = pagerState.currentPage,
            total = PAGES.size,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = stringResource(if (lastPage) R.string.action_start else R.string.action_next),
            onClick = {
                if (lastPage) {
                    viewModel.complete(onFinished)
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, index: Int) {
    val extras = LocalDhikrColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // A different geometric halo per page: one, two, then three concentric rings.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rings = index + 1
                repeat(rings) { ring ->
                    val radius = size.minDimension / 2f * (0.58f + ring * 0.16f)
                    drawCircle(
                        color = extras.gold.copy(alpha = 0.30f - ring * 0.07f),
                        radius = radius,
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            BrandMark(size = 68.dp)
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.subtitleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    val description = stringResource(
        R.string.onboarding_page_indicator,
        ArabicText.digits(current + 1),
        ArabicText.digits(total)
    )
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val selected = index == current
            val width by animateDpAsState(
                targetValue = if (selected) 26.dp else 8.dp,
                animationSpec = tween(220),
                label = "indicatorWidth"
            )
            val color by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = tween(220),
                label = "indicatorColor"
            )
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
