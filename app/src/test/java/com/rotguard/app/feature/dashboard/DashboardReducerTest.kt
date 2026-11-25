package com.rotguard.app.feature.dashboard

import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.DailyScreenMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardReducerTest {

    @Test
    fun `reduce maps snapshot to thriving Ready state when urges resisted`() {
        val state = DashboardReducer.reduce(
            DashboardUiState.Loading,
            DashboardInternalEvent.SnapshotLoaded(
                DashboardSnapshot(
                    shieldEnabled = true,
                    accessibilityGranted = true,
                    overlayGranted = true,
                    urgesResistedToday = 3,
                    urgesBypassedToday = 0,
                    minutesReclaimedToday = DailyScreenMinutes(15),
                    guardedAppNames = listOf("YouTube", "Chrome"),
                    personalMantra = "Focus first",
                ),
            ),
        ) as DashboardUiState.Ready

        assertEquals(94, state.brainHealth.value)
        assertEquals(BrainDecayStage.THRIVING, state.brainStage)
        assertTrue(state.isPermissionReady)
    }

    @Test
    fun `reduce drops brain health and clamps minimum at 5 when urges repeatedly bypassed`() {
        val state = DashboardReducer.reduce(
            DashboardUiState.Loading,
            DashboardInternalEvent.SnapshotLoaded(
                DashboardSnapshot(
                    shieldEnabled = true,
                    accessibilityGranted = false,
                    overlayGranted = false,
                    urgesResistedToday = 0,
                    urgesBypassedToday = 8,
                    minutesReclaimedToday = DailyScreenMinutes(0),
                    guardedAppNames = emptyList(),
                    personalMantra = "Focus",
                ),
            ),
        ) as DashboardUiState.Ready

        assertEquals(5, state.brainHealth.value)
        assertEquals(BrainDecayStage.ROTTED, state.brainStage)
        assertFalse(state.isPermissionReady)
    }

    @Test
    fun `reduce updates shieldEnabled when ShieldToggled event received`() {
        val initial = DashboardReducer.reduce(
            DashboardUiState.Loading,
            DashboardInternalEvent.SnapshotLoaded(
                DashboardSnapshot(true, true, true, 0, 0, DailyScreenMinutes(0), emptyList(), "Mantra"),
            ),
        )
        val paused = DashboardReducer.reduce(
            initial,
            DashboardInternalEvent.ShieldToggled(false),
        ) as DashboardUiState.Ready
        assertFalse(paused.shieldEnabled)
    }
}
