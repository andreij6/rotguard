package com.rotguard.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.ThemeMode
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
 * Manages Settings state, appearance theme selection, permission checks, and navigation shortcuts.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream and one-shot [effects] for launching System Settings,
 *   opening Inspiration, or replaying the onboarding setup.
 *
 * **Side Effects**
 * - Reads/writes user preferences via [SettingsDataSource] and fires tactile taps via [HapticConductor].
 *
 * **Invariants**
 * - Always rethrows [CancellationException] from coroutine catch blocks.
 *
 * **Anti-Patterns**
 * - Never construct Android Settings `Intent`s inside the ViewModel; emit [SettingsEffect] instead.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSource: SettingsDataSource,
    private val hapticConductor: HapticConductor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    init {
        loadSettings()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.RefreshRequested -> loadSettings()
            SettingsAction.RetryTapped -> loadSettings()
            SettingsAction.OpenAccessibilityTapped -> emitWithTick(SettingsEffect.OpenAccessibilitySettings)
            SettingsAction.OpenOverlayTapped -> emitWithTick(SettingsEffect.OpenOverlaySettings)
            SettingsAction.OpenUsageAccessTapped -> emitWithTick(SettingsEffect.OpenUsageAccessSettings)
            SettingsAction.OpenInspirationTapped -> emitWithTick(SettingsEffect.NavigateToInspiration)
            SettingsAction.ReplayOnboardingTapped -> emitWithTick(SettingsEffect.ReplayOnboarding)
            is SettingsAction.HoldDurationSelected -> updateHoldDuration(action.durationMillis)
            is SettingsAction.GraceWindowSelected -> updateGraceWindow(action.durationMillis)
            SettingsAction.HapticsToggled -> toggleHaptics()
            is SettingsAction.ThemeModeSelected -> updateThemeMode(action.themeMode)
            is SettingsAction.MantraDraftChanged -> {
                _uiState.value = SettingsReducer.reduce(
                    _uiState.value,
                    SettingsInternalEvent.MantraDraftUpdated(action.mantraText),
                )
            }
            SettingsAction.SaveMantraTapped -> saveMantra()
        }
    }

    private fun loadSettings() {
        val current = _uiState.value
        if (current !is SettingsUiState.Ready) {
            _uiState.value = SettingsReducer.reduce(current, SettingsInternalEvent.LoadStarted)
        }
        viewModelScope.launch {
            try {
                val snapshot = dataSource.loadSettingsSnapshotFromDatabase()
                _uiState.value = SettingsReducer.reduce(_uiState.value, SettingsInternalEvent.Loaded(snapshot))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = SettingsReducer.reduce(
                    _uiState.value,
                    SettingsInternalEvent.LoadFailed(R.string.settings_error_load),
                )
            } catch (_: IllegalStateException) {
                _uiState.value = SettingsReducer.reduce(
                    _uiState.value,
                    SettingsInternalEvent.LoadFailed(R.string.settings_error_load),
                )
            }
        }
    }

    private fun updateHoldDuration(durationMillis: HoldDurationMillis) {
        hapticConductor.playSelectionTick()
        _uiState.value = SettingsReducer.reduce(
            _uiState.value,
            SettingsInternalEvent.HoldDurationUpdated(durationMillis),
        )
        viewModelScope.launch {
            try {
                dataSource.updateHoldDurationInDatabase(durationMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Next refresh syncs persisted preference.
            }
        }
    }

    private fun updateGraceWindow(durationMillis: HoldDurationMillis) {
        hapticConductor.playSelectionTick()
        _uiState.value = SettingsReducer.reduce(
            _uiState.value,
            SettingsInternalEvent.GraceWindowUpdated(durationMillis),
        )
        viewModelScope.launch {
            try {
                dataSource.updateGraceWindowInDatabase(durationMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Next refresh syncs persisted preference.
            }
        }
    }

    private fun toggleHaptics() {
        val ready = _uiState.value as? SettingsUiState.Ready ?: return
        val nextEnabled = !ready.hapticsEnabled
        if (nextEnabled) hapticConductor.playSelectionTick()
        _uiState.value = SettingsReducer.reduce(ready, SettingsInternalEvent.HapticsUpdated(nextEnabled))
        viewModelScope.launch {
            try {
                dataSource.updateHapticsEnabledInDatabase(nextEnabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = SettingsReducer.reduce(
                    _uiState.value,
                    SettingsInternalEvent.HapticsUpdated(ready.hapticsEnabled),
                )
            }
        }
    }

    private fun updateThemeMode(themeMode: ThemeMode) {
        hapticConductor.playSelectionTick()
        _uiState.value = SettingsReducer.reduce(
            _uiState.value,
            SettingsInternalEvent.ThemeModeUpdated(themeMode),
        )
        viewModelScope.launch {
            try {
                dataSource.updateThemeModeInDatabase(themeMode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Next refresh syncs persisted theme.
            }
        }
    }

    private fun saveMantra() {
        val ready = _uiState.value as? SettingsUiState.Ready ?: return
        hapticConductor.playVictoryChord()
        viewModelScope.launch {
            try {
                dataSource.updatePersonalMantraInDatabase(ready.personalMantraDraft)
                _uiState.value = SettingsReducer.reduce(_uiState.value, SettingsInternalEvent.MantraSaved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Leave draft intact on transient storage failure.
            }
        }
    }

    private fun emitWithTick(effect: SettingsEffect) {
        hapticConductor.playSelectionTick()
        _effects.tryEmit(effect)
    }

    private companion object {
        const val EFFECT_BUFFER_CAPACITY = 8
    }
}
