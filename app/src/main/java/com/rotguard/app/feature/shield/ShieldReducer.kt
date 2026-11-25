package com.rotguard.app.feature.shield

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome

/**
 * Pure deterministic reducer and foreground-transition evaluator for the friction shield.
 *
 * **Responsibility & Boundaries**
 * - Owns all state transitions for [ShieldOverlayUiState] and foreground app interception rules.
 * - Handles the silent mid-hold second-thought switch and the inline post-close celebration sequence.
 * - Computes daily Focus Health deterministically from today's [InterceptEventEntity] history.
 *
 * **Side Effects**
 * - None. Must never call `WindowManager`, `AccessibilityService`, Room, or `HapticConductor`.
 *
 * **Invariants**
 * - System UI, keyboard/IME, and RotGuard's own package never trigger or clear an active visit.
 * - Leaving a guarded app for Home or another app clears per-open visit state so the next open triggers friction.
 * - `holdProgressFraction` and `celebrationProgressFraction` are strictly clamped within `0f..1f`.
 *
 * **Anti-Patterns**
 * - Never hold mutable static global flags outside [ShieldTrackerState].
 */
object ShieldReducer {

    private const val BASE_BRAIN_HEALTH = 85
    private const val RESISTED_BONUS = 3
    private const val BYPASSED_PENALTY = 12
    private const val MIN_BRAIN_HEALTH = 5
    private const val MAX_BRAIN_HEALTH = 100
    private const val MIN_HOLD_DURATION_MS = 1_000L
    private const val SECOND_THOUGHT_THRESHOLD = 0.5f

    /** Seeing the pause screen again within this window offers a break. */
    const val GRACE_OFFER_WINDOW_MS = 5 * 60 * 1_000L
    val GRACE_OPTIONS_MINUTES: List<Int> = listOf(30, 60, 120)

