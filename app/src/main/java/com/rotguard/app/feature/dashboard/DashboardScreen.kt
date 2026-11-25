package com.rotguard.app.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalArchedMedallion
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.BotanicalIllustration
import com.rotguard.app.designsystem.InspirationPhotoThumbnail
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.animatedCount
import com.rotguard.app.designsystem.floatingNavBarClearance
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.storage.BrainDecayStage
import java.util.Locale

private const val SQUARE_CORNER_DP = 6
private const val INSPIRATION_THUMB_DP = 60
private const val MAX_SHOWN_APP_NAMES = 4
private const val MANAGE_ARROW_DP = 14

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            DashboardUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is DashboardUiState.Error -> DashboardErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(DashboardAction.RetryTapped) },
            )
            is DashboardUiState.Ready -> DashboardReadyContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun DashboardReadyContent(
    state: DashboardUiState.Ready,
    onAction: (DashboardAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = floatingNavBarClearance()),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
    ) {
        DashboardTopRow(
            shieldEnabled = state.shieldEnabled,
            onToggleShield = { onAction(DashboardAction.ToggleShieldTapped) },
        )

        if (!state.isPermissionReady) {
            PermissionAlertBanner(
                onEnableClick = { onAction(DashboardAction.EnablePermissionsTapped) },
            )
        }

        if (state.appBreaks.isNotEmpty()) {
            AppBreaksCard(
                breaks = state.appBreaks,
                onResume = { onAction(DashboardAction.ResumePausingTapped(it)) },
            )
        }

        FocusHealthHeroCard(state = state)

        DashboardKpiRow(state = state)

        InspirationHomeCard(
            personalMantra = state.personalMantra,
            featuredPhoto = state.featuredInspirationPhoto,
            photoCount = state.inspirationPhotoCount,
            reminderCount = state.reminderCount,
            onClick = { onAction(DashboardAction.OpenInspirationTapped) },
        )

        BlockedAppsSummaryRow(
            summaryText = if (state.guardedAppNames.isEmpty()) {
                stringResource(R.string.dashboard_no_apps)
            } else {
                stringResource(
                    R.string.dashboard_paused_apps,
                    state.guardedAppNames.take(MAX_SHOWN_APP_NAMES).joinToString(" · "),
                )
            },
            onManageTapped = { onAction(DashboardAction.ManageAppsTapped) },
        )

        SquarePrimaryButton(
            text = stringResource(R.string.dashboard_test_blocker),
            onClick = { onAction(DashboardAction.TestShieldTapped) },
            useMossAccent = true,
        )

        Spacer(modifier = Modifier.height(height = 8.dp))
    }
}

@Composable
private fun DashboardTopRow(
    shieldEnabled: Boolean,
    onToggleShield: () -> Unit,
) {
    val palette = VerdantTheme.colors
    val statusColor = if (shieldEnabled) palette.mossAccent else palette.terracotta

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
        )
        Surface(
            modifier = Modifier.clickable(onClick = onToggleShield),
            shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
            color = if (shieldEnabled) palette.sageTint else palette.terracottaSoft,
            border = BorderStroke(width = 1.dp, color = statusColor),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = stringResource(
                        if (shieldEnabled) R.string.dashboard_status_active else R.string.dashboard_status_paused,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
        }
    }
}

