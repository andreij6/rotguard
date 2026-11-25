package com.rotguard.app.feature.shield

import com.rotguard.app.feature.shield.celebration.CelebrationDeck
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.InterceptOutcome
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates an active friction overlay session, hold timer progression, silent mid-hold transitions,
 * haptics, and the inline post-close celebration sequence.
 *
 * **Responsibility & Boundaries**
 * - Owns the single [uiState] stream observed by both [FrictionOverlayWindow] and [FrictionFallbackActivity].
 * - Executes the continuous press-and-hold timer and dispatches escalating haptic pulses via [HapticConductor].
 * - Runs the inline celebratory countdown after closing a distracted app before invoking `onExitToHome`.
 *
 * **Side Effects**
 * - Emits hardware vibrations via [HapticConductor] and persists outcomes via [ShieldDataSource].
 * - Invokes caller-supplied `onExitToHome` or `onContinueToApp` callbacks on completion.
 *
 * **Invariants**
 * - Releasing before `holdDurationMillis` cancels the active hold job and resets progress to `0f`.
 * - Caught coroutine exceptions always rethrow [CancellationException].
 *
 * **Anti-Patterns**
 * - Never launch timers from Compose UI; always forward [ShieldAction] to [onAction].
 */
@Singleton
class FrictionSessionController @Inject constructor(
    private val shieldDataSource: ShieldDataSource,
    private val hapticConductor: HapticConductor,
    private val clockProvider: ClockProvider,
    @ShieldTickerDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val controllerScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow<ShieldOverlayUiState>(ShieldOverlayUiState.Hidden)
    val uiState: StateFlow<ShieldOverlayUiState> = _uiState.asStateFlow()

    private var holdJob: Job? = null
    private var celebrationJob: Job? = null
    private var idleAutoCloseJob: Job? = null
    private var sessionCountsTowardStats = true

    // ponytail: in-memory shuffle bag; resets on process death (then may repeat a recent one). Persist if that shows.
    private val celebrationDeck = CelebrationDeck()
    private var onExitToHomeCallback: (() -> Unit)? = null
    private var onContinueToAppCallback: (() -> Unit)? = null

    /**
     * @param countsTowardStats false for "Test Pause Screen" / preview sessions: they play exactly like real ones but
     * never write an intercept outcome, so practice runs don't inflate urges stopped, time saved or focus health.
     */
    fun startBlockingSession(
        decision: ShieldDecision.ShowOverlay,
        onExitToHome: () -> Unit,
        onContinueToApp: () -> Unit,
        countsTowardStats: Boolean = true,
    ) {
        cancelActiveJobs()
        sessionCountsTowardStats = countsTowardStats
        onExitToHomeCallback = onExitToHome
        onContinueToAppCallback = onContinueToApp
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.SessionStarted(decision))
        hapticConductor.playWarningPulse(INITIAL_WARNING_INTENSITY)
        restartIdleAutoClose()
    }

    /**
     * Someone just staring at the pause screen hasn't chosen to open the app: after [IDLE_AUTO_CLOSE_MS] without a
     * hold, close it for them through the normal close path. Paused while holding, restarted on an early release.
     */
    private fun restartIdleAutoClose() {
        idleAutoCloseJob?.cancel()
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.IdleCountdownUpdated(null))
        idleAutoCloseJob = controllerScope.launch {
            // Quiet for the first stretch, then a visible per-second countdown to the auto-close.
            delay(IDLE_COUNTDOWN_SHOWN_AFTER_MS)
            var secondsLeft = ((IDLE_AUTO_CLOSE_MS - IDLE_COUNTDOWN_SHOWN_AFTER_MS) / MILLIS_PER_SECOND).toInt()
            while (secondsLeft > 0) {
                _uiState.value =
                    ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.IdleCountdownUpdated(secondsLeft))
                delay(MILLIS_PER_SECOND)
                secondsLeft--
            }
            val current = _uiState.value as? ShieldOverlayUiState.Blocking ?: return@launch
            if (!current.isHolding) handleProtectBrainExit()
        }
    }

    fun onAction(action: ShieldAction) {
        when (action) {
            ShieldAction.ProtectBrainExitTapped -> handleProtectBrainExit()
            ShieldAction.HoldStarted -> handleHoldStarted()
            ShieldAction.HoldReleased -> handleHoldReleased()
            ShieldAction.CelebrationFinishNowTapped -> finishCelebrationExit()
            is ShieldAction.GraceMinutesSelected -> {
                hapticConductor.playSelectionTick()
                _uiState.value =
                    ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.GraceMinutesUpdated(action.minutes))
            }
        }
    }

    fun dismissSilently() {
        cancelActiveJobs()
        onExitToHomeCallback = null
        onContinueToAppCallback = null
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.SessionDismissed)
    }

    private fun handleProtectBrainExit() {
        when (val current = _uiState.value) {
            is ShieldOverlayUiState.Blocking -> startInlineExitCelebration(current)
            is ShieldOverlayUiState.CelebratingExit -> finishCelebrationExit()
            ShieldOverlayUiState.Hidden -> Unit
        }
    }

    private fun startInlineExitCelebration(blocking: ShieldOverlayUiState.Blocking) {
        cancelActiveJobs()
        hapticConductor.playVictoryChord()
        _uiState.value = ShieldReducer.reduce(
            blocking,
            ShieldInternalEvent.ExitCelebrationStarted(
                estimatedMinutesSaved = MINUTES_SAVED_ON_RESIST,
                todayUrgesStoppedCount = blocking.todayResistedCount + 1,
                celebrationVariantIndex = celebrationDeck.draw(),
            ),
        )

        controllerScope.launch {
            persistOutcomeSafely(blocking, InterceptOutcome.RESISTED)
        }

        celebrationJob = controllerScope.launch {
            val startEpochMs = clockProvider.nowEpochMillis().value
            var loopElapsedMs = 0L
            var elapsedMs = 0L
            while (elapsedMs < EXIT_CELEBRATION_DURATION_MS) {
                delay(HOLD_TICK_INTERVAL_MS)
                loopElapsedMs += HOLD_TICK_INTERVAL_MS
                val wallElapsedMs = (clockProvider.nowEpochMillis().value - startEpochMs).coerceAtLeast(0L)
                elapsedMs = maxOf(loopElapsedMs, wallElapsedMs).coerceAtMost(EXIT_CELEBRATION_DURATION_MS)
                val fraction = elapsedMs.toFloat() / EXIT_CELEBRATION_DURATION_MS.toFloat()
                _uiState.value = ShieldReducer.reduce(
                    _uiState.value,
                    ShieldInternalEvent.ExitCelebrationProgressed(fraction),
                )
            }
            finishCelebrationExit()
        }
    }

    private fun finishCelebrationExit() {
        if (_uiState.value is ShieldOverlayUiState.Hidden) return
        cancelActiveJobs()
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.SessionDismissed)
        val callback = onExitToHomeCallback
        onExitToHomeCallback = null
        onContinueToAppCallback = null
        callback?.invoke()
    }

    private fun handleHoldStarted() {
        val current = _uiState.value as? ShieldOverlayUiState.Blocking ?: return
        if (current.isHolding) return

        cancelHoldJob()
        idleAutoCloseJob?.cancel()
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.HoldBegan)
        hapticConductor.playHoldProgressTick(INITIAL_HOLD_PULSE_FRACTION)

        val totalDurationMs = current.holdDurationMillis.value.coerceAtLeast(MIN_HOLD_DURATION_MS)
        holdJob = controllerScope.launch {
            val startEpochMs = clockProvider.nowEpochMillis().value
            var loopElapsedMs = 0L
            var elapsedMs = 0L
            var nextHapticBeatMs = HAPTIC_BEAT_INTERVAL_MS
            var alreadyTransitionedPrompt = current.hasTransitionedToSecondThought
            while (elapsedMs < totalDurationMs) {
                delay(HOLD_TICK_INTERVAL_MS)
                loopElapsedMs += HOLD_TICK_INTERVAL_MS
                val wallElapsedMs = (clockProvider.nowEpochMillis().value - startEpochMs).coerceAtLeast(0L)
                elapsedMs = maxOf(loopElapsedMs, wallElapsedMs).coerceAtMost(totalDurationMs)
                val fraction = elapsedMs.toFloat() / totalDurationMs.toFloat()
                val reduced = ShieldReducer.reduce(
                    _uiState.value,
                    ShieldInternalEvent.HoldProgressed(fraction),
                )
                _uiState.value = reduced

                val updatedBlocking = reduced as? ShieldOverlayUiState.Blocking
                if (updatedBlocking != null &&
                    !alreadyTransitionedPrompt &&
                    updatedBlocking.hasTransitionedToSecondThought
                ) {
                    alreadyTransitionedPrompt = true
                    hapticConductor.playSelectionTick()
                }
                if (elapsedMs >= nextHapticBeatMs && elapsedMs < totalDurationMs) {
                    hapticConductor.playHoldProgressTick(fraction)
                    nextHapticBeatMs += HAPTIC_BEAT_INTERVAL_MS
                }
            }
            completeHoldBypass(current)
        }
    }

    private suspend fun completeHoldBypass(blockingSession: ShieldOverlayUiState.Blocking) {
        val chosenBreakMinutes = (_uiState.value as? ShieldOverlayUiState.Blocking)?.graceMinutes
        hapticConductor.playSelectionTick()
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.SessionDismissed)
        val callback = onContinueToAppCallback
        onExitToHomeCallback = null
        onContinueToAppCallback = null
        persistOutcomeSafely(blockingSession, InterceptOutcome.BYPASSED)
        if (chosenBreakMinutes != null) startChosenBreakSafely(blockingSession, chosenBreakMinutes)
        callback?.invoke()
    }

    private fun handleHoldReleased() {
        val current = _uiState.value as? ShieldOverlayUiState.Blocking ?: return
        if (!current.isHolding) return
        cancelHoldJob()
        hapticConductor.playReleaseSoftTick()
        _uiState.value = ShieldReducer.reduce(_uiState.value, ShieldInternalEvent.HoldReleasedEarly)
        restartIdleAutoClose()
    }

    /** The break the user ticked on a repeat pause screen starts only once they actually hold through. */
    private suspend fun startChosenBreakSafely(blockingSession: ShieldOverlayUiState.Blocking, minutes: Int) {
        if (!sessionCountsTowardStats) return
        try {
            val until = clockProvider.nowEpochMillis().value + minutes * MILLIS_PER_MINUTE
            shieldDataSource.snoozeApp(blockingSession.packageName, until)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // Worst case the app is paused again next time; never block continuing into it.
        }
    }

    private suspend fun persistOutcomeSafely(
        blockingSession: ShieldOverlayUiState.Blocking,
        outcome: InterceptOutcome,
    ) {
        if (!sessionCountsTowardStats) return
        try {
            shieldDataSource.recordInterceptOutcomeToDatabase(
                packageName = blockingSession.packageName,
                appDisplayName = blockingSession.appDisplayName,
                outcome = outcome,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // Avoid crashing overlay exit if storage write fails transiently.
        } catch (_: IllegalStateException) {
            // Avoid crashing overlay exit if database is closing.
        }
    }

    private fun cancelHoldJob() {
        holdJob?.cancel()
        holdJob = null
    }

    private fun cancelActiveJobs() {
        cancelHoldJob()
        idleAutoCloseJob?.cancel()
        idleAutoCloseJob = null
        celebrationJob?.cancel()
        celebrationJob = null
    }

    companion object {
        const val EXIT_CELEBRATION_DURATION_MS = 1_550L
        const val IDLE_AUTO_CLOSE_MS = 15_000L
        const val IDLE_COUNTDOWN_SHOWN_AFTER_MS = 8_000L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MINUTES_SAVED_ON_RESIST = 5
        private const val INITIAL_WARNING_INTENSITY = 0.78f
        private const val INITIAL_HOLD_PULSE_FRACTION = 0.12f
        private const val HOLD_TICK_INTERVAL_MS = 50L
        private const val HAPTIC_BEAT_INTERVAL_MS = 350L
        private const val MIN_HOLD_DURATION_MS = 1_000L
    }
}
