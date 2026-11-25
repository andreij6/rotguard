package com.rotguard.app.feature.settings

import androidx.annotation.StringRes
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.ThemeMode

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
        val usageStatsGranted: Boolean,
        val globalHoldDurationMillis: HoldDurationMillis,
        val sessionGraceDurationMillis: HoldDurationMillis,
        val hapticsEnabled: Boolean,
        val personalMantraDraft: String,
        val mantraSavedHintVisible: Boolean,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val inspirationPhotoCount: Int = 0,
        val reminderCount: Int = 0,
    ) : SettingsUiState

    data class Error(@StringRes val messageRes: Int) : SettingsUiState
}

sealed interface SettingsAction {
    data object RefreshRequested : SettingsAction
    data object OpenAccessibilityTapped : SettingsAction
    data object OpenOverlayTapped : SettingsAction
    data object OpenUsageAccessTapped : SettingsAction
    data class HoldDurationSelected(val durationMillis: HoldDurationMillis) : SettingsAction
    data class GraceWindowSelected(val durationMillis: HoldDurationMillis) : SettingsAction
    data object HapticsToggled : SettingsAction
    data class ThemeModeSelected(val themeMode: ThemeMode) : SettingsAction
    data object OpenInspirationTapped : SettingsAction
    data class MantraDraftChanged(val mantraText: String) : SettingsAction
    data object SaveMantraTapped : SettingsAction
    data object ReplayOnboardingTapped : SettingsAction
    data object RetryTapped : SettingsAction
}

sealed interface SettingsEffect {
    data object OpenAccessibilitySettings : SettingsEffect
    data object OpenOverlaySettings : SettingsEffect
    data object OpenUsageAccessSettings : SettingsEffect
    data object NavigateToInspiration : SettingsEffect
    data object ReplayOnboarding : SettingsEffect
}

sealed interface SettingsInternalEvent {
    data object LoadStarted : SettingsInternalEvent
    data class Loaded(val snapshot: SettingsSnapshot) : SettingsInternalEvent
    data class PermissionsUpdated(
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
        val usageStatsGranted: Boolean,
    ) : SettingsInternalEvent
    data class HoldDurationUpdated(val durationMillis: HoldDurationMillis) : SettingsInternalEvent
    data class GraceWindowUpdated(val durationMillis: HoldDurationMillis) : SettingsInternalEvent
    data class HapticsUpdated(val enabled: Boolean) : SettingsInternalEvent
    data class ThemeModeUpdated(val themeMode: ThemeMode) : SettingsInternalEvent
    data class MantraDraftUpdated(val text: String) : SettingsInternalEvent
    data object MantraSaved : SettingsInternalEvent
    data class LoadFailed(@StringRes val messageRes: Int) : SettingsInternalEvent
}

data class SettingsSnapshot(
    val accessibilityGranted: Boolean,
    val overlayGranted: Boolean,
    val usageStatsGranted: Boolean,
    val globalHoldDurationMillis: HoldDurationMillis,
    val sessionGraceDurationMillis: HoldDurationMillis,
    val hapticsEnabled: Boolean,
    val personalMantra: String,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val inspirationPhotoCount: Int = 0,
    val reminderCount: Int = 0,
)
