package com.rotguard.app.feature.onboarding

import com.rotguard.app.R
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.StarterTextCatalog
import com.rotguard.app.storage.UserGoalId
import kotlin.math.roundToInt

/**
 * Pure deterministic reducer for the onboarding calibration flow.
 *
 * **Responsibility & Boundaries**
 * - Owns all state transitions across the 8-step onboarding wizard, impact simulator math,
 *   life-cost calculations, inspiration photo staging, and hold-to-pledge progress state.
 * - Purely transforms `(OnboardingUiState, OnboardingInternalEvent) -> OnboardingUiState`.
 *
 * **Side Effects**
 * - None. Must never access Room DAOs, Android Context, coroutines, or haptics.
 *
 * **Invariants**
 * - `dailyDoomscrollHours` is always clamped to `1..8`.
 * - `pledgeHoldProgressFraction` is always clamped to `0f..1f`.
 *
 * **Anti-Patterns**
 * - Never launch timers or persist Room entities inside [reduce].
 */
object OnboardingReducer {

    private const val DAYS_PER_YEAR = 365.0
    private const val HOURS_PER_DAY = 24.0
    private const val WAKING_HOURS_PER_DAY = 16.0
    private const val PROJECTION_YEARS = 50.0
    private const val TENTHS_MULTIPLIER = 10.0
    private const val MIN_DAILY_HOURS = 1
    private const val MAX_DAILY_HOURS = 8
    private const val MAX_MANTRA_CHARS = 80

    val DEFAULT_GOALS: List<OnboardingGoalOption> = listOf(
        OnboardingGoalOption(
            id = UserGoalId("deep_work"),
            titleRes = R.string.goal_deep_work_title,
            subtitleRes = R.string.goal_deep_work_subtitle,
            suggestedMantra = StarterTextCatalog.MANTRA_OUTLAST_FEED,
        ),
        OnboardingGoalOption(
            id = UserGoalId("better_sleep"),
            titleRes = R.string.goal_better_sleep_title,
            subtitleRes = R.string.goal_better_sleep_subtitle,
            suggestedMantra = StarterTextCatalog.MANTRA_PROTECT_REST,
        ),
        OnboardingGoalOption(
            id = UserGoalId("create_ship"),
            titleRes = R.string.goal_create_title,
            subtitleRes = R.string.goal_create_subtitle,
            suggestedMantra = StarterTextCatalog.MANTRA_CREATE_FIRST,
        ),
        OnboardingGoalOption(
            id = UserGoalId("present_offline"),
            titleRes = R.string.goal_present_title,
            subtitleRes = R.string.goal_present_subtitle,
            suggestedMantra = StarterTextCatalog.MANTRA_BE_PRESENT_HERE,
        ),
        OnboardingGoalOption(
            id = UserGoalId("read_daily"),
            titleRes = R.string.goal_read_title,
            subtitleRes = R.string.goal_read_subtitle,
            suggestedMantra = StarterTextCatalog.MANTRA_TEN_PAGES,
        ),
    )

    /**
     * Stored English labels; they are Room keys (`peakRotWindow`), shown translated via
     * [StarterTextCatalog.resFor].
     */
    val DEFAULT_PEAK_WINDOWS: List<String> = listOf(
        "Morning Wake-Up",
        "Midday Slump",
        "Between Classes / Meetings",
        "After Work / School",
        StarterTextCatalog.PEAK_LATE_NIGHT,
    )

    fun reduce(state: OnboardingUiState, event: OnboardingInternalEvent): OnboardingUiState {
        return when (event) {
            OnboardingInternalEvent.LoadStarted -> OnboardingUiState.Loading
            is OnboardingInternalEvent.LoadFailed -> OnboardingUiState.Error(event.messageRes)
            is OnboardingInternalEvent.Loaded -> createActiveState(event)
            else -> reduceActiveState(state, event)
        }
    }

    private fun reduceActiveState(
        state: OnboardingUiState,
        event: OnboardingInternalEvent,
    ): OnboardingUiState {
        val current = state as? OnboardingUiState.Active ?: return state
        return when (event) {
            OnboardingInternalEvent.StepAdvanced,
            OnboardingInternalEvent.StepRewound,
            is OnboardingInternalEvent.SimulatorStageUpdated,
            is OnboardingInternalEvent.DailyHoursUpdated,
            -> reduceStepAndAudit(current, event)
            is OnboardingInternalEvent.GoalSelectionToggled,
            is OnboardingInternalEvent.PeakWindowUpdated,
            is OnboardingInternalEvent.PersonalMantraUpdated,
            OnboardingInternalEvent.InspirationPhotoSaveStarted,
            is OnboardingInternalEvent.InspirationPhotoAdded,
            is OnboardingInternalEvent.InspirationPhotoSaveFailed,
            is OnboardingInternalEvent.InspirationPhotoDeleted,
            is OnboardingInternalEvent.InspirationPhotoEnabledChanged,
            is OnboardingInternalEvent.AppChoiceToggled,
            -> reducePreferences(current, event)
            else -> reducePermissionsAndPledge(current, event)
        }
    }

