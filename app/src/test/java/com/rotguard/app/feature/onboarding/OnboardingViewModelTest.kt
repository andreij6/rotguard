package com.rotguard.app.feature.onboarding

import app.cash.turbine.test
import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.TimestampEpochMillis
import com.rotguard.app.storage.UserGoalId
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class OnboardingViewModelTest {

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
    fun `simulator selection triggers haptic shudder and updates stage`() = runTest {
        val haptics = RecordingHapticConductor()
        val viewModel = OnboardingViewModel(FakeOnboardingDataSource(), haptics, FixedClockProvider())
        advanceUntilIdle()

        viewModel.onAction(OnboardingAction.NextStepTapped)
        viewModel.onAction(OnboardingAction.SimulatorStageSelected(BrainDecayStage.ROTTED))

        val active = viewModel.uiState.value as OnboardingUiState.Active
        assertEquals(BrainDecayStage.ROTTED, active.simulatorStage)
        assertEquals(listOf(4), haptics.rotStagesPlayed)
    }

    @Test
    fun `pledge hold resets on early release and completes + saves after 3 seconds`() = runTest {
        val dataSource = FakeOnboardingDataSource()
        val haptics = RecordingHapticConductor()
        val viewModel = OnboardingViewModel(dataSource, haptics, FixedClockProvider())
        advanceUntilIdle()

        // Advance to COMMITMENT_PLEDGE (step 8)
        repeat(7) { viewModel.onAction(OnboardingAction.NextStepTapped) }

        viewModel.onAction(OnboardingAction.PledgeHoldStarted)
        advanceTimeBy(1_000L)
        viewModel.onAction(OnboardingAction.PledgeHoldReleased)
        val releasedState = viewModel.uiState.value as OnboardingUiState.Active
        assertTrue(releasedState.releasedPledgeEarlyHint)
        assertEquals(0f, releasedState.pledgeHoldProgressFraction)

        viewModel.onAction(OnboardingAction.PledgeHoldStarted)
        advanceTimeBy(OnboardingViewModel.PLEDGE_HOLD_DURATION_MS + 100L)
        advanceUntilIdle()

        val completedState = viewModel.uiState.value as OnboardingUiState.Active
        assertEquals(OnboardingStep.COMPLETE, completedState.step)
        assertTrue(dataSource.savedCalled)
        assertTrue(haptics.victoryChordCount > 0)
    }

    @Test
    fun `picking and removing inspiration photo updates onboarding list`() = runTest {
        val dataSource = FakeOnboardingDataSource()
        val viewModel = OnboardingViewModel(dataSource, RecordingHapticConductor(), FixedClockProvider())
        advanceUntilIdle()

        viewModel.onAction(OnboardingAction.InspirationPhotoPicked("content://img/22"))
        advanceUntilIdle()

        val afterPick = viewModel.uiState.value as OnboardingUiState.Active
        assertEquals(1, afterPick.inspirationPhotos.size)

        viewModel.onAction(OnboardingAction.InspirationPhotoRemoved(afterPick.inspirationPhotos.first().id))
        advanceUntilIdle()

        val afterRemove = viewModel.uiState.value as OnboardingUiState.Active
        assertTrue(afterRemove.inspirationPhotos.isEmpty())
    }

    @Test
    fun `load failure transitions to Error state and retry recovers`() = runTest {
        val dataSource = FakeOnboardingDataSource(shouldFailLoad = true)
        val viewModel = OnboardingViewModel(dataSource, RecordingHapticConductor(), FixedClockProvider())
        advanceUntilIdle()

        assertEquals(OnboardingUiState.Error(R.string.onboarding_error_load), viewModel.uiState.value)

        dataSource.shouldFailLoad = false
        viewModel.onAction(OnboardingAction.RetryTapped)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OnboardingUiState.Active)
    }

    @Test
    fun `finish button emits NavigateToDashboard effect`() = runTest {
        val viewModel = OnboardingViewModel(
            FakeOnboardingDataSource(),
            RecordingHapticConductor(),
            FixedClockProvider(),
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onAction(OnboardingAction.FinishOnboardingTapped)
            assertEquals(OnboardingEffect.NavigateToDashboard, awaitItem())
        }
    }

    private class FakeOnboardingDataSource(
        var shouldFailLoad: Boolean = false,
    ) : OnboardingDataSource {
        var savedCalled = false

        override suspend fun loadInitialOnboardingData(): InitialOnboardingPayload {
            if (shouldFailLoad) throw IOException("Package query failed")
            return InitialOnboardingPayload(
                appChoices = listOf(
                    OnboardingAppChoice(AppPackageName("com.google.android.youtube"), "YouTube", true, true),
                ),
                permissions = PermissionStatusSnapshot(false, false, false),
                existingDailyHours = 3,
                existingGoalIds = setOf(UserGoalId("deep_work")),
                existingPeakWindow = "Late Night (9pm - 1am)",
                existingMantra = "Protect tonight's focus.",
                inspirationPhotos = emptyList(),
            )
        }

        override suspend fun addOnboardingInspirationPhoto(uriString: String): OnboardingInspirationPreview {
            return OnboardingInspirationPreview(
                id = InspirationPhotoId(55L),
                title = "My Inspiration",
                caption = "Keep going",
                filePath = "/local/55.jpg",
            )
        }

        override suspend fun removeOnboardingInspirationPhoto(photoId: InspirationPhotoId) = Unit

        override suspend fun setOnboardingInspirationPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean) = Unit

        override suspend fun saveOnboardingCommitmentToDatabase(request: OnboardingCommitmentRequest) {
            savedCalled = true
        }

        override fun inspectCurrentPermissions() = PermissionStatusSnapshot(true, true, false)
    }

    private class RecordingHapticConductor : HapticConductor {
        val rotStagesPlayed = mutableListOf<Int>()
        var victoryChordCount = 0
        override fun playSelectionTick() = Unit
        override fun playWarningPulse(intensity: Float) = Unit
        override fun playHoldProgressTick(progressFraction: Float) = Unit
        override fun playRotDecayShudder(stageIndex: Int) {
            rotStagesPlayed += stageIndex
        }
        override fun playVictoryChord() {
            victoryChordCount += 1
        }
        override fun playReleaseSoftTick() = Unit
    }

    private class FixedClockProvider : ClockProvider {
        override fun nowEpochMillis() = TimestampEpochMillis(1_000L)
        override fun startOfTodayEpochMillis() = TimestampEpochMillis(0L)
    }
}
