package com.rotguard.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import com.rotguard.app.storage.HapticConductor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the Home Focus Health dashboard state and user interactions.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream and one-shot [effects] for launching the blocker preview
 *   or navigating to Inspiration, Settings, or Blocked Apps.
 *
 * **Side Effects**
 * - Reads dashboard metrics and persists protection toggles through [DashboardDataSource].
 * - Triggers tactile feedback via [HapticConductor].
 *
 * **Invariants**
 * - Always rethrows [CancellationException] from coroutine catch blocks.
 * - Uses linear `suspend` calls rather than multi-flow nested operator chains.
 *
 * **Anti-Patterns**
 * - Never call [DashboardDataSource] or launch coroutines from Compose UI.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dataSource: DashboardDataSource,
    private val hapticConductor: HapticConductor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    init {
        refreshDashboard(showLoading = true)
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.RefreshRequested -> refreshDashboard(showLoading = false)
            DashboardAction.RetryTapped -> refreshDashboard(showLoading = true)
            DashboardAction.ToggleShieldTapped -> toggleShield()
            DashboardAction.TestShieldTapped -> emitWithTick(DashboardEffect.LaunchDemoShield)
            DashboardAction.EnablePermissionsTapped -> emitWithTick(DashboardEffect.NavigateToSettingsPermissions)
            DashboardAction.ManageAppsTapped -> emitWithTick(DashboardEffect.NavigateToGuardedApps)
            DashboardAction.OpenInspirationTapped -> emitWithTick(DashboardEffect.NavigateToInspiration)
            is DashboardAction.ResumePausingTapped -> endBreak(action.packageName)
        }
    }

    private fun refreshDashboard(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = DashboardReducer.reduce(_uiState.value, DashboardInternalEvent.LoadStarted)
        }
        viewModelScope.launch {
            try {
                val snapshot = dataSource.fetchDashboardSnapshotFromStorage()
                _uiState.value = DashboardReducer.reduce(
                    _uiState.value,
                    DashboardInternalEvent.SnapshotLoaded(snapshot),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = DashboardReducer.reduce(
                    _uiState.value,
                    DashboardInternalEvent.LoadFailed(R.string.dashboard_error_load),
                )
            } catch (_: IllegalStateException) {
                _uiState.value = DashboardReducer.reduce(
                    _uiState.value,
                    DashboardInternalEvent.LoadFailed(R.string.dashboard_error_load),
                )
            }
        }
    }

    private fun toggleShield() {
        val current = _uiState.value as? DashboardUiState.Ready ?: return
        val nextEnabled = !current.shieldEnabled
        hapticConductor.playSelectionTick()
        _uiState.value = DashboardReducer.reduce(_uiState.value, DashboardInternalEvent.ShieldToggled(nextEnabled))
        viewModelScope.launch {
            try {
                dataSource.updateShieldEnabledInDatabase(nextEnabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = DashboardReducer.reduce(
                    _uiState.value,
                    DashboardInternalEvent.ShieldToggled(current.shieldEnabled),
                )
            }
        }
    }

    private fun endBreak(packageName: String) {
        hapticConductor.playButtonPress()
        viewModelScope.launch {
            try {
                dataSource.endAppBreak(packageName)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Refresh below still shows the true state.
            }
            refreshDashboard(showLoading = false)
        }
    }

    private fun emitWithTick(effect: DashboardEffect) {
        hapticConductor.playSelectionTick()
        _effects.tryEmit(effect)
    }

    private companion object {
        const val EFFECT_BUFFER_CAPACITY = 8
    }
}
