package com.rotguard.app.feature.insights

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.DailyScreenMinutes
import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome
import java.time.Instant
import java.time.ZoneId

/**
 * Pure deterministic reducer and 7-day trend aggregator for Insights & Daily Focus Check.
 *
 * **Responsibility & Boundaries**
 * - Maps [InsightsRawPayload] into [InsightsUiState.Ready] and builds 7-day Focus Health trend
 *   and per-app closed/opened breakdowns from event rows.
 *
 * **Side Effects**
 * - None. Must never read Room DAOs or Android Context.
 *
 * **Invariants**
 * - Weekly trend always contains exactly 7 chronological day points ending today.
 * - `isWinningDuel` is `true` whenever `yourBypassesToday <= rivalDailyTargetCount`.
 *
 * **Anti-Patterns**
 * - Never aggregate chart data inside Compose rendering blocks.
 */
object InsightsReducer {

    private const val DAYS_IN_WEEK = 7
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val BASE_HEALTH = 85
    private const val RESISTED_BONUS = 3
    private const val BYPASSED_PENALTY = 12
    private const val MIN_HEALTH = 5
    private const val MAX_HEALTH = 100

    fun reduce(state: InsightsUiState, event: InsightsInternalEvent): InsightsUiState {
        return when (event) {
            InsightsInternalEvent.LoadStarted -> if (state is InsightsUiState.Ready) state else InsightsUiState.Loading
            is InsightsInternalEvent.LoadFailed -> InsightsUiState.Error(event.messageRes)
            is InsightsInternalEvent.Loaded -> {
                val payload = event.payload
                InsightsUiState.Ready(
                    yourBypassesToday = payload.yourBypassesToday,
                    rivalDailyTargetCount = payload.rivalDailyTargetCount,
                    isWinningDuel = payload.yourBypassesToday <= payload.rivalDailyTargetCount,
                    weeklyTrend = payload.weeklyTrend,
                    appBreakdowns = payload.appBreakdowns,
                    totalMinutesSavedThisWeek = payload.totalMinutesSavedThisWeek,
                )
            }
        }
    }

    fun aggregatePayload(
        events: List<InterceptEventEntity>,
        startOfTodayEpochMillis: Long,
        rivalDailyTargetCount: Int,
    ): InsightsRawPayload {
        val todayEvents = events.filter { it.timestampEpochMillis >= startOfTodayEpochMillis }
        val yourBypassesToday = todayEvents.count { it.outcomeName == InterceptOutcome.BYPASSED.name }
        val zone = ZoneId.systemDefault()

        val trend = (0 until DAYS_IN_WEEK).map { offsetFromOldest ->
            val daysAgo = (DAYS_IN_WEEK - 1) - offsetFromOldest
            val dayStart = startOfTodayEpochMillis - (daysAgo * MILLIS_PER_DAY)
            val dayEnd = dayStart + MILLIS_PER_DAY
            val dayEvents = events.filter {
                it.timestampEpochMillis >= dayStart && it.timestampEpochMillis < dayEnd
            }
            val score = computeDayHealth(dayEvents)
            DailyTrendPoint(
                dayOfWeek = Instant.ofEpochMilli(dayStart).atZone(zone).dayOfWeek,
                brainHealth = score,
                resistedCount = dayEvents.count { it.outcomeName == InterceptOutcome.RESISTED.name },
            )
        }

        val breakdowns = todayEvents
            .groupBy { it.packageName }
            .map { (pkgName, appEvents) ->
                val appLabel = appEvents.first().appDisplayName
                AppBreakdownItem(
                    packageName = AppPackageName(pkgName),
                    appDisplayName = appLabel,
                    resistedCount = appEvents.count { it.outcomeName == InterceptOutcome.RESISTED.name },
                    bypassedCount = appEvents.count { it.outcomeName == InterceptOutcome.BYPASSED.name },
                    minutesSaved = DailyScreenMinutes(appEvents.sumOf { it.estimatedMinutesSaved }),
                )
            }
            .sortedByDescending { it.resistedCount + it.bypassedCount }

        val totalWeekMinutes = events.sumOf { it.estimatedMinutesSaved }
        return InsightsRawPayload(
            yourBypassesToday = yourBypassesToday,
            rivalDailyTargetCount = rivalDailyTargetCount,
            weeklyTrend = trend,
            appBreakdowns = breakdowns,
            totalMinutesSavedThisWeek = DailyScreenMinutes(totalWeekMinutes),
        )
    }

    private fun computeDayHealth(events: List<InterceptEventEntity>): BrainHealthPercent {
        var score = BASE_HEALTH
        events.forEach { event ->
            when (event.outcomeName) {
                InterceptOutcome.RESISTED.name -> score += RESISTED_BONUS
                InterceptOutcome.BYPASSED.name -> score -= BYPASSED_PENALTY
            }
        }
        return BrainHealthPercent(score.coerceIn(MIN_HEALTH, MAX_HEALTH))
    }
}
