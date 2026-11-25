package com.rotguard.app.feature.shield

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.BrainHealthPercent
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.ThemeMode
import com.rotguard.app.storage.TimestampEpochMillis

@Immutable
data class BlockerInspirationVisual(
    val title: String,
    val caption: String,
    val filePath: String? = null,
    val bundledDrawableName: String? = null,
)

sealed interface ShieldOverlayUiState {
    data object Hidden : ShieldOverlayUiState

    data class Blocking(
        val packageName: AppPackageName,
        val appDisplayName: String,
        val brainHealth: BrainHealthPercent,
        val brainStage: BrainDecayStage,
        val personalMantra: String,
        val todayAttemptsCount: Int,
        val holdDurationMillis: HoldDurationMillis,
        val holdProgressFraction: Float,
        val isHolding: Boolean,
        val releasedEarlyHint: Boolean,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        @StringRes val openingPromptRes: Int = BlockerPromptCatalog.DEFAULT_OPENING,
        @StringRes val secondThoughtPromptRes: Int = BlockerPromptCatalog.DEFAULT_SECOND_THOUGHT,
        val hasTransitionedToSecondThought: Boolean = false,
        val inspirationVisual: BlockerInspirationVisual? = null,
        val todayResistedCount: Int = 0,
        @StringRes val reminderCategoryBadgeRes: Int? = null,
        /** 2nd+ pause for this app within 5 minutes: offer to stop pausing it for a while. */
        val offerGracePeriod: Boolean = false,
        /** Chosen break length if the checkbox is ticked; applied only if the user then holds to continue. */
        val graceMinutes: Int? = null,
    ) : ShieldOverlayUiState

    data class CelebratingExit(
        val appDisplayName: String,
        val themeMode: ThemeMode,
        val estimatedMinutesSaved: Int,
        val todayUrgesStoppedCount: Int,
        val inspirationVisual: BlockerInspirationVisual?,
        val celebrationProgressFraction: Float,
        /** Index into `CELEBRATION_VARIANTS`: which of the 50 celebration animations plays. */
        val celebrationVariantIndex: Int = 0,
    ) : ShieldOverlayUiState
}

sealed interface ShieldAction {
    data object ProtectBrainExitTapped : ShieldAction
    data object HoldStarted : ShieldAction
    data object HoldReleased : ShieldAction
    data object CelebrationFinishNowTapped : ShieldAction
    /** null = checkbox cleared; otherwise one of [ShieldReducer.GRACE_OPTIONS_MINUTES]. */
    data class GraceMinutesSelected(val minutes: Int?) : ShieldAction
}

sealed interface ShieldDecision {
    data object Ignore : ShieldDecision

    data class ShowOverlay(
        val packageName: AppPackageName,
        val appDisplayName: String,
        val holdDurationMillis: HoldDurationMillis,
        val brainHealth: BrainHealthPercent,
        val brainStage: BrainDecayStage,
        val personalMantra: String,
        val todayAttemptsCount: Int,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val variationIndex: Int = 0,
        @StringRes val openingPromptRes: Int = BlockerPromptCatalog.DEFAULT_OPENING,
        @StringRes val secondThoughtPromptRes: Int = BlockerPromptCatalog.DEFAULT_SECOND_THOUGHT,
        val inspirationIndex: Int = -1,
        val inspirationVisual: BlockerInspirationVisual? = null,
        val todayResistedCount: Int = 0,
        val selectedReminderId: Long = -1L,
        @StringRes val reminderCategoryBadgeRes: Int? = null,
        val offerGracePeriod: Boolean = false,
    ) : ShieldDecision
}

@Immutable
data class ShieldTrackerState(
    val activeGuardedApp: AppPackageName? = null,
    val graceGrantedUntilEpochByApp: Map<String, Long> = emptyMap(),
    val isOverlayShowing: Boolean = false,
    /**
     * The app the user just closed from the pause screen. Its late window events while it is still animating away
     * must not re-trigger the shield; cleared as soon as any other real app (usually the launcher) comes forward.
     */
    val justClosedApp: AppPackageName? = null,
)

data class ShieldEvaluationResult(
    val nextTrackerState: ShieldTrackerState,
    val decision: ShieldDecision,
)

data class ShieldEvaluationContext(
    val shieldEnabled: Boolean,
    val personalMantra: String,
    val globalHoldDurationMillis: HoldDurationMillis,
    val sessionGraceDurationMillis: HoldDurationMillis,
    val guardedAppsByPackage: Map<String, GuardedAppEntity>,
    val todayAttemptsByPackage: Map<String, Int>,
    val brainHealth: BrainHealthPercent,
    val nowEpochMillis: TimestampEpochMillis,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedVariationIndex: Int = 0,
    @StringRes val openingPromptRes: Int = BlockerPromptCatalog.DEFAULT_OPENING,
    @StringRes val secondThoughtPromptRes: Int = BlockerPromptCatalog.DEFAULT_SECOND_THOUGHT,
    val selectedInspirationIndex: Int = -1,
    val inspirationVisual: BlockerInspirationVisual? = null,
    val todayResistedCount: Int = 0,
    val selectedReminderId: Long = -1L,
    @StringRes val reminderCategoryBadgeRes: Int? = null,
    /** Pause-screen views per app in the last [ShieldReducer.GRACE_OFFER_WINDOW_MS]. */
    val recentAttemptsByPackage: Map<String, Int> = emptyMap(),
    /** Apps the user put on a break: package -> epoch millis the break ends. */
    val snoozedUntilByPackage: Map<String, Long> = emptyMap(),
)

sealed interface ShieldInternalEvent {
    data class SessionStarted(val decision: ShieldDecision.ShowOverlay) : ShieldInternalEvent
    data object HoldBegan : ShieldInternalEvent
    data class HoldProgressed(val progressFraction: Float) : ShieldInternalEvent
    data object HoldReleasedEarly : ShieldInternalEvent
    data class GraceMinutesUpdated(val minutes: Int?) : ShieldInternalEvent
    data class ExitCelebrationStarted(
        val estimatedMinutesSaved: Int,
        val todayUrgesStoppedCount: Int,
        val celebrationVariantIndex: Int = 0,
    ) : ShieldInternalEvent
    data class ExitCelebrationProgressed(val progressFraction: Float) : ShieldInternalEvent
    data object SessionDismissed : ShieldInternalEvent
}
