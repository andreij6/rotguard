package com.rotguard.app.feature.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.InstalledAppIcon
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.animatedCount
import com.rotguard.app.designsystem.floatingNavBarClearance
import java.time.format.TextStyle
import java.util.Locale

private const val MAX_PERCENT_FLOAT = 100f
private const val MIN_BAR_RATIO = 0.08f
private const val HEALTHY_BAR_THRESHOLD = 80
private const val MEDIUM_BAR_THRESHOLD = 50
private const val BAR_CORNER_RADIUS_DP = 8
private const val DUEL_BOX_CORNER_DP = 10

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onAction: (InsightsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            InsightsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is InsightsUiState.Error -> InsightsErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(InsightsAction.RetryTapped) },
            )
            is InsightsUiState.Ready -> InsightsReadyContent(state = state)
        }
    }
}

@Composable
private fun InsightsReadyContent(state: InsightsUiState.Ready) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = floatingNavBarClearance()),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.insights_title),
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
        )

        FocusDuelCard(state = state)

        WeeklyHealthTrendCard(state = state)

        AppInterceptBreakdownCard(state = state)
    }
}

@Composable
private fun FocusDuelCard(state: InsightsUiState.Ready) {
    val palette = VerdantTheme.colors
    val verdictColor = if (state.isWinningDuel) palette.mossAccent else palette.terracotta

    BotanicalCard(warm = true, borderColor = palette.warmBorder) {
        Text(
            text = stringResource(R.string.insights_duel_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        Text(
            text = stringResource(
                if (state.isWinningDuel) R.string.insights_duel_ahead else R.string.insights_duel_behind,
            ),
            style = MaterialTheme.typography.headlineMedium,
            color = verdictColor,
        )
        Text(
            text = stringResource(R.string.insights_duel_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        ) {
            DuelScoreBox(
                score = formatCount(animatedCount(state.yourBypassesToday)),
                label = stringResource(R.string.insights_your_opens),
                highlighted = state.isWinningDuel,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.insights_vs),
                style = MaterialTheme.typography.labelMedium,
                color = palette.inkMuted,
            )
            DuelScoreBox(
                score = formatCount(animatedCount(state.rivalDailyTargetCount)),
                label = stringResource(R.string.insights_daily_avg),
                highlighted = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DuelScoreBox(
    score: String,
    label: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = VerdantTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(size = DUEL_BOX_CORNER_DP.dp),
        color = if (highlighted) palette.sageTint else palette.card,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) palette.mossAccent else palette.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = score,
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlighted) palette.ink else palette.terracotta,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) palette.mossAccent else palette.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeeklyHealthTrendCard(
    state: InsightsUiState.Ready,
) {
    val palette = VerdantTheme.colors
    val trend = state.weeklyTrend
    val totalMinutesSaved = state.totalMinutesSavedThisWeek.value
    BotanicalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_trend_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
            SquareTagBadge(
                text = pluralStringResource(
                    R.plurals.insights_week_minutes_saved,
                    totalMinutesSaved,
                    totalMinutesSaved,
                ),
                accentColor = palette.mossAccent,
                containerColor = palette.sageTint,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            trend.forEach { day ->
                val fillRatio = (day.brainHealth.value / MAX_PERCENT_FLOAT).coerceIn(MIN_BAR_RATIO, 1f)
                val barColor = when {
                    day.brainHealth.value >= HEALTHY_BAR_THRESHOLD -> palette.mossAccent
                    day.brainHealth.value >= MEDIUM_BAR_THRESHOLD -> palette.sunGold
                    else -> palette.terracotta
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(width = 26.dp)
                            .height(height = 110.dp)
                            .clip(RoundedCornerShape(size = BAR_CORNER_RADIUS_DP.dp))
                            .background(palette.sageTint),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillRatio)
                                .clip(RoundedCornerShape(size = BAR_CORNER_RADIUS_DP.dp))
                                .background(barColor),
                        )
                    }
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInterceptBreakdownCard(state: InsightsUiState.Ready) {
    val palette = VerdantTheme.colors
    val items = state.appBreakdowns
    BotanicalCard {
        Text(
            text = stringResource(R.string.insights_most_closed),
            style = MaterialTheme.typography.titleLarge,
            color = palette.ink,
        )
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.insights_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
            )
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InstalledAppIcon(
                        packageName = item.packageName,
                        displayName = item.appDisplayName,
                        size = 36.dp,
                    )
                    val closedText =
                        pluralStringResource(R.plurals.insights_closed_count, item.resistedCount, item.resistedCount)
                    val openedText =
                        pluralStringResource(R.plurals.insights_opened_count, item.bypassedCount, item.bypassedCount)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.appDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink,
                        )
                        Text(
                            text = stringResource(R.string.home_two_parts, closedText, openedText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.inkMuted,
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.insights_app_minutes_saved,
                            item.minutesSaved.value,
                            item.minutesSaved.value,
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.mossAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsErrorView(message: String, onRetry: () -> Unit) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = palette.terracotta)
        Spacer(modifier = Modifier.height(height = 16.dp))
        SquarePrimaryButton(text = stringResource(R.string.home_retry), onClick = onRetry, useMossAccent = true)
    }
}

private fun formatCount(count: Int): String = String.format(Locale.getDefault(), "%d", count)
