package com.rotguard.app.feature.onboarding

import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.UserGoalId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReducerTest {

    @Test
    fun `reduce advances through 8 steps sequentially including INSPIRATION_PHOTOS and clamps at COMPLETE`() {
        var state: OnboardingUiState = OnboardingReducer.reduce(
            OnboardingUiState.Loading,
            loadedEvent(),
        )
        assertEquals(OnboardingStep.WELCOME, (state as OnboardingUiState.Active).step)

        val expectedOrder = listOf(
            OnboardingStep.ROT_SIMULATOR,
            OnboardingStep.TIME_AUDIT,
            OnboardingStep.GOAL_PICKER,
            OnboardingStep.INSPIRATION_PHOTOS,
            OnboardingStep.APP_SELECTION,
            OnboardingStep.PERMISSIONS,
            OnboardingStep.COMMITMENT_PLEDGE,
            OnboardingStep.COMPLETE,
            OnboardingStep.COMPLETE,
        )
        expectedOrder.forEach { expectedStep ->
            state = OnboardingReducer.reduce(state, OnboardingInternalEvent.StepAdvanced)
            assertEquals(expectedStep, (state as OnboardingUiState.Active).step)
        }
    }

    @Test
    fun `reduce computes accurate life-cost metrics when daily hours change`() {
        val loaded = OnboardingReducer.reduce(OnboardingUiState.Loading, loadedEvent())
        val updated = OnboardingReducer.reduce(
            loaded,
            OnboardingInternalEvent.DailyHoursUpdated(hours = 4),
        ) as OnboardingUiState.Active

        assertEquals(4, updated.dailyDoomscrollHours)
        assertEquals(61, updated.daysLostPerYear)
        assertEquals(125, updated.wakingYearsLostTenthsOver50Years)
    }

    @Test
    fun `reduce adds and removes inspiration photos during onboarding`() {
        val initial = OnboardingReducer.reduce(OnboardingUiState.Loading, loadedEvent())
        val added = OnboardingReducer.reduce(
            initial,
            OnboardingInternalEvent.InspirationPhotoAdded(
                OnboardingInspirationPreview(
                    id = InspirationPhotoId(77L),
                    title = "My Inspiration",
                    caption = "Stay balanced",
                    filePath = "/files/inspiration/77.jpg",
                ),
            ),
        ) as OnboardingUiState.Active
        assertEquals(1, added.inspirationPhotos.size)

        val removed = OnboardingReducer.reduce(
            added,
            OnboardingInternalEvent.InspirationPhotoDeleted(InspirationPhotoId(77L)),
        ) as OnboardingUiState.Active
        assertTrue(removed.inspirationPhotos.isEmpty())
    }

    @Test
    fun `reduce resets pledge hold progress and sets early hint when released early`() {
        var state = OnboardingReducer.reduce(OnboardingUiState.Loading, loadedEvent())
        state = OnboardingReducer.reduce(state, OnboardingInternalEvent.PledgeHoldBegan)
        state = OnboardingReducer.reduce(state, OnboardingInternalEvent.PledgeHoldProgressed(0.55f))
        state = OnboardingReducer.reduce(state, OnboardingInternalEvent.PledgeHoldReleasedEarly)

        val active = state as OnboardingUiState.Active
        assertFalse(active.isHoldingPledge)
        assertEquals(0f, active.pledgeHoldProgressFraction)
        assertTrue(active.releasedPledgeEarlyHint)
    }

    @Test
    fun `reduce updates simulator stage and toggles selected guarded apps`() {
        val initial = OnboardingReducer.reduce(OnboardingUiState.Loading, loadedEvent())
        val rotUpdated = OnboardingReducer.reduce(
            initial,
            OnboardingInternalEvent.SimulatorStageUpdated(BrainDecayStage.CRACKING),
        )
        val appToggled = OnboardingReducer.reduce(
            rotUpdated,
            OnboardingInternalEvent.AppChoiceToggled(AppPackageName("com.google.android.youtube")),
        ) as OnboardingUiState.Active

        assertEquals(BrainDecayStage.CRACKING, appToggled.simulatorStage)
        assertFalse(
            appToggled.appChoices.first { it.packageName.value == "com.google.android.youtube" }.isSelected,
        )
    }

    @Test
    fun `reduce maps load and photo failures to string resource ids`() {
        val failed = OnboardingReducer.reduce(
            OnboardingUiState.Loading,
            OnboardingInternalEvent.LoadFailed(R.string.onboarding_error_load),
        )
        assertEquals(OnboardingUiState.Error(R.string.onboarding_error_load), failed)

        val loaded = OnboardingReducer.reduce(OnboardingUiState.Loading, loadedEvent())
        val photoFailed = OnboardingReducer.reduce(
            loaded,
            OnboardingInternalEvent.InspirationPhotoSaveFailed(R.string.onboarding_error_photo),
        ) as OnboardingUiState.Active
        assertEquals(R.string.onboarding_error_photo, photoFailed.inspirationPhotoErrorRes)

        val retried = OnboardingReducer.reduce(
            photoFailed,
            OnboardingInternalEvent.InspirationPhotoSaveStarted,
        ) as OnboardingUiState.Active
        assertNull(retried.inspirationPhotoErrorRes)
    }

    private fun loadedEvent(): OnboardingInternalEvent.Loaded = OnboardingInternalEvent.Loaded(
        appChoices = listOf(
            OnboardingAppChoice(
                packageName = AppPackageName("com.google.android.youtube"),
                displayName = "YouTube",
                isRecommended = true,
                isSelected = true,
            ),
        ),
        accessibilityGranted = false,
        overlayGranted = false,
        existingDailyHours = 3,
        existingGoalIds = setOf(UserGoalId("deep_work")),
        existingPeakWindow = "Late Night (9pm - 1am)",
        existingMantra = "Build things that outlast the feed.",
    )
}
