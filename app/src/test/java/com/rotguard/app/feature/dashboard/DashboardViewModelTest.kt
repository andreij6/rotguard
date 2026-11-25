package com.rotguard.app.feature.dashboard

import app.cash.turbine.test
import com.rotguard.app.R
import com.rotguard.app.storage.DailyScreenMinutes
import com.rotguard.app.storage.HapticConductor
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleShield persists new state and emits demo or inspiration navigation effects`() = runTest {
        val dataSource = FakeDashboardDataSource()
        val viewModel = DashboardViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.ToggleShieldTapped)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as DashboardUiState.Ready
        assertFalse(ready.shieldEnabled)
        assertFalse(dataSource.storedEnabled)

        viewModel.effects.test {
            viewModel.onAction(DashboardAction.TestShieldTapped)
            assertEquals(DashboardEffect.LaunchDemoShield, awaitItem())
            viewModel.onAction(DashboardAction.OpenInspirationTapped)
            assertEquals(DashboardEffect.NavigateToInspiration, awaitItem())
        }
    }

    @Test
    fun `load error transitions to Error state and retry recovers`() = runTest {
        val dataSource = FakeDashboardDataSource(shouldFail = true)
        val viewModel = DashboardViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        assertEquals(DashboardUiState.Error(R.string.dashboard_error_load), viewModel.uiState.value)

        dataSource.shouldFail = false
        viewModel.onAction(DashboardAction.RetryTapped)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DashboardUiState.Ready)
    }

    private class FakeDashboardDataSource(var shouldFail: Boolean = false) : DashboardDataSource {
        var storedEnabled = true
        override suspend fun endAppBreak(packageName: String) = Unit

        override suspend fun fetchDashboardSnapshotFromStorage(): DashboardSnapshot {
            if (shouldFail) throw IOException("DB read error")
            return DashboardSnapshot(
                shieldEnabled = storedEnabled,
                accessibilityGranted = true,
                overlayGranted = true,
                urgesResistedToday = 2,
                urgesBypassedToday = 0,
                minutesReclaimedToday = DailyScreenMinutes(10),
                guardedAppNames = listOf("YouTube"),
                personalMantra = "Focus",
                inspirationPhotoCount = 3,
                reminderCount = 4,
            )
        }
        override suspend fun updateShieldEnabledInDatabase(enabled: Boolean) {
            storedEnabled = enabled
        }
    }

    private class NoOpHapticConductor : HapticConductor {
        override fun playSelectionTick() = Unit
        override fun playWarningPulse(intensity: Float) = Unit
        override fun playHoldProgressTick(progressFraction: Float) = Unit
        override fun playRotDecayShudder(stageIndex: Int) = Unit
        override fun playVictoryChord() = Unit
        override fun playReleaseSoftTick() = Unit
    }
}
