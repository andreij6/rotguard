package com.rotguard.app.feature.insights

import com.rotguard.app.R
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.DailyScreenMinutes
import java.io.IOException
import java.time.DayOfWeek
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
class InsightsViewModelTest {

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
    fun `loads weekly insights into Ready state and recovers from IO error`() = runTest {
        val dataSource = FakeInsightsDataSource(shouldFail = true)
        val viewModel = InsightsViewModel(dataSource)
        advanceUntilIdle()

        assertEquals(InsightsUiState.Error(R.string.insights_error_load), viewModel.uiState.value)

        dataSource.shouldFail = false
        viewModel.onAction(InsightsAction.RetryTapped)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as InsightsUiState.Ready
        assertEquals(20, ready.totalMinutesSavedThisWeek.value)
        assertTrue(ready.isWinningDuel)
    }

    private class FakeInsightsDataSource(var shouldFail: Boolean) : InsightsDataSource {
        override suspend fun fetchWeeklyInsightsFromDatabase(): InsightsRawPayload {
            if (shouldFail) throw IOException("Insights load failed")
            return InsightsRawPayload(
                yourBypassesToday = 1,
                rivalDailyTargetCount = 14,
                weeklyTrend = listOf(DailyTrendPoint(DayOfWeek.MONDAY, BrainHealthPercent(88), 4)),
                appBreakdowns = emptyList(),
                totalMinutesSavedThisWeek = DailyScreenMinutes(20),
            )
        }
    }
}
