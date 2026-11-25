package com.rotguard.app.feature.settings

/**
 * Pure deterministic reducer for Settings, Appearance Theme, Permissions, and Pause Preferences.
 *
 * **Responsibility & Boundaries**
 * - Transforms [SettingsUiState] across load, permission refresh, theme selection,
 *   pause timing preferences, haptic toggling, and personal reminder edits.
 *
 * **Side Effects**
 * - None. Must never open System Settings intents, access Room DAOs, or fire haptics.
 *
 * **Invariants**
 * - `personalMantraDraft` is capped at 80 characters to remain legible inside the pause card.
 *
 * **Anti-Patterns**
 * - Never launch intents or read `LocalContext` inside [reduce].
 */
object SettingsReducer {

    private const val MAX_MANTRA_CHARS = 80

    fun reduce(state: SettingsUiState, event: SettingsInternalEvent): SettingsUiState {
        return when (event) {
            SettingsInternalEvent.LoadStarted -> SettingsUiState.Loading
            is SettingsInternalEvent.LoadFailed -> SettingsUiState.Error(event.messageRes)
            is SettingsInternalEvent.Loaded -> SettingsUiState.Ready(
                accessibilityGranted = event.snapshot.accessibilityGranted,
                overlayGranted = event.snapshot.overlayGranted,
                usageStatsGranted = event.snapshot.usageStatsGranted,
                globalHoldDurationMillis = event.snapshot.globalHoldDurationMillis,
                sessionGraceDurationMillis = event.snapshot.sessionGraceDurationMillis,
                hapticsEnabled = event.snapshot.hapticsEnabled,
                personalMantraDraft = event.snapshot.personalMantra,
                mantraSavedHintVisible = false,
                themeMode = event.snapshot.themeMode,
                inspirationPhotoCount = event.snapshot.inspirationPhotoCount,
                reminderCount = event.snapshot.reminderCount,
            )
            else -> reduceReadyState(state, event)
        }
    }

    private fun reduceReadyState(
        state: SettingsUiState,
        event: SettingsInternalEvent,
    ): SettingsUiState {
        val ready = state as? SettingsUiState.Ready ?: return state
        return when (event) {
            is SettingsInternalEvent.PermissionsUpdated -> ready.copy(
                accessibilityGranted = event.accessibilityGranted,
                overlayGranted = event.overlayGranted,
                usageStatsGranted = event.usageStatsGranted,
            )
            is SettingsInternalEvent.HoldDurationUpdated -> ready.copy(
                globalHoldDurationMillis = event.durationMillis,
            )
            is SettingsInternalEvent.GraceWindowUpdated -> ready.copy(
                sessionGraceDurationMillis = event.durationMillis,
            )
            is SettingsInternalEvent.HapticsUpdated -> ready.copy(
                hapticsEnabled = event.enabled,
            )
            is SettingsInternalEvent.ThemeModeUpdated -> ready.copy(
                themeMode = event.themeMode,
            )
            is SettingsInternalEvent.MantraDraftUpdated -> ready.copy(
                personalMantraDraft = event.text.take(MAX_MANTRA_CHARS),
                mantraSavedHintVisible = false,
            )
            SettingsInternalEvent.MantraSaved -> ready.copy(
                mantraSavedHintVisible = true,
            )
            SettingsInternalEvent.LoadStarted,
            is SettingsInternalEvent.Loaded,
            is SettingsInternalEvent.LoadFailed,
            -> ready
        }
    }
}