@Composable
private fun PermissionAlertBanner(onEnableClick: () -> Unit) {
    val palette = VerdantTheme.colors
    BotanicalCard(
        borderColor = palette.terracotta,
        warm = true,
    ) {
        Text(
            text = stringResource(R.string.dashboard_setup_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.terracotta,
        )
        Text(
            text = stringResource(R.string.dashboard_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink,
        )
        SquareOutlineButton(
            text = stringResource(R.string.dashboard_setup_button),
            onClick = onEnableClick,
            borderColor = palette.terracotta,
            textColor = palette.terracotta,
        )
    }
}

@Composable
private fun FocusHealthHeroCard(state: DashboardUiState.Ready) {
    val palette = VerdantTheme.colors
    val stageSummaryRes = when (state.brainStage) {
        BrainDecayStage.THRIVING -> R.string.dashboard_stage_thriving
        BrainDecayStage.ALERT -> R.string.dashboard_stage_alert
        BrainDecayStage.STRAINED -> R.string.dashboard_stage_strained
        BrainDecayStage.CRACKING, BrainDecayStage.ROTTED -> R.string.dashboard_stage_reset
    }
    val minutesSaved = state.minutesReclaimedToday.value

    BotanicalCard(borderColor = palette.cardBorder) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            BotanicalArchedMedallion(
                illustration = BotanicalIllustration.TERRARIUM,
                contentDescription = stringResource(R.string.dashboard_medallion_description),
                size = 156.dp,
            )
            Text(
                text = stringResource(R.string.dashboard_health_percent, animatedCount(state.brainHealth.value)),
                style = MaterialTheme.typography.displaySmall,
                color = palette.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.dashboard_focus_health),
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_hero_summary,
                    minutesSaved,
                    stringResource(stageSummaryRes),
                    minutesSaved,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mossAccent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DashboardKpiRow(state: DashboardUiState.Ready) {
    val palette = VerdantTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        DashboardKpiTile(
            value = String.format(Locale.getDefault(), "%d", animatedCount(state.urgesResistedToday)),
            label = stringResource(R.string.dashboard_kpi_urges_stopped),
            valueColor = palette.mossAccent,
            modifier = Modifier.weight(1f),
        )
        DashboardKpiTile(
            value = stringResource(R.string.home_minutes_short, animatedCount(state.minutesReclaimedToday.value)),
            label = stringResource(R.string.dashboard_kpi_time_saved),
            valueColor = palette.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DashboardKpiTile(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val palette = VerdantTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(size = 14.dp),
        color = palette.card,
        border = BorderStroke(width = 1.dp, color = palette.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.inkMuted,
            )
        }
    }
}

@Composable
private fun InspirationHomeCard(
    personalMantra: String,
    featuredPhoto: DashboardInspirationPreview?,
    photoCount: Int,
    reminderCount: Int,
    onClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(
        warm = true,
        borderColor = palette.warmBorder,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InspirationPhotoThumbnail(
                filePath = featuredPhoto?.filePath,
                bundledDrawableName = featuredPhoto?.bundledDrawableName,
                contentDescription = featuredPhoto?.title?.let { localizedStarterText(it) }
                    ?: stringResource(R.string.dashboard_inspiration_fallback),
                modifier = Modifier.size(size = INSPIRATION_THUMB_DP.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_two_parts,
                            pluralStringResource(R.plurals.dashboard_photo_count, photoCount, photoCount),
                            pluralStringResource(R.plurals.dashboard_reminder_count, reminderCount, reminderCount),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mossAccent,
                    )
                    ManageLink()
                }
                Text(
                    text = stringResource(R.string.dashboard_mantra_quoted, localizedStarterText(personalMantra)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.ink,
                )
            }
        }
    }
}

/** "Manage" plus a forward arrow that flips in right-to-left languages. */
@Composable
private fun ManageLink() {
    val palette = VerdantTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_manage),
            style = MaterialTheme.typography.labelMedium,
            color = palette.inkMuted,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = palette.inkMuted,
            modifier = Modifier.size(size = MANAGE_ARROW_DP.dp),
        )
    }
}

@Composable
private fun BlockedAppsSummaryRow(
    summaryText: String,
    onManageTapped: () -> Unit,
) {
    val palette = VerdantTheme.colors

    BotanicalCard(onClick = onManageTapped) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.dashboard_edit),
                style = MaterialTheme.typography.titleMedium,
                color = palette.mossAccent,
            )
        }
    }
}

@Composable
private fun DashboardErrorView(message: String, onRetry: () -> Unit) {
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
