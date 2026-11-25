package com.rotguard.app.feature.onboarding

import androidx.annotation.StringRes
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.UserGoalId

enum class OnboardingStep(
    val stepNumber: Int,
    val totalVisibleSteps: Int,
) {
    WELCOME(stepNumber = 1, totalVisibleSteps = 8),
    ROT_SIMULATOR(stepNumber = 2, totalVisibleSteps = 8),
    TIME_AUDIT(stepNumber = 3, totalVisibleSteps = 8),
    GOAL_PICKER(stepNumber = 4, totalVisibleSteps = 8),
    INSPIRATION_PHOTOS(stepNumber = 5, totalVisibleSteps = 8),
    APP_SELECTION(stepNumber = 6, totalVisibleSteps = 8),
    PERMISSIONS(stepNumber = 7, totalVisibleSteps = 8),
    COMMITMENT_PLEDGE(stepNumber = 8, totalVisibleSteps = 8),
    COMPLETE(stepNumber = 8, totalVisibleSteps = 8),
}

data class OnboardingGoalOption(
    val id: UserGoalId,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    /** Stored English starter mantra (a [com.rotguard.app.storage.StarterTextCatalog] key). */
    val suggestedMantra: String,
)

data class OnboardingAppChoice(
    val packageName: AppPackageName,
    val displayName: String,
    val isRecommended: Boolean,
    val isSelected: Boolean,
)

data class OnboardingInspirationPreview(
    val id: InspirationPhotoId,
    val title: String,
    val caption: String,
    val filePath: String? = null,
    val bundledDrawableName: String? = null,
    val isEnabled: Boolean = true,
) {
    /** Stock scenes can be shown or hidden but never removed. */
    val isStock: Boolean get() = bundledDrawableName != null
}

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState

    data class Active(
        val step: OnboardingStep,
        val simulatorStage: BrainDecayStage,
        val dailyDoomscrollHours: Int,
        val daysLostPerYear: Int,
        val wakingYearsLostTenthsOver50Years: Int,
        val availableGoals: List<OnboardingGoalOption>,
        val selectedGoalIds: Set<UserGoalId>,
        val peakWindowOptions: List<String>,
        val selectedPeakWindow: String,
        val personalMantra: String,
        val inspirationPhotos: List<OnboardingInspirationPreview>,
        val isAddingInspirationPhoto: Boolean,
        @StringRes val inspirationPhotoErrorRes: Int?,
        val appChoices: List<OnboardingAppChoice>,
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
        val pledgeHoldProgressFraction: Float,
        val isHoldingPledge: Boolean,
        val releasedPledgeEarlyHint: Boolean,
    ) : OnboardingUiState

    data class Error(@StringRes val messageRes: Int) : OnboardingUiState
}

sealed interface OnboardingAction {
    data object NextStepTapped : OnboardingAction
    data object PreviousStepTapped : OnboardingAction
    data class SimulatorStageSelected(val stage: BrainDecayStage) : OnboardingAction
    data class DailyHoursSelected(val hours: Int) : OnboardingAction
    data class GoalToggled(val goalId: UserGoalId) : OnboardingAction
    data class PeakWindowSelected(val windowLabel: String) : OnboardingAction
    data class PersonalMantraChanged(val mantraText: String) : OnboardingAction
    data class InspirationPhotoPicked(val uriString: String) : OnboardingAction
    data class InspirationPhotoRemoved(val photoId: InspirationPhotoId) : OnboardingAction
    data class InspirationPhotoToggled(val photoId: InspirationPhotoId) : OnboardingAction
    data class GuardedAppToggled(val packageName: AppPackageName) : OnboardingAction
    data object OpenAccessibilityTapped : OnboardingAction
    data object OpenOverlayTapped : OnboardingAction
    data object PermissionsRefreshed : OnboardingAction
    data object PledgeHoldStarted : OnboardingAction
    data object PledgeHoldReleased : OnboardingAction
    data object FinishOnboardingTapped : OnboardingAction
    data object RetryTapped : OnboardingAction
}

sealed interface OnboardingEffect {
    data object OpenAccessibilitySettings : OnboardingEffect
    data object OpenOverlaySettings : OnboardingEffect
    data object NavigateToDashboard : OnboardingEffect
}

sealed interface OnboardingInternalEvent {
    data object LoadStarted : OnboardingInternalEvent

    data class Loaded(
        val appChoices: List<OnboardingAppChoice>,
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
        val existingDailyHours: Int,
        val existingGoalIds: Set<UserGoalId>,
        val existingPeakWindow: String,
        val existingMantra: String,
        val existingInspirationPhotos: List<OnboardingInspirationPreview> = emptyList(),
    ) : OnboardingInternalEvent

    data class LoadFailed(@StringRes val messageRes: Int) : OnboardingInternalEvent
    data object StepAdvanced : OnboardingInternalEvent
    data object StepRewound : OnboardingInternalEvent
    data class SimulatorStageUpdated(val stage: BrainDecayStage) : OnboardingInternalEvent
    data class DailyHoursUpdated(val hours: Int) : OnboardingInternalEvent
    data class GoalSelectionToggled(val goalId: UserGoalId) : OnboardingInternalEvent
    data class PeakWindowUpdated(val windowLabel: String) : OnboardingInternalEvent
    data class PersonalMantraUpdated(val mantraText: String) : OnboardingInternalEvent
    data object InspirationPhotoSaveStarted : OnboardingInternalEvent
    data class InspirationPhotoAdded(val photo: OnboardingInspirationPreview) : OnboardingInternalEvent
    data class InspirationPhotoSaveFailed(@StringRes val messageRes: Int) : OnboardingInternalEvent
    data class InspirationPhotoDeleted(val photoId: InspirationPhotoId) : OnboardingInternalEvent
    data class InspirationPhotoEnabledChanged(val photoId: InspirationPhotoId, val enabled: Boolean) :
        OnboardingInternalEvent
    data class AppChoiceToggled(val packageName: AppPackageName) : OnboardingInternalEvent
    data class PermissionsUpdated(
        val accessibilityGranted: Boolean,
        val overlayGranted: Boolean,
    ) : OnboardingInternalEvent
    data object PledgeHoldBegan : OnboardingInternalEvent
    data class PledgeHoldProgressed(val progressFraction: Float) : OnboardingInternalEvent
    data object PledgeHoldReleasedEarly : OnboardingInternalEvent
    data object PledgeHoldCompleted : OnboardingInternalEvent
}
