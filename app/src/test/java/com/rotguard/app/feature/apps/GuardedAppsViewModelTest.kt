package com.rotguard.app.feature.apps

import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.HoldDurationMillis
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
class GuardedAppsViewModelTest {

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
    fun `toggling app and selecting hold duration persists updates to dataSource`() = runTest {
        val dataSource = FakeAppsDataSource()
        val viewModel = GuardedAppsViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        viewModel.onAction(GuardedAppsAction.AppGuardToggled(AppPackageName("com.google.android.youtube")))
        viewModel.onAction(GuardedAppsAction.GlobalHoldDurationSelected(HoldDurationMillis(6_000L)))
        advanceUntilIdle()

        assertEquals(listOf("com.google.android.youtube" to false), dataSource.persistedToggles)
        assertEquals(6_000L, dataSource.persistedHoldDurationMs)
    }

    @Test
    fun `load failure transitions to Error and retry recovers`() = runTest {
        val dataSource = FakeAppsDataSource(shouldFail = true)
        val viewModel = GuardedAppsViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        assertEquals(GuardedAppsUiState.Error(R.string.apps_error_load), viewModel.uiState.value)

        dataSource.shouldFail = false
        viewModel.onAction(GuardedAppsAction.RetryTapped)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GuardedAppsUiState.Ready)
    }

    private class FakeAppsDataSource(var shouldFail: Boolean = false) : GuardedAppsDataSource {
        val persistedToggles = mutableListOf<Pair<String, Boolean>>()
        var persistedHoldDurationMs = 4_000L

        override suspend fun fetchInstalledAndGuardedApps(): GuardedAppsCatalogSnapshot {
            if (shouldFail) throw IOException("Catalog failed")
            return GuardedAppsCatalogSnapshot(
                globalHoldDurationMillis = HoldDurationMillis(persistedHoldDurationMs),
                apps = listOf(
                    GuardedAppRow(AppPackageName("com.google.android.youtube"), "YouTube", true, true, 4),
                ),
            )
        }

        override suspend fun persistAppGuardToggle(packageName: AppPackageName, isGuarded: Boolean) {
            persistedToggles += packageName.value to isGuarded
        }

        override suspend fun persistGlobalHoldDuration(durationMillis: HoldDurationMillis) {
            persistedHoldDurationMs = durationMillis.value
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