    private val IGNORED_TRANSIENT_PACKAGES: Set<String> = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
    )

    fun reduce(state: ShieldOverlayUiState, event: ShieldInternalEvent): ShieldOverlayUiState = when (event) {
        is ShieldInternalEvent.SessionStarted -> ShieldOverlayUiState.Blocking(
            packageName = event.decision.packageName,
            appDisplayName = event.decision.appDisplayName,
            brainHealth = event.decision.brainHealth,
            brainStage = event.decision.brainStage,
            personalMantra = event.decision.personalMantra,
            todayAttemptsCount = event.decision.todayAttemptsCount,
            holdDurationMillis = event.decision.holdDurationMillis,
            holdProgressFraction = 0f,
            isHolding = false,
            releasedEarlyHint = false,
            themeMode = event.decision.themeMode,
            openingPromptRes = event.decision.openingPromptRes,
            secondThoughtPromptRes = event.decision.secondThoughtPromptRes,
            hasTransitionedToSecondThought = false,
            inspirationVisual = event.decision.inspirationVisual,
            todayResistedCount = event.decision.todayResistedCount,
            reminderCategoryBadgeRes = event.decision.reminderCategoryBadgeRes,
            offerGracePeriod = event.decision.offerGracePeriod,
        )
        is ShieldInternalEvent.GraceMinutesUpdated -> reduceBlocking(state) {
            it.copy(graceMinutes = event.minutes?.takeIf { minutes -> minutes in GRACE_OPTIONS_MINUTES })
        }
        ShieldInternalEvent.HoldBegan -> reduceBlocking(state) {
            it.copy(isHolding = true, releasedEarlyHint = false)
        }
        is ShieldInternalEvent.HoldProgressed -> reduceBlocking(state) { blocking ->
            val clamped = event.progressFraction.coerceIn(0f, 1f)
            val transitioned = blocking.hasTransitionedToSecondThought || clamped >= SECOND_THOUGHT_THRESHOLD
            blocking.copy(
                holdProgressFraction = clamped,
                hasTransitionedToSecondThought = transitioned,
            )
        }
        ShieldInternalEvent.HoldReleasedEarly -> reduceBlocking(state) {
            it.copy(
                isHolding = false,
                holdProgressFraction = 0f,
                hasTransitionedToSecondThought = false,
                releasedEarlyHint = true,
            )
        }
        is ShieldInternalEvent.ExitCelebrationStarted -> reduceBlocking(state) { blocking ->
            ShieldOverlayUiState.CelebratingExit(
                appDisplayName = blocking.appDisplayName,
                themeMode = blocking.themeMode,
                estimatedMinutesSaved = event.estimatedMinutesSaved,
                todayUrgesStoppedCount = event.todayUrgesStoppedCount,
                inspirationVisual = blocking.inspirationVisual,
                celebrationProgressFraction = 0f,
                celebrationVariantIndex = event.celebrationVariantIndex,
            )
        }
        is ShieldInternalEvent.ExitCelebrationProgressed -> {
            if (state is ShieldOverlayUiState.CelebratingExit) {
                state.copy(celebrationProgressFraction = event.progressFraction.coerceIn(0f, 1f))
            } else {
                state
            }
        }
        ShieldInternalEvent.SessionDismissed -> ShieldOverlayUiState.Hidden
    }

    private inline fun reduceBlocking(
        state: ShieldOverlayUiState,
        transform: (ShieldOverlayUiState.Blocking) -> ShieldOverlayUiState,
    ): ShieldOverlayUiState = if (state is ShieldOverlayUiState.Blocking) transform(state) else state

    fun evaluateForegroundTransition(
        trackerState: ShieldTrackerState,
        foregroundPackageName: String,
        ownPackageName: String,
        context: ShieldEvaluationContext,
    ): ShieldEvaluationResult {
        if (isTransientOrSelfPackage(foregroundPackageName, ownPackageName) || trackerState.isOverlayShowing) {
            return ShieldEvaluationResult(trackerState, ShieldDecision.Ignore)
        }
        if (trackerState.justClosedApp?.value == foregroundPackageName) {
            return ShieldEvaluationResult(trackerState, ShieldDecision.Ignore)
        }
        return evaluateRealForegroundApp(trackerState.copy(justClosedApp = null), foregroundPackageName, context)
    }

    private fun evaluateRealForegroundApp(
        trackerState: ShieldTrackerState,
        foregroundPackageName: String,
        context: ShieldEvaluationContext,
    ): ShieldEvaluationResult {

        val guardedApp = context.guardedAppsByPackage[foregroundPackageName]
        val isShieldTarget = context.shieldEnabled && guardedApp != null && guardedApp.isGuarded
        if (!isShieldTarget) {
            return ShieldEvaluationResult(trackerState.copy(activeGuardedApp = null), ShieldDecision.Ignore)
        }

        val targetPackage = AppPackageName(foregroundPackageName)
        val now = context.nowEpochMillis.value
        val graceUntil = trackerState.graceGrantedUntilEpochByApp[foregroundPackageName]
        val snoozedUntil = context.snoozedUntilByPackage[foregroundPackageName]
        val alreadyActiveOrGraced = trackerState.activeGuardedApp == targetPackage ||
            (graceUntil != null && now < graceUntil) ||
            (snoozedUntil != null && now < snoozedUntil)
        if (alreadyActiveOrGraced) {
            return ShieldEvaluationResult(
                nextTrackerState = trackerState.copy(activeGuardedApp = targetPackage),
                decision = ShieldDecision.Ignore,
            )
        }

        val holdDuration = (guardedApp!!.customHoldDurationMillis ?: context.globalHoldDurationMillis.value)
            .coerceAtLeast(MIN_HOLD_DURATION_MS)
        val attemptsSoFar = context.todayAttemptsByPackage[foregroundPackageName] ?: 0
        val decision = ShieldDecision.ShowOverlay(
            packageName = targetPackage,
            appDisplayName = guardedApp.displayName,
            holdDurationMillis = HoldDurationMillis(holdDuration),
            brainHealth = context.brainHealth,
            brainStage = BrainDecayStage.fromHealthPercent(context.brainHealth),
            personalMantra = context.personalMantra,
            todayAttemptsCount = attemptsSoFar + 1,
            themeMode = context.themeMode,
            variationIndex = context.selectedVariationIndex,
            openingPromptRes = context.openingPromptRes,
            secondThoughtPromptRes = context.secondThoughtPromptRes,
            inspirationIndex = context.selectedInspirationIndex,
            inspirationVisual = context.inspirationVisual,
            todayResistedCount = context.todayResistedCount,
            selectedReminderId = context.selectedReminderId,
            reminderCategoryBadgeRes = context.reminderCategoryBadgeRes,
            offerGracePeriod = (context.recentAttemptsByPackage[foregroundPackageName] ?: 0) >= 1,
        )
        return ShieldEvaluationResult(
            nextTrackerState = trackerState.copy(isOverlayShowing = true),
            decision = decision,
        )
    }

    fun computeBrainHealthFromEvents(events: List<InterceptEventEntity>): BrainHealthPercent {
        var score = BASE_BRAIN_HEALTH
        events.forEach { event ->
            when (event.outcomeName) {
                InterceptOutcome.RESISTED.name -> score += RESISTED_BONUS
                InterceptOutcome.BYPASSED.name -> score -= BYPASSED_PENALTY
            }
        }
        return BrainHealthPercent(score.coerceIn(MIN_BRAIN_HEALTH, MAX_BRAIN_HEALTH))
    }

    private fun isTransientOrSelfPackage(
        foregroundPackageName: String,
        ownPackageName: String,
    ): Boolean {
        return foregroundPackageName.isBlank() ||
            foregroundPackageName == ownPackageName ||
            foregroundPackageName in IGNORED_TRANSIENT_PACKAGES ||
            foregroundPackageName.contains("inputmethod")
    }
}
