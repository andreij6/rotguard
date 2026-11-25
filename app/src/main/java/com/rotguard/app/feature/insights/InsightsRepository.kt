package com.rotguard.app.feature.insights

import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.InterceptEventDao
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

interface InsightsDataSource {
    suspend fun fetchWeeklyInsightsFromDatabase(): InsightsRawPayload
}

/**
 * Room-backed insights repository fetching 7-day intercept analytics and duel targets.
 *
 * **Responsibility & Boundaries**
 * - Loads the last 7 days of `intercept_events` and user duel configuration from Room, delegating
 *   aggregation to [InsightsReducer.aggregatePayload].
 *
 * **Side Effects**
 * - Queries `intercept_events` and `user_profile` tables in Room.
 *
 * **Invariants**
 * - Queries a clean 7-day rolling window anchored at [ClockProvider.startOfTodayEpochMillis].
 *
 * **Anti-Patterns**
 * - Never mutate Room or trigger haptics during read-only analytics loads.
 */
@Singleton
class InsightsRepository @Inject constructor(
    private val interceptEventDao: InterceptEventDao,
    private val userProfileDao: UserProfileDao,
    private val clockProvider: ClockProvider,
) : InsightsDataSource {

    override suspend fun fetchWeeklyInsightsFromDatabase(): InsightsRawPayload {
        val profile = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        val startOfToday = clockProvider.startOfTodayEpochMillis().value
        val sixDaysAgoStart = startOfToday - (SIX_DAYS * MILLIS_PER_DAY)
        val events = interceptEventDao.getEventsSinceOnce(sixDaysAgoStart)
        return InsightsReducer.aggregatePayload(
            events = events,
            startOfTodayEpochMillis = startOfToday,
            rivalDailyTargetCount = profile.rivalDailyTargetCount,
        )
    }

    private companion object {
        const val SIX_DAYS = 6L
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
