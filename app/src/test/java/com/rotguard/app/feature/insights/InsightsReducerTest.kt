package com.rotguard.app.feature.insights

import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsReducerTest {

    @Test
    fun `aggregatePayload produces 7 trend points, package breakdown, and evaluates duel status`() {
        val todayStart = 10 * 86_400_000L
        val events = listOf(
            InterceptEventEntity(
                id = 1,
                packageName = "com.google.android.youtube",
                appDisplayName = "YouTube",
                timestampEpochMillis = todayStart + 1_000L,
                outcomeName = InterceptOutcome.RESISTED.name,
                estimatedMinutesSaved = 5,
            ),
            InterceptEventEntity(
                id = 2,
                packageName = "com.google.android.youtube",
                appDisplayName = "YouTube",
                timestampEpochMillis = todayStart + 2_000L,
                outcomeName = InterceptOutcome.BYPASSED.name,
                estimatedMinutesSaved = 0,
            ),
        )

        val payload = InsightsReducer.aggregatePayload(
            events = events,
            startOfTodayEpochMillis = todayStart,
            rivalDailyTargetCount = 5,
        )

        assertEquals(7, payload.weeklyTrend.size)
        val consecutiveDays = payload.weeklyTrend.zipWithNext().all { (earlier, later) ->
            earlier.dayOfWeek.plus(1) == later.dayOfWeek
        }
        assertTrue(consecutiveDays)
        assertEquals(1, payload.yourBypassesToday)
        assertEquals(5, payload.totalMinutesSavedThisWeek.value)
        assertEquals("YouTube", payload.appBreakdowns.first().appDisplayName)
        assertEquals("com.google.android.youtube", payload.appBreakdowns.first().packageName.value)

        val winningState = InsightsReducer.reduce(
            InsightsUiState.Loading,
            InsightsInternalEvent.Loaded(payload),
        ) as InsightsUiState.Ready
        assertTrue(winningState.isWinningDuel)

        val losingState = InsightsReducer.reduce(
            InsightsUiState.Loading,
            InsightsInternalEvent.Loaded(payload.copy(yourBypassesToday = 8)),
        ) as InsightsUiState.Ready
        assertFalse(losingState.isWinningDuel)
    }
}
