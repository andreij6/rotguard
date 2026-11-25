package com.rotguard.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.InspirationPhotoId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates the interactive onboarding tutorial, haptic feedback, photo setup, and pledge persistence.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream and one-shot [effects] for opening System Settings
 *   or transitioning into the main Home screen after setup completes.
 * - Owns the 3-second hold-to-pledge timer and triggers tactile feedback through [HapticConductor].
 *
 * **Side Effects**
 * - Queries device packages, stores inspiration photos, and persists calibration via [OnboardingDataSource].
 * - Emits hardware haptic waveforms/primitives via [HapticConductor].
 *
 * **Invariants**
 * - Releasing the pledge hold before 3000ms cancels the hold timer and resets progress to `0f`.
 * - Caught coroutine exceptions always rethrow [CancellationException].
 *
 * **Anti-Patterns**
 * - Never let Compose UI launch hold timers or mutate business state directly; always dispatch [OnboardingAction].
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingDataSource,
    private val hapticConductor: HapticConductor,
    private val clockProvider: ClockProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()

    private var pledgeHoldJob: Job? = null

    init {
        loadOnboardingState()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SimulatorStageSelected,
            is OnboardingAction.DailyHoursSelected,
            is OnboardingAction.GoalToggled,
            is OnboardingAction.PeakWindowSelected,
            is OnboardingAction.PersonalMantraChanged,
            is OnboardingAction.GuardedAppToggled,
            is OnboardingAction.InspirationPhotoPicked,
            is OnboardingAction.InspirationPhotoRemoved,
            is OnboardingAction.InspirationPhotoToggled,
            -> handleInputOrSelectionAction(action)
            else -> handleNavigationOrPledgeAction(action)
        }
    }

    private fun handleInputOrSelectionAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SimulatorStageSelected -> {
                applyEvent(OnboardingInternalEvent.SimulatorStageUpdated(action.stage))
                hapticConductor.playRotDecayShudder(action.stage.levelIndex)
            }
            is OnboardingAction.DailyHoursSelected -> {
                applyEvent(OnboardingInternalEvent.DailyHoursUpdated(action.hours))
                hapticConductor.playSelectionTick()
            }
            is OnboardingAction.GoalToggled -> {
                applyEvent(OnboardingInternalEvent.GoalSelectionToggled(action.goalId))
                hapticConductor.playSelectionTick()
            }
            is OnboardingAction.PeakWindowSelected -> {
                applyEvent(OnboardingInternalEvent.PeakWindowUpdated(action.windowLabel))
                hapticConductor.playSelectionTick()
            }
            is OnboardingAction.PersonalMantraChanged -> {
                applyEvent(OnboardingInternalEvent.PersonalMantraUpdated(action.mantraText))
            }
            is OnboardingAction.InspirationPhotoPicked -> handleInspirationPhotoPicked(action.uriString)
            is OnboardingAction.InspirationPhotoRemoved -> handleInspirationPhotoRemoved(action.photoId)
            is OnboardingAction.InspirationPhotoToggled -> handleInspirationPhotoToggled(action.photoId)
            is OnboardingAction.GuardedAppToggled -> {
                applyEvent(OnboardingInternalEvent.AppChoiceToggled(action.packageName))
                hapticConductor.playSelectionTick()
            }
            else -> Unit
        }
    }

    private fun handleNavigationOrPledgeAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.NextStepTapped -> handleNextStep()
            OnboardingAction.PreviousStepTapped -> {
                cancelHoldJob()
                hapticConductor.playButtonPress()
                applyEvent(OnboardingInternalEvent.StepRewound)
            }
            OnboardingAction.OpenAccessibilityTapped -> {
                hapticConductor.playButtonPress()
                _effects.tryEmit(OnboardingEffect.OpenAccessibilitySettings)
            }
            OnboardingAction.OpenOverlayTapped -> {
                hapticConductor.playButtonPress()
                _effects.tryEmit(OnboardingEffect.OpenOverlaySettings)
            }
            OnboardingAction.PermissionsRefreshed -> refreshPermissions()
            OnboardingAction.PledgeHoldStarted -> startPledgeHold()
            OnboardingAction.PledgeHoldReleased -> releasePledgeHold()
            OnboardingAction.FinishOnboardingTapped -> {
                hapticConductor.playVictoryChord()
                _effects.tryEmit(OnboardingEffect.NavigateToDashboard)
            }
            OnboardingAction.RetryTapped -> loadOnboardingState()
            else -> Unit
        }
    }

    private fun loadOnboardingState() {
        applyEvent(OnboardingInternalEvent.LoadStarted)
        viewModelScope.launch {
            try {
                val payload = repository.loadInitialOnboardingData()
                applyEvent(
                    OnboardingInternalEvent.Loaded(
                        appChoices = payload.appChoices,
                        accessibilityGranted = payload.permissions.accessibilityEnabled,
                        overlayGranted = payload.permissions.overlayEnabled,
                        existingDailyHours = payload.existingDailyHours,
                        existingGoalIds = payload.existingGoalIds,
                        existingPeakWindow = payload.existingPeakWindow,
                        existingMantra = payload.existingMantra,
                        existingInspirationPhotos = payload.inspirationPhotos,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(OnboardingInternalEvent.LoadFailed(R.string.onboarding_error_load))
            } catch (_: IllegalStateException) {
                applyEvent(OnboardingInternalEvent.LoadFailed(R.string.onboarding_error_load))
            }
        }
    }

    private fun handleInspirationPhotoPicked(uriString: String) {
        applyEvent(OnboardingInternalEvent.InspirationPhotoSaveStarted)
        viewModelScope.launch {
            try {
                val added = repository.addOnboardingInspirationPhoto(uriString)
                hapticConductor.playVictoryChord()
                applyEvent(OnboardingInternalEvent.InspirationPhotoAdded(added))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(OnboardingInternalEvent.InspirationPhotoSaveFailed(R.string.onboarding_error_photo))
            } catch (_: IllegalStateException) {
                applyEvent(OnboardingInternalEvent.InspirationPhotoSaveFailed(R.string.onboarding_error_photo))
            }
        }
    }

    private fun handleInspirationPhotoToggled(photoId: InspirationPhotoId) {
        val active = _uiState.value as? OnboardingUiState.Active ?: return
        val photo = active.inspirationPhotos.firstOrNull { it.id == photoId } ?: return
        val enabled = !photo.isEnabled
        hapticConductor.playSelectionTick()
        applyEvent(OnboardingInternalEvent.InspirationPhotoEnabledChanged(photoId, enabled))
        viewModelScope.launch {
            try {
                repository.setOnboardingInspirationPhotoEnabled(photoId, enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Keep UI responsive; next load re-syncs from storage.
            }
        }
    }

    private fun handleInspirationPhotoRemoved(photoId: InspirationPhotoId) {
        hapticConductor.playSelectionTick()
        applyEvent(OnboardingInternalEvent.InspirationPhotoDeleted(photoId))
        viewModelScope.launch {
            try {
                repository.removeOnboardingInspirationPhoto(photoId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Keep UI responsive; next load re-syncs from storage.
            }
        }
    }

    private fun handleNextStep() {
        val current = _uiState.value as? OnboardingUiState.Active ?: return
        hapticConductor.playButtonPress()
        if (current.step == OnboardingStep.COMPLETE) {
            _effects.tryEmit(OnboardingEffect.NavigateToDashboard)
            return
        }
        applyEvent(OnboardingInternalEvent.StepAdvanced)
    }

    private fun refreshPermissions() {
        val snapshot = repository.inspectCurrentPermissions()
        applyEvent(
            OnboardingInternalEvent.PermissionsUpdated(
                accessibilityGranted = snapshot.accessibilityEnabled,
                overlayGranted = snapshot.overlayEnabled,
            ),
        )
    }

    private fun startPledgeHold() {
        val current = _uiState.value as? OnboardingUiState.Active ?: return
        if (current.step != OnboardingStep.COMMITMENT_PLEDGE || current.isHoldingPledge) return

        cancelHoldJob()
        applyEvent(OnboardingInternalEvent.PledgeHoldBegan)
        hapticConductor.playHoldProgressTick(INITIAL_HOLD_PULSE_FRACTION)

        pledgeHoldJob = viewModelScope.launch {
            try {
                val startEpochMs = clockProvider.nowEpochMillis().value
                var loopElapsedMs = 0L
                var elapsedMs = 0L
                var nextHapticTickMs = HAPTIC_BEAT_INTERVAL_MS
                while (elapsedMs < PLEDGE_HOLD_DURATION_MS) {
                    delay(HOLD_FRAME_STEP_MS)
                    loopElapsedMs += HOLD_FRAME_STEP_MS
                    val wallElapsedMs = (clockProvider.nowEpochMillis().value - startEpochMs).coerceAtLeast(0L)
                    elapsedMs = maxOf(loopElapsedMs, wallElapsedMs).coerceAtMost(PLEDGE_HOLD_DURATION_MS)
                    val fraction = elapsedMs.toFloat() / PLEDGE_HOLD_DURATION_MS.toFloat()
                    applyEvent(OnboardingInternalEvent.PledgeHoldProgressed(fraction))
                    if (elapsedMs >= nextHapticTickMs && elapsedMs < PLEDGE_HOLD_DURATION_MS) {
                        hapticConductor.playHoldProgressTick(fraction)
                        nextHapticTickMs += HAPTIC_BEAT_INTERVAL_MS
                    }
                }
                val latest = _uiState.value as? OnboardingUiState.Active ?: return@launch
                repository.saveOnboardingCommitmentToDatabase(
                    OnboardingCommitmentRequest(
                        dailyDoomscrollHours = latest.dailyDoomscrollHours,
                        selectedGoalIds = latest.selectedGoalIds,
                        peakWindow = latest.selectedPeakWindow,
                        personalMantra = latest.personalMantra,
                        appChoices = latest.appChoices,
                    ),
                )
                hapticConductor.playVictoryChord()
                applyEvent(OnboardingInternalEvent.PledgeHoldCompleted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(OnboardingInternalEvent.LoadFailed(R.string.onboarding_error_save))
            } catch (_: IllegalStateException) {
                applyEvent(OnboardingInternalEvent.LoadFailed(R.string.onboarding_error_save))
            }
        }
    }

    private fun releasePledgeHold() {
        val current = _uiState.value as? OnboardingUiState.Active ?: return
        if (!current.isHoldingPledge || current.step == OnboardingStep.COMPLETE) return
        cancelHoldJob()
        hapticConductor.playReleaseSoftTick()
        applyEvent(OnboardingInternalEvent.PledgeHoldReleasedEarly)
    }

    private fun cancelHoldJob() {
        pledgeHoldJob?.cancel()
        pledgeHoldJob = null
    }

    private fun applyEvent(event: OnboardingInternalEvent) {
        _uiState.value = OnboardingReducer.reduce(_uiState.value, event)
    }

    override fun onCleared() {
        cancelHoldJob()
        super.onCleared()
    }

    companion object {
        const val PLEDGE_HOLD_DURATION_MS = 3_000L
        private const val INITIAL_HOLD_PULSE_FRACTION = 0.1f
        private const val HOLD_FRAME_STEP_MS = 50L
        private const val HAPTIC_BEAT_INTERVAL_MS = 350L
        private const val EFFECT_BUFFER_CAPACITY = 8
    }
}
