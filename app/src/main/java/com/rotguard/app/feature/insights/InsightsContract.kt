package com.rotguard.app.feature.insights

import androidx.annotation.StringRes
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.DailyScreenMinutes
import java.time.DayOfWeek

data class DailyTrendPoint(
    val dayOfWeek: DayOfWeek,
    val brainHealth: BrainHealthPercent,
    val resistedCount: Int,
)

data class AppBreakdownItem(
    val packageName: AppPackageName,
    val appDisplayName: String,
    val resistedCount: Int,
    val bypassedCount: Int,
    val minutesSaved: DailyScreenMinutes,
)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Ready(
        val yourBypassesToday: Int,
        val rivalDailyTargetCount: Int,
        val isWinningDuel: Boolean,
        val weeklyTrend: List<DailyTrendPoint>,
        val appBreakdowns: List<AppBreakdownItem>,
        val totalMinutesSavedThisWeek: DailyScreenMinutes,
    ) : InsightsUiState

    data class Error(@StringRes val messageRes: Int) : InsightsUiState
}

sealed interface InsightsAction {
    data object RefreshRequested : InsightsAction
    data object RetryTapped : InsightsAction
}

sealed interface InsightsInternalEvent {
    data object LoadStarted : InsightsInternalEvent
    data class Loaded(val payload: InsightsRawPayload) : InsightsInternalEvent
    data class LoadFailed(@StringRes val messageRes: Int) : InsightsInternalEvent
}

data class InsightsRawPayload(
    val yourBypassesToday: Int,
    val rivalDailyTargetCount: Int,
    val weeklyTrend: List<DailyTrendPoint>,
    val appBreakdowns: List<AppBreakdownItem>,
    val totalMinutesSavedThisWeek: DailyScreenMinutes,
)
