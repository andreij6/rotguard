package com.rotguard.app.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.AppChoiceTile
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.SquareSegmentButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.floatingNavBarClearance
import com.rotguard.app.storage.HoldDurationMillis

private const val APP_GRID_COLUMNS = 2
private const val SQUARE_CORNER_DP = 6
private const val HOLD_3S_MS = 3_000L
private const val HOLD_4S_MS = 4_000L
private const val HOLD_6S_MS = 6_000L
private const val HOLD_10S_MS = 10_000L
private const val MILLIS_PER_SECOND = 1_000L

@Composable
fun GuardedAppsScreen(
    state: GuardedAppsUiState,
    onAction: (GuardedAppsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            GuardedAppsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is GuardedAppsUiState.Error -> GuardedAppsErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(GuardedAppsAction.RetryTapped) },
            )
            is GuardedAppsUiState.Ready -> GuardedAppsReadyContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun GuardedAppsReadyContent(
    state: GuardedAppsUiState.Ready,
    onAction: (GuardedAppsAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = 16.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apps_title),
                style = MaterialTheme.typography.headlineLarge,
                color = palette.ink,
            )
            SquareTagBadge(
                text = pluralStringResource(R.plurals.apps_active_count, state.guardedCount, state.guardedCount),
                accentColor = palette.mossAccent,
                containerColor = palette.sageTint,
            )
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(GuardedAppsAction.SearchQueryChanged(it)) },
            placeholder = { Text(stringResource(R.string.apps_search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.mossAccent,
                unfocusedBorderColor = palette.cardBorder,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
                unfocusedPlaceholderColor = palette.inkMuted,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        HoldDurationPresetRow(
            selectedDurationMillis = state.globalHoldDurationMillis,
            onSelect = { onAction(GuardedAppsAction.GlobalHoldDurationSelected(it)) },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(count = APP_GRID_COLUMNS),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            contentPadding = PaddingValues(bottom = floatingNavBarClearance()),
        ) {
            items(
                items = state.filteredApps,
                key = { it.packageName.value },
            ) { app ->
                GuardedAppTile(
                    app = app,
                    onToggle = { onAction(GuardedAppsAction.AppGuardToggled(app.packageName)) },
                )
            }
        }
    }
}

@Composable
private fun HoldDurationPresetRow(
    selectedDurationMillis: HoldDurationMillis,
    onSelect: (HoldDurationMillis) -> Unit,
) {
    val palette = VerdantTheme.colors
    val presets = listOf(HOLD_3S_MS, HOLD_4S_MS, HOLD_6S_MS, HOLD_10S_MS)

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            text = stringResource(R.string.apps_pause_length),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            presets.forEach { presetMs ->
                val seconds = (presetMs / MILLIS_PER_SECOND).toInt()
                SquareSegmentButton(
                    label = stringResource(R.string.apps_seconds_short, seconds),
                    selected = selectedDurationMillis.value == presetMs,
                    onClick = { onSelect(HoldDurationMillis(presetMs)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GuardedAppTile(
    app: GuardedAppRow,
    onToggle: () -> Unit,
) {
    val palette = VerdantTheme.colors
    AppChoiceTile(
        packageName = app.packageName,
        displayName = app.displayName,
        selected = app.isGuarded,
        statusText = if (app.isGuarded) {
            val seconds = app.effectiveHoldSeconds
            pluralStringResource(R.plurals.apps_hold_seconds, seconds, seconds)
        } else {
            stringResource(R.string.apps_not_paused)
        },
        statusColor = if (app.isGuarded) palette.mossAccent else palette.inkMuted,
        onToggle = onToggle,
        badgeText = if (app.isRecommended) stringResource(R.string.apps_suggested) else null,
    )
}

@Composable
private fun GuardedAppsErrorView(message: String, onRetry: () -> Unit) {
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
