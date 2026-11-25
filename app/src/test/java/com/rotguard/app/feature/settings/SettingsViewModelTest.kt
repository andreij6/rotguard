package com.rotguard.app.feature.settings

import app.cash.turbine.test
import com.rotguard.app.R
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.ThemeMode
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `updates theme mode, hold duration, saves mantra, and emits navigation effects`() = runTest {
        val dataSource = FakeSettingsDataSource()
        val viewModel = SettingsViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ThemeModeSelected(ThemeMode.DARK))
        viewModel.onAction(SettingsAction.HoldDurationSelected(HoldDurationMillis(10_000L)))
        viewModel.onAction(SettingsAction.MantraDraftChanged("Stay calm"))
        viewModel.onAction(SettingsAction.SaveMantraTapped)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, dataSource.themeMode)
        assertEquals(10_000L, dataSource.holdDurationMs)
        assertEquals("Stay calm", dataSource.mantra)
        val ready = viewModel.uiState.value as SettingsUiState.Ready
        assertTrue(ready.mantraSavedHintVisible)
        assertEquals(ThemeMode.DARK, ready.themeMode)

        viewModel.effects.test {
            viewModel.onAction(SettingsAction.OpenInspirationTapped)
            assertEquals(SettingsEffect.NavigateToInspiration, awaitItem())
            viewModel.onAction(SettingsAction.ReplayOnboardingTapped)
            assertEquals(SettingsEffect.ReplayOnboarding, awaitItem())
        }
    }

    @Test
    fun `load failure transitions to Error state and retry recovers`() = runTest {
        val dataSource = FakeSettingsDataSource(shouldFail = true)
        val viewModel = SettingsViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        assertEquals(SettingsUiState.Error(R.string.settings_error_load), viewModel.uiState.value)

        dataSource.shouldFail = false
        viewModel.onAction(SettingsAction.RetryTapped)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.Ready)
    }

    private class FakeSettingsDataSource(var shouldFail: Boolean = false) : SettingsDataSource {
        var holdDurationMs = 4_000L
        var graceMs = 0L
        var haptics = true
        var themeMode = ThemeMode.SYSTEM
        var mantra = "Focus"

        override suspend fun loadSettingsSnapshotFromDatabase(): SettingsSnapshot {
            if (shouldFail) throw IOException("Settings load failed")
            return SettingsSnapshot(
                accessibilityGranted = false,
                overlayGranted = false,
                usageStatsGranted = false,
                globalHoldDurationMillis = HoldDurationMillis(holdDurationMs),
                sessionGraceDurationMillis = HoldDurationMillis(graceMs),
                hapticsEnabled = haptics,
                personalMantra = mantra,
                themeMode = themeMode,
                inspirationPhotoCount = 3,
                reminderCount = 4,
            )
        }

        override fun inspectSystemPermissions() = PermissionStatusSnapshot(true, true, false)
        override suspend fun updateHoldDurationInDatabase(durationMillis: HoldDurationMillis) {
            holdDurationMs = durationMillis.value
        }
        override suspend fun updateGraceWindowInDatabase(durationMillis: HoldDurationMillis) {
            graceMs = durationMillis.value
        }
        override suspend fun updateHapticsEnabledInDatabase(enabled: Boolean) {
            haptics = enabled
        }
        override suspend fun updateThemeModeInDatabase(themeMode: ThemeMode) {
            this.themeMode = themeMode
        }
        override suspend fun updatePersonalMantraInDatabase(mantraText: String) {
            mantra = mantraText
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
