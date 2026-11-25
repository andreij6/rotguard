package com.rotguard.app.feature.shield

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InterceptOutcome
import com.rotguard.app.storage.TimestampEpochMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrictionSessionControllerTest {

    @Test
    fun `closing app plays inline celebration then exits to home cleanly`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val haptics = RecordingHapticConductor()
        val controller = FrictionSessionController(dataSource, haptics, FixedClockProvider(), dispatcher)
        var exitInvocations = 0

        controller.startBlockingSession(
            decision = sampleDecision(),
            onExitToHome = { exitInvocations += 1 },
            onContinueToApp = {},
        )
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Blocking)

        controller.onAction(ShieldAction.ProtectBrainExitTapped)
        advanceTimeBy(400L)

        val celebrating = controller.uiState.value as ShieldOverlayUiState.CelebratingExit
        assertEquals("YouTube", celebrating.appDisplayName)
        assertEquals(0, exitInvocations)
        assertEquals(1, haptics.victoryChordCount)

        advanceTimeBy(FrictionSessionController.EXIT_CELEBRATION_DURATION_MS)
        advanceUntilIdle()

        assertEquals(1, exitInvocations)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Hidden)
        assertEquals(listOf(InterceptOutcome.RESISTED), dataSource.recordedOutcomes)
    }

    @Test
    fun `staring at the pause screen for 15 seconds closes the app like tapping close`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val controller = FrictionSessionController(
            dataSource,
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        var exits = 0
        controller.startBlockingSession(sampleDecision(), onExitToHome = { exits += 1 }, onContinueToApp = {})

        advanceTimeBy(FrictionSessionController.IDLE_AUTO_CLOSE_MS - 100L)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Blocking)

        advanceTimeBy(200L)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.CelebratingExit)
        advanceUntilIdle()
        assertEquals(1, exits)
        assertEquals(listOf(InterceptOutcome.RESISTED), dataSource.recordedOutcomes)
    }

    @Test
    fun `a ticked break starts only after holding through, for the chosen length`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val controller = FrictionSessionController(
            dataSource,
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        controller.startBlockingSession(sampleDecision(), onExitToHome = {}, onContinueToApp = {})
        controller.onAction(ShieldAction.GraceMinutesSelected(60))
        assertEquals(60, (controller.uiState.value as ShieldOverlayUiState.Blocking).graceMinutes)

        controller.onAction(ShieldAction.HoldStarted)
        advanceUntilIdle()

        assertEquals(listOf(AppPackageName("com.google.android.youtube") to 1_000L + 60 * 60_000L), dataSource.snoozes)
    }

    @Test
    fun `test sessions play normally but never record an outcome`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val controller = FrictionSessionController(
            dataSource,
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        var exits = 0
        controller.startBlockingSession(
            sampleDecision(),
            onExitToHome = { exits += 1 },
            onContinueToApp = {},
            countsTowardStats = false,
        )

        controller.onAction(ShieldAction.ProtectBrainExitTapped)
        advanceUntilIdle()

        assertEquals(1, exits)
        assertTrue(dataSource.recordedOutcomes.isEmpty())
    }

    @Test
    fun `idle countdown appears after 8 seconds and ticks down to 1`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = FrictionSessionController(
            FakeShieldDataSource(),
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        controller.startBlockingSession(sampleDecision(), onExitToHome = {}, onContinueToApp = {})

        advanceTimeBy(7_900L)
        assertEquals(null, (controller.uiState.value as ShieldOverlayUiState.Blocking).idleSecondsLeft)
        advanceTimeBy(200L)
        assertEquals(7, (controller.uiState.value as ShieldOverlayUiState.Blocking).idleSecondsLeft)
        advanceTimeBy(6_000L)
        assertEquals(1, (controller.uiState.value as ShieldOverlayUiState.Blocking).idleSecondsLeft)

        controller.onAction(ShieldAction.HoldStarted)
        assertEquals(null, (controller.uiState.value as ShieldOverlayUiState.Blocking).idleSecondsLeft)
    }

    @Test
    fun `holding pauses the idle auto-close and an early release restarts it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = FrictionSessionController(
            FakeShieldDataSource(),
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        controller.startBlockingSession(sampleDecision(), onExitToHome = {}, onContinueToApp = {})

        advanceTimeBy(14_000L)
        controller.onAction(ShieldAction.HoldStarted)
        advanceTimeBy(1_000L)
        controller.onAction(ShieldAction.HoldReleased)
        advanceTimeBy(FrictionSessionController.IDLE_AUTO_CLOSE_MS - 1_000L)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Blocking)

        advanceTimeBy(1_100L)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.CelebratingExit)
    }

    @Test
    fun `tapping during inline celebration exits early without waiting full timer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val controller = FrictionSessionController(
            dataSource,
            RecordingHapticConductor(),
            FixedClockProvider(),
            dispatcher,
        )
        var exitInvocations = 0

        controller.startBlockingSession(
            decision = sampleDecision(),
            onExitToHome = { exitInvocations += 1 },
            onContinueToApp = {},
        )
        controller.onAction(ShieldAction.ProtectBrainExitTapped)
        advanceTimeBy(200L)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.CelebratingExit)

        controller.onAction(ShieldAction.CelebrationFinishNowTapped)
        advanceUntilIdle()

        assertEquals(1, exitInvocations)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Hidden)
        assertEquals(listOf(InterceptOutcome.RESISTED), dataSource.recordedOutcomes)
    }

    @Test
    fun `hold silently transitions prompt halfway and bypasses after full hold duration`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataSource = FakeShieldDataSource()
        val haptics = RecordingHapticConductor()
        val controller = FrictionSessionController(dataSource, haptics, FixedClockProvider(), dispatcher)
        var continued = false

        controller.startBlockingSession(
            decision = sampleDecision(holdDurationMs = 3_000L),
            onExitToHome = {},
            onContinueToApp = { continued = true },
        )

        controller.onAction(ShieldAction.HoldStarted)
        advanceTimeBy(1_600L)
        val midHold = controller.uiState.value as ShieldOverlayUiState.Blocking
        assertTrue(midHold.hasTransitionedToSecondThought)
        assertTrue(haptics.selectionTickCount > 0)

        controller.onAction(ShieldAction.HoldReleased)
        val releasedState = controller.uiState.value as ShieldOverlayUiState.Blocking
        assertTrue(releasedState.releasedEarlyHint)
        assertFalse(releasedState.hasTransitionedToSecondThought)
        assertEquals(0f, releasedState.holdProgressFraction)

        controller.onAction(ShieldAction.HoldStarted)
        advanceTimeBy(3_100L)
        advanceUntilIdle()

        assertTrue(continued)
        assertTrue(controller.uiState.value is ShieldOverlayUiState.Hidden)
        assertEquals(listOf(InterceptOutcome.BYPASSED), dataSource.recordedOutcomes)
    }

    private fun sampleDecision(holdDurationMs: Long = 4_000L) = ShieldDecision.ShowOverlay(
        packageName = AppPackageName("com.google.android.youtube"),
        appDisplayName = "YouTube",
        holdDurationMillis = HoldDurationMillis(holdDurationMs),
        brainHealth = BrainHealthPercent(82),
        brainStage = BrainDecayStage.THRIVING,
        personalMantra = "Stay intentional",
        todayAttemptsCount = 1,
        todayResistedCount = 2,
    )

    private class FakeShieldDataSource : ShieldDataSource {
        val recordedOutcomes = mutableListOf<InterceptOutcome>()
        val snoozes = mutableListOf<Pair<AppPackageName, Long>>()

        override suspend fun snoozeApp(packageName: AppPackageName, untilEpochMillis: Long) {
            snoozes += packageName to untilEpochMillis
        }

        override suspend fun loadShieldEvaluationContext(): ShieldEvaluationContext {
            error("Not needed in controller test")
        }

        override suspend fun loadDemoBlockingDecision(
            packageName: AppPackageName,
            appDisplayName: String,
        ): ShieldDecision.ShowOverlay {
            error("Not needed in controller test")
        }

        override suspend fun recordInterceptOutcomeToDatabase(
            packageName: AppPackageName,
            appDisplayName: String,
            outcome: InterceptOutcome,
        ) {
            recordedOutcomes += outcome
        }

        override suspend fun recordBlockerRotationUsed(
            variationIndex: Int,
            inspirationIndex: Int,
            reminderId: Long,
        ) = Unit
    }

    private class RecordingHapticConductor : HapticConductor {
        var selectionTickCount = 0
        var victoryChordCount = 0
        override fun playSelectionTick() {
            selectionTickCount += 1
        }
        override fun playWarningPulse(intensity: Float) = Unit
        override fun playHoldProgressTick(progressFraction: Float) = Unit
        override fun playRotDecayShudder(stageIndex: Int) = Unit
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
