package com.rotguard.app.feature.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.SquareSegmentButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.floatingNavBarClearance
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.ThemeMode

private const val SQUARE_CORNER_DP = 6
private const val HOLD_3S_MS = 3_000L
private const val HOLD_4S_MS = 4_000L
private const val HOLD_6S_MS = 6_000L
private const val HOLD_10S_MS = 10_000L
private const val GRACE_2M_MS = 120_000L
private const val GRACE_5M_MS = 300_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            SettingsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is SettingsUiState.Error -> SettingsErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(SettingsAction.RetryTapped) },
            )
            is SettingsUiState.Ready -> SettingsReadyContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun SettingsReadyContent(
    state: SettingsUiState.Ready,
    onAction: (SettingsAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = floatingNavBarClearance()),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
        )

        AppearanceThemeCard(
            selectedTheme = state.themeMode,
            onSelectTheme = { onAction(SettingsAction.ThemeModeSelected(it)) },
        )

        InspirationShortcutCard(
            photoCount = state.inspirationPhotoCount,
            reminderCount = state.reminderCount,
            onClick = { onAction(SettingsAction.OpenInspirationTapped) },
        )

        PermissionStatusCard(state = state, onAction = onAction)

        PausePreferencesCard(state = state, onAction = onAction)

        MantraEditorCard(
            draft = state.personalMantraDraft,
            savedHint = state.mantraSavedHintVisible,
            onDraftChanged = { onAction(SettingsAction.MantraDraftChanged(it)) },
            onSaveTapped = { onAction(SettingsAction.SaveMantraTapped) },
        )

        SquareOutlineButton(
            text = stringResource(R.string.settings_replay_onboarding),
            onClick = { onAction(SettingsAction.ReplayOnboardingTapped) },
        )

        Spacer(modifier = Modifier.height(height = 8.dp))
    }
}

@Composable
private fun AppearanceThemeCard(
    selectedTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
) {
    val palette = VerdantTheme.colors
    val options = listOf(
        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
    )

    BotanicalCard {
        Text(
            text = stringResource(R.string.settings_appearance_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            options.forEach { (mode, label) ->
                SquareSegmentButton(
                    label = label,
                    selected = mode == selectedTheme,
                    onClick = { onSelectTheme(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InspirationShortcutCard(
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_inspiration_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(
                        R.string.settings_inspiration_summary,
                        pluralStringResource(R.plurals.settings_photo_count, photoCount, photoCount),
                        pluralStringResource(R.plurals.settings_reminder_count, reminderCount, reminderCount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
            Text(
                text = stringResource(R.string.settings_open),
                style = MaterialTheme.typography.titleMedium,
                color = palette.mossAccent,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.mossAccent,
                modifier = Modifier.padding(start = 4.dp).size(size = 18.dp),
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    state: SettingsUiState.Ready,
    onAction: (SettingsAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    val activeBadge = stringResource(R.string.settings_badge_active)
    val enableBadge = stringResource(R.string.settings_badge_enable)
    BotanicalCard {
        Text(
            text = stringResource(R.string.settings_protection_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        PermissionRow(
            label = stringResource(R.string.settings_permission_detect),
            granted = state.accessibilityGranted,
            activeBadge = activeBadge,
            neededBadge = enableBadge,
            onClick = { onAction(SettingsAction.OpenAccessibilityTapped) },
        )
        PermissionRow(
            label = stringResource(R.string.settings_permission_pause),
            granted = state.overlayGranted,
            activeBadge = activeBadge,
            neededBadge = stringResource(R.string.settings_badge_allow),
            onClick = { onAction(SettingsAction.OpenOverlayTapped) },
        )
        PermissionRow(
            label = stringResource(R.string.settings_permission_insights),
            granted = state.usageStatsGranted,
            activeBadge = activeBadge,
            neededBadge = enableBadge,
            onClick = { onAction(SettingsAction.OpenUsageAccessTapped) },
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    activeBadge: String,
    neededBadge: String,
    onClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        SquareTagBadge(
            text = if (granted) activeBadge else neededBadge,
            accentColor = if (granted) palette.mossAccent else palette.terracotta,
            containerColor = if (granted) palette.sageTint else palette.terracottaSoft,
        )
    }
}

@Immutable
private data class CalibrationOptionGroup(
    val items: List<Pair<Long, String>>,
)

@Composable
private fun PausePreferencesCard(
    state: SettingsUiState.Ready,
    onAction: (SettingsAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    val holdOptions = CalibrationOptionGroup(
        listOf(HOLD_3S_MS, HOLD_4S_MS, HOLD_6S_MS, HOLD_10S_MS).map { millis ->
            millis to stringResource(R.string.settings_seconds_short, (millis / MILLIS_PER_SECOND).toInt())
        },
    )
    val graceOptions = CalibrationOptionGroup(
        listOf(
            0L to stringResource(R.string.settings_grace_every_open),
            GRACE_2M_MS to stringResource(R.string.settings_minutes_short, (GRACE_2M_MS / MILLIS_PER_MINUTE).toInt()),
            GRACE_5M_MS to stringResource(R.string.settings_minutes_short, (GRACE_5M_MS / MILLIS_PER_MINUTE).toInt()),
        ),
    )

    BotanicalCard {
        Text(
            text = stringResource(R.string.settings_pause_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        Text(
            text = stringResource(R.string.settings_pause_length),
            style = MaterialTheme.typography.titleMedium,
            color = palette.ink,
        )
        SegmentOptionRow(
            options = holdOptions,
            selectedValue = state.globalHoldDurationMillis.value,
            onSelect = { onAction(SettingsAction.HoldDurationSelected(HoldDurationMillis(it))) },
        )
        Text(
            text = stringResource(R.string.settings_reask),
            style = MaterialTheme.typography.titleMedium,
            color = palette.ink,
        )
        SegmentOptionRow(
            options = graceOptions,
            selectedValue = state.sessionGraceDurationMillis.value,
            onSelect = { onAction(SettingsAction.GraceWindowSelected(HoldDurationMillis(it))) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_vibration),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.hapticsEnabled,
                onCheckedChange = { onAction(SettingsAction.HapticsToggled) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.mossBtnInk,
                    checkedTrackColor = palette.mossAccent,
                ),
            )
        }
    }
}

@Composable
private fun SegmentOptionRow(
    options: CalibrationOptionGroup,
    selectedValue: Long,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        options.items.forEach { (value, label) ->
            SquareSegmentButton(
                label = label,
                selected = value == selectedValue,
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MantraEditorCard(
    draft: String,
    savedHint: Boolean,
    onDraftChanged: (String) -> Unit,
    onSaveTapped: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(warm = true) {
        Text(
            text = stringResource(R.string.settings_quick_add_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        OutlinedTextField(
            value = localizedStarterText(draft),
            onValueChange = onDraftChanged,
            singleLine = true,
            shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.mossAccent,
                unfocusedBorderColor = palette.warmBorder,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (savedHint) R.string.settings_mantra_saved else R.string.settings_mantra_more,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (savedHint) palette.mossAccent else palette.inkMuted,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
                color = palette.mossBtnFill,
                border = BorderStroke(width = 1.dp, color = palette.mossBtnStroke),
                onClick = onSaveTapped,
            ) {
                Text(
                    text = stringResource(R.string.settings_add),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.mossBtnInk,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsErrorView(message: String, onRetry: () -> Unit) {
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
        SquarePrimaryButton(
            text = stringResource(R.string.settings_retry),
            onClick = onRetry,
            useMossAccent = true,
        )
    }
}
