package com.rotguard.app.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.HoldDurationMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the Guarded Apps picker, search query, and global friction hold presets.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream driving the Guarded Apps screen.
 *
 * **Side Effects**
 * - Loads installed apps and persists toggle / duration updates via [GuardedAppsDataSource].
 * - Emits tactile selection ticks via [HapticConductor].
 *
 * **Invariants**
 * - Always rethrows [CancellationException] inside coroutine catch blocks.
 *
 * **Anti-Patterns**
 * - Never query `PackageManager` or mutate Room directly from Composables.
 */
@HiltViewModel
class GuardedAppsViewModel @Inject constructor(
    private val dataSource: GuardedAppsDataSource,
    private val hapticConductor: HapticConductor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuardedAppsUiState>(GuardedAppsUiState.Loading)
    val uiState: StateFlow<GuardedAppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun onAction(action: GuardedAppsAction) {
        when (action) {
            is GuardedAppsAction.SearchQueryChanged -> {
                _uiState.value = GuardedAppsReducer.reduce(
                    _uiState.value,
                    GuardedAppsInternalEvent.SearchUpdated(action.query),
                )
            }
            is GuardedAppsAction.GlobalHoldDurationSelected -> updateGlobalHold(action.durationMillis)
            is GuardedAppsAction.AppGuardToggled -> toggleApp(action.packageName)
            GuardedAppsAction.RetryTapped -> loadApps()
        }
    }

    private fun loadApps() {
        _uiState.value = GuardedAppsReducer.reduce(_uiState.value, GuardedAppsInternalEvent.LoadStarted)
        viewModelScope.launch {
            try {
                val snapshot = dataSource.fetchInstalledAndGuardedApps()
                _uiState.value = GuardedAppsReducer.reduce(
                    _uiState.value,
                    GuardedAppsInternalEvent.Loaded(snapshot.globalHoldDurationMillis, snapshot.apps),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = GuardedAppsReducer.reduce(
                    _uiState.value,
                    GuardedAppsInternalEvent.LoadFailed(R.string.apps_error_load),
                )
            } catch (_: IllegalStateException) {
                _uiState.value = GuardedAppsReducer.reduce(
                    _uiState.value,
                    GuardedAppsInternalEvent.LoadFailed(R.string.apps_error_load),
                )
            }
        }
    }

    private fun toggleApp(packageName: AppPackageName) {
        val current = _uiState.value as? GuardedAppsUiState.Ready ?: return
        val targetRow = current.allApps.firstOrNull { it.packageName == packageName } ?: return
        val nextGuarded = !targetRow.isGuarded
        hapticConductor.playSelectionTick()
        _uiState.value = GuardedAppsReducer.reduce(current, GuardedAppsInternalEvent.AppToggled(packageName))
        viewModelScope.launch {
            try {
                dataSource.persistAppGuardToggle(packageName, nextGuarded)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = GuardedAppsReducer.reduce(
                    _uiState.value,
                    GuardedAppsInternalEvent.AppToggled(packageName),
                )
            }
        }
    }

    private fun updateGlobalHold(durationMillis: HoldDurationMillis) {
        hapticConductor.playSelectionTick()
        _uiState.value = GuardedAppsReducer.reduce(
            _uiState.value,
            GuardedAppsInternalEvent.GlobalHoldDurationUpdated(durationMillis),
        )
        viewModelScope.launch {
            try {
                dataSource.persistGlobalHoldDuration(durationMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Keep optimistic UI state; next refresh syncs from Room.
            }
        }
    }
}
