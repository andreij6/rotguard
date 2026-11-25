package com.rotguard.app.feature.shield

import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome
import com.rotguard.app.storage.TimestampEpochMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldReducerTest {

    @Test
    fun `evaluateForegroundTransition shows overlay and carries the selected prompt pair for YouTube`() {
        val context = baseContext(
            guardedApps = mapOf(
                "com.google.android.youtube" to GuardedAppEntity(
                    packageName = "com.google.android.youtube",
                    displayName = "YouTube",
                    isGuarded = true,
                    customHoldDurationMillis = 4_000L,
                ),
            ),
        ).copy(
            openingPromptRes = R.string.blocker_opening_4,
            secondThoughtPromptRes = R.string.blocker_second_4,
        )

        val result = ShieldReducer.evaluateForegroundTransition(
            trackerState = ShieldTrackerState(),
            foregroundPackageName = "com.google.android.youtube",
            ownPackageName = "com.rotguard.app",
            context = context,
        )

        assertTrue(result.nextTrackerState.isOverlayShowing)
        val decision = result.decision as ShieldDecision.ShowOverlay
        assertEquals("YouTube", decision.appDisplayName)
        assertEquals(4_000L, decision.holdDurationMillis.value)
        assertEquals(1, decision.todayAttemptsCount)
        assertEquals(R.string.blocker_opening_4, decision.openingPromptRes)
        assertEquals(R.string.blocker_second_4, decision.secondThoughtPromptRes)
    }

    @Test
    fun `evaluateForegroundTransition ignores systemui without clearing active visit and clears on launcher`() {
        val activeState = ShieldTrackerState(activeGuardedApp = AppPackageName("com.google.android.youtube"))
        val context = baseContext(
            guardedApps = mapOf(
                "com.google.android.youtube" to GuardedAppEntity("com.google.android.youtube", "YouTube", true, null),
            ),
        )

        val systemUiResult = ShieldReducer.evaluateForegroundTransition(
            trackerState = activeState,
            foregroundPackageName = "com.android.systemui",
            ownPackageName = "com.rotguard.app",
            context = context,
        )
        assertEquals(AppPackageName("com.google.android.youtube"), systemUiResult.nextTrackerState.activeGuardedApp)
        assertTrue(systemUiResult.decision is ShieldDecision.Ignore)

        val launcherResult = ShieldReducer.evaluateForegroundTransition(
            trackerState = activeState,
            foregroundPackageName = "com.google.android.apps.nexuslauncher",
            ownPackageName = "com.rotguard.app",
            context = context,
        )
        assertNull(launcherResult.nextTrackerState.activeGuardedApp)
        assertTrue(launcherResult.decision is ShieldDecision.Ignore)
    }

    @Test
    fun `just closed app does not re-trigger while closing, but does after another app comes forward`() {
        val youtube = "com.google.android.youtube"
        val context = baseContext(guardedApps = mapOf(youtube to GuardedAppEntity(youtube, "YouTube", true, null)))
        val closed = ShieldTrackerState(justClosedApp = AppPackageName(youtube))

        val lateEvent = ShieldReducer.evaluateForegroundTransition(closed, youtube, "com.rotguard.app", context)
        assertTrue(lateEvent.decision is ShieldDecision.Ignore)
        assertEquals(AppPackageName(youtube), lateEvent.nextTrackerState.justClosedApp)

        val launcher = ShieldReducer.evaluateForegroundTransition(
            lateEvent.nextTrackerState,
            "com.google.android.apps.nexuslauncher",
            "com.rotguard.app",
            context,
        )
        assertNull(launcher.nextTrackerState.justClosedApp)

        val reopened = ShieldReducer.evaluateForegroundTransition(
            launcher.nextTrackerState,
            youtube,
            "com.rotguard.app",
            context,
        )
        assertTrue(reopened.decision is ShieldDecision.ShowOverlay)
    }

    @Test
    fun `repeat visit within five minutes offers a break and an active break skips the pause screen`() {
        val youtube = "com.google.android.youtube"
        val apps = mapOf(youtube to GuardedAppEntity(youtube, "YouTube", true, null))

        val first = ShieldReducer.evaluateForegroundTransition(
            ShieldTrackerState(),
            youtube,
            "com.rotguard.app",
            baseContext(apps),
        )
        assertFalse((first.decision as ShieldDecision.ShowOverlay).offerGracePeriod)

        val repeat = ShieldReducer.evaluateForegroundTransition(
            ShieldTrackerState(),
            youtube,
            "com.rotguard.app",
            baseContext(apps).copy(recentAttemptsByPackage = mapOf(youtube to 1)),
        )
        assertTrue((repeat.decision as ShieldDecision.ShowOverlay).offerGracePeriod)

        val onBreak = ShieldReducer.evaluateForegroundTransition(
            ShieldTrackerState(),
            youtube,
            "com.rotguard.app",
            baseContext(apps).copy(snoozedUntilByPackage = mapOf(youtube to 20_000L)),
        )
        assertTrue(onBreak.decision is ShieldDecision.Ignore)
    }

    @Test
    fun `evaluateForegroundTransition honors active grace window and ignores disabled shield`() {
        val context = baseContext(
            guardedApps = mapOf(
                "com.google.android.youtube" to GuardedAppEntity("com.google.android.youtube", "YouTube", true, null),
            ),
            nowEpochMillis = 50_000L,
        )
        val gracedState = ShieldTrackerState(
            graceGrantedUntilEpochByApp = mapOf("com.google.android.youtube" to 90_000L),
        )

        val gracedResult = ShieldReducer.evaluateForegroundTransition(
            trackerState = gracedState,
            foregroundPackageName = "com.google.android.youtube",
            ownPackageName = "com.rotguard.app",
            context = context,
        )
        assertTrue(gracedResult.decision is ShieldDecision.Ignore)
        assertEquals(AppPackageName("com.google.android.youtube"), gracedResult.nextTrackerState.activeGuardedApp)

        val disabledResult = ShieldReducer.evaluateForegroundTransition(
            trackerState = ShieldTrackerState(),
            foregroundPackageName = "com.google.android.youtube",
            ownPackageName = "com.rotguard.app",
            context = context.copy(shieldEnabled = false),
        )
        assertTrue(disabledResult.decision is ShieldDecision.Ignore)
    }

    @Test
    fun `computeBrainHealthFromEvents reflects resisted gains and bypassed drops`() {
        val events = listOf(
            InterceptEventEntity(1, "pkg", "YouTube", 10L, InterceptOutcome.RESISTED.name, 5),
            InterceptEventEntity(2, "pkg", "YouTube", 20L, InterceptOutcome.BYPASSED.name, 0),
            InterceptEventEntity(3, "pkg", "YouTube", 30L, InterceptOutcome.BYPASSED.name, 0),
        )
        val health = ShieldReducer.computeBrainHealthFromEvents(events)
        assertEquals(64, health.value)
    }

    @Test
    fun `reduce switches to second thought at halfway hold progress and resets on early release`() {
        val decision = sampleDecision()
        var state = ShieldReducer.reduce(ShieldOverlayUiState.Hidden, ShieldInternalEvent.SessionStarted(decision))
        state = ShieldReducer.reduce(state, ShieldInternalEvent.HoldBegan)
        state = ShieldReducer.reduce(state, ShieldInternalEvent.HoldProgressed(0.49f))
        assertFalse((state as ShieldOverlayUiState.Blocking).hasTransitionedToSecondThought)

        state = ShieldReducer.reduce(state, ShieldInternalEvent.HoldProgressed(0.5f))
        assertTrue((state as ShieldOverlayUiState.Blocking).hasTransitionedToSecondThought)

        state = ShieldReducer.reduce(state, ShieldInternalEvent.HoldReleasedEarly)
        val blocking = state as ShieldOverlayUiState.Blocking
        assertFalse(blocking.isHolding)
        assertEquals(0f, blocking.holdProgressFraction)
        assertFalse(blocking.hasTransitionedToSecondThought)
        assertTrue(blocking.releasedEarlyHint)
    }

    @Test
    fun `reduce transitions from Blocking to inline CelebratingExit and clamps progress`() {
        val initial = ShieldReducer.reduce(
            ShieldOverlayUiState.Hidden,
            ShieldInternalEvent.SessionStarted(sampleDecision()),
        )
        val celebrating = ShieldReducer.reduce(
            initial,
            ShieldInternalEvent.ExitCelebrationStarted(
                estimatedMinutesSaved = 5,
                todayUrgesStoppedCount = 4,
                celebrationVariantIndex = 37,
            ),
        ) as ShieldOverlayUiState.CelebratingExit
        assertEquals("YouTube", celebrating.appDisplayName)
        assertEquals(4, celebrating.todayUrgesStoppedCount)
        assertEquals(37, celebrating.celebrationVariantIndex)

        val progressed = ShieldReducer.reduce(
            celebrating,
            ShieldInternalEvent.ExitCelebrationProgressed(1.4f),
        ) as ShieldOverlayUiState.CelebratingExit
        assertEquals(1f, progressed.celebrationProgressFraction)
    }

    private fun sampleDecision() = ShieldDecision.ShowOverlay(
        packageName = AppPackageName("com.google.android.youtube"),
        appDisplayName = "YouTube",
        holdDurationMillis = HoldDurationMillis(4_000L),
        brainHealth = BrainHealthPercent(80),
        brainStage = BrainDecayStage.THRIVING,
        personalMantra = "Focus",
        todayAttemptsCount = 2,
    )

    private fun baseContext(
        guardedApps: Map<String, GuardedAppEntity>,
        nowEpochMillis: Long = 10_000L,
    ) = ShieldEvaluationContext(
        shieldEnabled = true,
        personalMantra = "Stay focused",
        globalHoldDurationMillis = HoldDurationMillis(4_000L),
        sessionGraceDurationMillis = HoldDurationMillis(0L),
        guardedAppsByPackage = guardedApps,
        todayAttemptsByPackage = emptyMap(),
        brainHealth = BrainHealthPercent(85),
        nowEpochMillis = TimestampEpochMillis(nowEpochMillis),
    )
}
