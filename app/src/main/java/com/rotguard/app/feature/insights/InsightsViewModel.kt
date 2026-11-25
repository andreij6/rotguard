package com.rotguard.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the Insights and Focus Duel screen state.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream summarizing Focus Duel results, 7-day Brain Health trend,
 *   and per-app intercept totals.
 *
 * **Side Effects**
 * - Fetches weekly intercept history via [InsightsDataSource].
 *
 * **Invariants**
 * - Always rethrows [CancellationException] inside coroutine catch blocks.
 *
 * **Anti-Patterns**
 * - Never query Room or run date math inside `@Composable` functions.
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val dataSource: InsightsDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        refreshInsights(showLoading = true)
    }

    fun onAction(action: InsightsAction) {
        when (action) {
            InsightsAction.RefreshRequested -> refreshInsights(showLoading = false)
            InsightsAction.RetryTapped -> refreshInsights(showLoading = true)
        }
    }

    private fun refreshInsights(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = InsightsReducer.reduce(_uiState.value, InsightsInternalEvent.LoadStarted)
        }
        viewModelScope.launch {
            try {
                val payload = dataSource.fetchWeeklyInsightsFromDatabase()
                _uiState.value = InsightsReducer.reduce(_uiState.value, InsightsInternalEvent.Loaded(payload))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = InsightsReducer.reduce(
                    _uiState.value,
                    InsightsInternalEvent.LoadFailed(R.string.insights_error_load),
                )
            } catch (_: IllegalStateException) {
                _uiState.value = InsightsReducer.reduce(
                    _uiState.value,
                    InsightsInternalEvent.LoadFailed(R.string.insights_error_load),
                )
            }
        }
    }
}