    private fun reduceStepAndAudit(
        current: OnboardingUiState.Active,
        event: OnboardingInternalEvent,
    ): OnboardingUiState.Active = when (event) {
        OnboardingInternalEvent.StepAdvanced -> current.copy(
            step = nextStep(current.step),
            releasedPledgeEarlyHint = false,
            inspirationPhotoErrorRes = null,
        )
        OnboardingInternalEvent.StepRewound -> current.copy(
            step = previousStep(current.step),
            pledgeHoldProgressFraction = 0f,
            isHoldingPledge = false,
            releasedPledgeEarlyHint = false,
            inspirationPhotoErrorRes = null,
        )
        is OnboardingInternalEvent.SimulatorStageUpdated -> current.copy(simulatorStage = event.stage)
        is OnboardingInternalEvent.DailyHoursUpdated -> {
            val clamped = event.hours.coerceIn(MIN_DAILY_HOURS, MAX_DAILY_HOURS)
            current.copy(
                dailyDoomscrollHours = clamped,
                daysLostPerYear = computeDaysLostPerYear(clamped),
                wakingYearsLostTenthsOver50Years = computeWakingYearsTenthsOver50Years(clamped),
            )
        }
        else -> current
    }

    private fun reducePreferences(
        current: OnboardingUiState.Active,
        event: OnboardingInternalEvent,
    ): OnboardingUiState.Active = when (event) {
        is OnboardingInternalEvent.GoalSelectionToggled -> {
            val updated = if (event.goalId in current.selectedGoalIds) {
                current.selectedGoalIds - event.goalId
            } else {
                current.selectedGoalIds + event.goalId
            }
            current.copy(selectedGoalIds = updated)
        }
        is OnboardingInternalEvent.PeakWindowUpdated -> current.copy(selectedPeakWindow = event.windowLabel)
        is OnboardingInternalEvent.PersonalMantraUpdated -> current.copy(
            personalMantra = event.mantraText.take(MAX_MANTRA_CHARS),
        )
        OnboardingInternalEvent.InspirationPhotoSaveStarted -> current.copy(
            isAddingInspirationPhoto = true,
            inspirationPhotoErrorRes = null,
        )
        is OnboardingInternalEvent.InspirationPhotoAdded -> current.copy(
            inspirationPhotos = listOf(event.photo) + current.inspirationPhotos,
            isAddingInspirationPhoto = false,
            inspirationPhotoErrorRes = null,
        )
        is OnboardingInternalEvent.InspirationPhotoSaveFailed -> current.copy(
            isAddingInspirationPhoto = false,
            inspirationPhotoErrorRes = event.messageRes,
        )
        is OnboardingInternalEvent.InspirationPhotoDeleted -> current.copy(
            inspirationPhotos = current.inspirationPhotos.filterNot { it.id == event.photoId && !it.isStock },
        )
        is OnboardingInternalEvent.InspirationPhotoEnabledChanged -> current.copy(
            inspirationPhotos = current.inspirationPhotos.map {
                if (it.id == event.photoId) it.copy(isEnabled = event.enabled) else it
            },
        )
        is OnboardingInternalEvent.AppChoiceToggled -> current.copy(
            appChoices = current.appChoices.map { choice ->
                if (choice.packageName == event.packageName) {
                    choice.copy(isSelected = !choice.isSelected)
                } else {
                    choice
                }
            },
        )
        else -> current
    }

