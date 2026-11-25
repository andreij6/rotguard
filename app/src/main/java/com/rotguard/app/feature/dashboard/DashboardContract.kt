package com.rotguard.app.feature.dashboard

import androidx.annotation.StringRes
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.DailyScreenMinutes

data class DashboardInspirationPreview(
    val title: String,
    val caption: String,
    val filePath: String? = null,
    val bundledDrawableName: String? = null,
)

/** An app the user put on a break from a repeat pause screen; it won't be paused until [untilEpochMillis]. */
data class AppBreak(
    val packageName: String,
    val appName: String,
    val untilEpochMillis: Long,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Ready(
        val shieldEnabled: Boolean,
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
        val brainHealth: BrainHealthPercent,
        val brainStage: BrainDecayStage,
        val urgesResistedToday: Int,
        val urgesBypassedToday: Int,
        val minutesReclaimedToday: DailyScreenMinutes,
        val guardedAppNames: List<String>,
        val personalMantra: String,
        val featuredInspirationPhoto: DashboardInspirationPreview? = null,
        val inspirationPhotoCount: Int = 0,
        val reminderCount: Int = 0,
        val appBreaks: List<AppBreak> = emptyList(),
    ) : DashboardUiState {
        val isPermissionReady: Boolean
            get() = accessibilityGranted && overlayGranted
    }

    data class Error(@StringRes val messageRes: Int) : DashboardUiState
}

sealed interface DashboardAction {
    data object RefreshRequested : DashboardAction
    data object ToggleShieldTapped : DashboardAction
    data object TestShieldTapped : DashboardAction
    data object EnablePermissionsTapped : DashboardAction
    data object ManageAppsTapped : DashboardAction
    data object OpenInspirationTapped : DashboardAction
    data object RetryTapped : DashboardAction
    data class ResumePausingTapped(val packageName: String) : DashboardAction
}

sealed interface DashboardEffect {
    data object LaunchDemoShield : DashboardEffect
    data object NavigateToSettingsPermissions : DashboardEffect
    data object NavigateToGuardedApps : DashboardEffect
    data object NavigateToInspiration : DashboardEffect
}

sealed interface DashboardInternalEvent {
    data object LoadStarted : DashboardInternalEvent
    data class SnapshotLoaded(val snapshot: DashboardSnapshot) : DashboardInternalEvent
    data class ShieldToggled(val enabled: Boolean) : DashboardInternalEvent
    data class LoadFailed(@StringRes val messageRes: Int) : DashboardInternalEvent
}

data class DashboardSnapshot(
    val shieldEnabled: Boolean,
    val accessibilityGranted: Boolean,
    val overlayGranted: Boolean,
    val urgesResistedToday: Int,
    val urgesBypassedToday: Int,
    val minutesReclaimedToday: DailyScreenMinutes,
    val guardedAppNames: List<String>,
    val personalMantra: String,
    val featuredInspirationPhoto: DashboardInspirationPreview? = null,
    val inspirationPhotoCount: Int = 0,
    val reminderCount: Int = 0,
    val appBreaks: List<AppBreak> = emptyList(),
)
