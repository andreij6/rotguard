package com.rotguard.app.feature.dashboard

import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent

/**
 * Pure deterministic reducer for the Home Focus Health dashboard.
 *
 * **Responsibility & Boundaries**
 * - Maps loaded [DashboardSnapshot] metrics into [DashboardUiState.Ready] including derived
 *   Focus Health percentage, stage, and Inspiration card summary.
 *
 * **Side Effects**
 * - None. Must never access Room DAOs, Android Context, or haptics.
 *
 * **Invariants**
 * - `BrainHealthPercent` is bounded strictly within `5..100`.
 * - Empty event history starts at a healthy `85%` baseline.
 *
 * **Anti-Patterns**
 * - Never represent mutually exclusive loading/ready/error states with separate booleans.
 */
object DashboardReducer {

    private const val BASE_HEALTH = 85
    private const val RESISTED_GAIN = 3
    private const val BYPASSED_DROP = 12
    private const val MIN_HEALTH = 5
    private const val MAX_HEALTH = 100

    fun reduce(state: DashboardUiState, event: DashboardInternalEvent): DashboardUiState {
        return when (event) {
            DashboardInternalEvent.LoadStarted -> DashboardUiState.Loading
            is DashboardInternalEvent.LoadFailed -> DashboardUiState.Error(event.messageRes)
            is DashboardInternalEvent.SnapshotLoaded -> mapSnapshotToReady(event.snapshot)
            is DashboardInternalEvent.ShieldToggled -> {
                val ready = state as? DashboardUiState.Ready ?: return state
                ready.copy(shieldEnabled = event.enabled)
            }
        }
    }

    private fun mapSnapshotToReady(snapshot: DashboardSnapshot): DashboardUiState.Ready {
        val rawHealth = BASE_HEALTH +
            (snapshot.urgesResistedToday * RESISTED_GAIN) -
            (snapshot.urgesBypassedToday * BYPASSED_DROP)
        val health = BrainHealthPercent(rawHealth.coerceIn(MIN_HEALTH, MAX_HEALTH))
        return DashboardUiState.Ready(
            shieldEnabled = snapshot.shieldEnabled,
            accessibilityGranted = snapshot.accessibilityGranted,
            overlayGranted = snapshot.overlayGranted,
            brainHealth = health,
            brainStage = BrainDecayStage.fromHealthPercent(health),
            urgesResistedToday = snapshot.urgesResistedToday,
            urgesBypassedToday = snapshot.urgesBypassedToday,
            minutesReclaimedToday = snapshot.minutesReclaimedToday,
            guardedAppNames = snapshot.guardedAppNames,
            personalMantra = snapshot.personalMantra,
            featuredInspirationPhoto = snapshot.featuredInspirationPhoto,
            inspirationPhotoCount = snapshot.inspirationPhotoCount,
            reminderCount = snapshot.reminderCount,
            appBreaks = snapshot.appBreaks,
        )
    }
}