    private fun reducePermissionsAndPledge(
        current: OnboardingUiState.Active,
        event: OnboardingInternalEvent,
    ): OnboardingUiState.Active = when (event) {
        is OnboardingInternalEvent.PermissionsUpdated -> current.copy(
            accessibilityGranted = event.accessibilityGranted,
            overlayGranted = event.overlayGranted,
        )
        OnboardingInternalEvent.PledgeHoldBegan -> current.copy(
            isHoldingPledge = true,
            releasedPledgeEarlyHint = false,
        )
        is OnboardingInternalEvent.PledgeHoldProgressed -> current.copy(
            pledgeHoldProgressFraction = event.progressFraction.coerceIn(0f, 1f),
        )
        OnboardingInternalEvent.PledgeHoldReleasedEarly -> current.copy(
            isHoldingPledge = false,
            pledgeHoldProgressFraction = 0f,
            releasedPledgeEarlyHint = true,
        )
        OnboardingInternalEvent.PledgeHoldCompleted -> current.copy(
            step = OnboardingStep.COMPLETE,
            isHoldingPledge = false,
            pledgeHoldProgressFraction = 1f,
            releasedPledgeEarlyHint = false,
        )
        else -> current
    }

    private fun createActiveState(event: OnboardingInternalEvent.Loaded): OnboardingUiState.Active {
        val hours = event.existingDailyHours.coerceIn(MIN_DAILY_HOURS, MAX_DAILY_HOURS)
        return OnboardingUiState.Active(
            step = OnboardingStep.WELCOME,
            simulatorStage = BrainDecayStage.THRIVING,
            dailyDoomscrollHours = hours,
            daysLostPerYear = computeDaysLostPerYear(hours),
            wakingYearsLostTenthsOver50Years = computeWakingYearsTenthsOver50Years(hours),
            availableGoals = DEFAULT_GOALS,
            selectedGoalIds = event.existingGoalIds,
            peakWindowOptions = DEFAULT_PEAK_WINDOWS,
            selectedPeakWindow = event.existingPeakWindow,
            personalMantra = event.existingMantra,
            inspirationPhotos = event.existingInspirationPhotos,
            isAddingInspirationPhoto = false,
            inspirationPhotoErrorRes = null,
            appChoices = event.appChoices,
            accessibilityGranted = event.accessibilityGranted,
            overlayGranted = event.overlayGranted,
            pledgeHoldProgressFraction = 0f,
            isHoldingPledge = false,
            releasedPledgeEarlyHint = false,
        )
    }

    fun computeDaysLostPerYear(dailyHours: Int): Int {
        val clamped = dailyHours.coerceIn(MIN_DAILY_HOURS, MAX_DAILY_HOURS)
        return ((clamped * DAYS_PER_YEAR) / HOURS_PER_DAY).roundToInt()
    }

    fun computeWakingYearsTenthsOver50Years(dailyHours: Int): Int {
        val clamped = dailyHours.coerceIn(MIN_DAILY_HOURS, MAX_DAILY_HOURS)
        val years = (clamped / WAKING_HOURS_PER_DAY) * PROJECTION_YEARS
        return (years * TENTHS_MULTIPLIER).roundToInt()
    }

    private fun nextStep(current: OnboardingStep): OnboardingStep = when (current) {
        OnboardingStep.WELCOME -> OnboardingStep.ROT_SIMULATOR
        OnboardingStep.ROT_SIMULATOR -> OnboardingStep.TIME_AUDIT
        OnboardingStep.TIME_AUDIT -> OnboardingStep.GOAL_PICKER
        OnboardingStep.GOAL_PICKER -> OnboardingStep.INSPIRATION_PHOTOS
        OnboardingStep.INSPIRATION_PHOTOS -> OnboardingStep.APP_SELECTION
        OnboardingStep.APP_SELECTION -> OnboardingStep.PERMISSIONS
        OnboardingStep.PERMISSIONS -> OnboardingStep.COMMITMENT_PLEDGE
        OnboardingStep.COMMITMENT_PLEDGE -> OnboardingStep.COMPLETE
        OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
    }

    private fun previousStep(current: OnboardingStep): OnboardingStep = when (current) {
        OnboardingStep.WELCOME -> OnboardingStep.WELCOME
        OnboardingStep.ROT_SIMULATOR -> OnboardingStep.WELCOME
        OnboardingStep.TIME_AUDIT -> OnboardingStep.ROT_SIMULATOR
        OnboardingStep.GOAL_PICKER -> OnboardingStep.TIME_AUDIT
        OnboardingStep.INSPIRATION_PHOTOS -> OnboardingStep.GOAL_PICKER
        OnboardingStep.APP_SELECTION -> OnboardingStep.INSPIRATION_PHOTOS
        OnboardingStep.PERMISSIONS -> OnboardingStep.APP_SELECTION
        OnboardingStep.COMMITMENT_PLEDGE -> OnboardingStep.PERMISSIONS
        OnboardingStep.COMPLETE -> OnboardingStep.COMMITMENT_PLEDGE
    }
}
