package com.clockadventure.presentation.screens.rewards

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.UnlockState
import com.clockadventure.domain.model.UnlockableType
import com.clockadventure.presentation.R
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.CoinIcon
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.LockIcon
import com.clockadventure.presentation.components.Pill
import com.clockadventure.presentation.components.ProgressBar
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.TrophyIcon
import com.clockadventure.presentation.components.ContentColumn
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

@Composable
fun RewardsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RewardsScreen(
        state = state,
        onBack = onBack,
        onClaimDaily = viewModel::claimDailyReward,
        onBuy = viewModel::buy,
        onEquip = viewModel::equip,
        modifier = modifier
    )
}

@Composable
internal fun RewardsScreen(
    state: RewardsUiState,
    onBack: () -> Unit,
    onClaimDaily: () -> Unit,
    onBuy: (String) -> Unit,
    onEquip: (com.clockadventure.domain.model.Unlockable) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            ContentColumn {

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.rewards_title),
                    subtitle = stringResource(R.string.rewards_subtitle),
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                ) {
                    // ---- daily gift ------------------------------------------------------
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(Dimens.gapMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrophyIcon(tint = Palette.Gold, modifier = Modifier.size(Dimens.minTouchTarget))
                            Spacer(modifier = Modifier.width(Dimens.gapSmall))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.rewards_daily_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.dailyClaimed) {
                                        stringResource(R.string.rewards_daily_claimed)
                                    } else {
                                        stringResource(R.string.rewards_daily_body)
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(R.string.rewards_streak, state.progress.streakDays),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.accent
                                )
                            }
                            if (!state.dailyClaimed) {
                                ArcadeButton(
                                    text = stringResource(R.string.rewards_daily_claim),
                                    onClick = onClaimDaily,
                                    height = Dimens.minTouchTarget,
                                    modifier = Modifier.width(120.dp),
                                    topColor = Palette.SunYellow,
                                    bottomColor = Palette.Orange
                                )
                            }
                        }
                    }

                    // ---- achievements ----------------------------------------------------
                    Text(
                        text = stringResource(R.string.rewards_achievements),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        state.achievements.forEach { item ->
                            AchievementChip(item = item, language = lang)
                        }
                    }

                    // ---- shop -------------------------------------------------------------
                    Text(
                        text = stringResource(R.string.rewards_shop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.rewards_shop_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = stringResource(R.string.rewards_cost, state.progress.coins),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    state.unlocks.forEach { unlock ->
                        ShopRow(
                            unlock = unlock,
                            language = lang,
                            coins = state.progress.coins,
                            stars = state.progress.stars,
                            equipped = isEquipped(state.settings, unlock.unlockable.refId),
                            onBuy = { onBuy(unlock.unlockable.id) },
                            onEquip = { onEquip(unlock.unlockable) }
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                }
            }
        
            }
        }
    }
}

private fun isEquipped(
    settings: com.clockadventure.domain.model.AppSettings,
    refId: String
): Boolean = settings.clockStyle.name == refId ||
    settings.theme.name == refId ||
    settings.character.name == refId

@Composable
private fun AchievementChip(item: AchievementState, language: com.clockadventure.domain.model.AppLanguage) {
    val color = if (item.unlocked) Palette.Gold else Color(0xFFB8C2D6)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Dimens.minTouchTarget)) {
            if (item.unlocked) {
                TrophyIcon(tint = color)
            } else {
                LockIcon(tint = Color(0xFF9AA7C7))
            }
        }
        Text(
            text = item.achievement.title[language],
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (!item.unlocked) {
            ProgressBar(progress = item.progressFraction, height = 8.dp)
        }
    }
}

@Composable
private fun ShopRow(
    unlock: UnlockState,
    language: com.clockadventure.domain.model.AppLanguage,
    coins: Int,
    stars: Int,
    equipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val item = unlock.unlockable
    val affordable = coins >= item.coinCost
    val starsOk = stars >= item.requiredStars
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Dimens.cornerMedium) {
        Row(modifier = Modifier.padding(Dimens.gapMedium), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name[language],
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description[language],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(tint = Palette.Gold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.rewards_cost, item.coinCost),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (item.requiredStars > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.rewards_need_stars, item.requiredStars),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (starsOk) Palette.Mint else Palette.Coral
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(Dimens.gapSmall))
            when {
                equipped -> Pill(text = stringResource(R.string.rewards_equipped), containerColor = Palette.Mint)
                !unlock.unlocked -> ArcadeButton(
                    text = stringResource(R.string.rewards_buy),
                    onClick = onBuy,
                    enabled = affordable && starsOk,
                    height = Dimens.minTouchTarget,
                    modifier = Modifier.width(110.dp)
                )
                else -> {
                    val label = when (item.type) {
                        UnlockableType.CLOCK_STYLE, UnlockableType.THEME, UnlockableType.CHARACTER ->
                            stringResource(R.string.rewards_equip)
                    }
                    ArcadeButton(
                        text = label,
                        onClick = onEquip,
                        height = Dimens.minTouchTarget,
                        modifier = Modifier.width(110.dp),
                        topColor = Palette.Ocean,
                        bottomColor = Color(0xFF1560C0)
                    )
                }
            }
        }
    }
}
