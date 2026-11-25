package com.rotguard.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.LocalRevealTimeline
import com.rotguard.app.designsystem.LocalTypingSound
import com.rotguard.app.designsystem.RevealTimeline
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.StaggeredFlyIn
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.rememberTypingSoundPlayer
import kotlinx.coroutines.delay

private const val PROGRESS_ANIM_MS = 700
private const val TRANSITION_MS = 520
private const val CTA_AFTER_CONTENT_MS = 400L
private const val CTA_SLOT_HEIGHT_DP = 56
private const val CONTENT_FADE_DP = 40

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            OnboardingUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is OnboardingUiState.Error -> OnboardingErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(OnboardingAction.RetryTapped) },
            )
            is OnboardingUiState.Active -> OnboardingActiveContent(
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun OnboardingActiveContent(
    state: OnboardingUiState.Active,
    onAction: (OnboardingAction) -> Unit,
) {
    val progressTarget = state.step.stepNumber.toFloat() / state.step.totalVisibleSteps.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = PROGRESS_ANIM_MS),
        label = "onboardingProgress",
    )

    val typingSound = rememberTypingSoundPlayer()
    val revealTimelines = remember { mutableMapOf<OnboardingStep, RevealTimeline>() }
    val revealTimelineFor = { step: OnboardingStep -> revealTimelines.getOrPut(step) { RevealTimeline() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        OnboardingHeaderBar(
            step = state.step,
            progress = animatedProgress,
            canGoBack = state.step != OnboardingStep.WELCOME && state.step != OnboardingStep.COMPLETE,
            onBackTapped = { onAction(OnboardingAction.PreviousStepTapped) },
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalTypingSound provides typingSound) {
                    OnboardingStepBody(state = state, onAction = onAction, revealTimelineFor = revealTimelineFor)
                }
                // Lets the last item scroll fully clear of the fade below.
                Spacer(modifier = Modifier.height(height = CONTENT_FADE_DP.dp))
            }
            // Content softly fades out above the bottom button instead of being cut off.
            val canvas = VerdantTheme.colors.canvas
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(height = CONTENT_FADE_DP.dp)
                    .background(Brush.verticalGradient(listOf(canvas.copy(alpha = 0f), canvas))),
            )
        }

        Spacer(modifier = Modifier.height(height = 14.dp))

        OnboardingBottomActionBar(
            step = state.step,
            revealTimeline = revealTimelineFor(state.step),
            onAction = onAction,
        )
    }
}

@Composable
private fun OnboardingStepBody(
    state: OnboardingUiState.Active,
    onAction: (OnboardingAction) -> Unit,
    revealTimelineFor: (OnboardingStep) -> RevealTimeline,
) {
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            fadeIn(tween(durationMillis = TRANSITION_MS))
                .togetherWith(fadeOut(tween(durationMillis = TRANSITION_MS)))
        },
        label = "onboardingStepContent",
    ) { currentStep ->
        CompositionLocalProvider(LocalRevealTimeline provides revealTimelineFor(currentStep)) {
            when (currentStep) {
                OnboardingStep.WELCOME -> WelcomeStepContent(animationKey = currentStep)
                OnboardingStep.ROT_SIMULATOR -> RotSimulatorStepContent(
                    animationKey = currentStep,
                    stage = state.simulatorStage,
                    onStageSelected = { onAction(OnboardingAction.SimulatorStageSelected(it)) },
                )
                OnboardingStep.TIME_AUDIT -> TimeAuditStepContent(
                    animationKey = currentStep,
                    dailyHours = state.dailyDoomscrollHours,
                    daysLostPerYear = state.daysLostPerYear,
                    wakingYearsTenths = state.wakingYearsLostTenthsOver50Years,
                    onHoursSelected = { onAction(OnboardingAction.DailyHoursSelected(it)) },
                )
                OnboardingStep.GOAL_PICKER -> GoalPickerStepContent(
                    animationKey = currentStep,
                    state = state,
                    onAction = onAction,
                )
                OnboardingStep.INSPIRATION_PHOTOS -> OnboardingInspirationStepContent(
                    animationKey = currentStep,
                    state = state,
                    onPhotoPicked = { onAction(OnboardingAction.InspirationPhotoPicked(it)) },
                    onRemovePhoto = { onAction(OnboardingAction.InspirationPhotoRemoved(it)) },
                    onTogglePhoto = { onAction(OnboardingAction.InspirationPhotoToggled(it)) },
                )
                OnboardingStep.APP_SELECTION -> AppSelectionStepContent(
                    animationKey = currentStep,
                    state = state,
                    onAppToggled = { onAction(OnboardingAction.GuardedAppToggled(it)) },
                )
                OnboardingStep.PERMISSIONS -> PermissionsStepContent(
                    animationKey = currentStep,
                    accessibilityGranted = state.accessibilityGranted,
                    overlayGranted = state.overlayGranted,
                    onOpenAccessibility = { onAction(OnboardingAction.OpenAccessibilityTapped) },
                    onOpenOverlay = { onAction(OnboardingAction.OpenOverlayTapped) },
                )
                OnboardingStep.COMMITMENT_PLEDGE -> CommitmentPledgeStepContent(
                    animationKey = currentStep,
                    holdProgressFraction = state.pledgeHoldProgressFraction,
                    isHolding = state.isHoldingPledge,
                    releasedEarlyHint = state.releasedPledgeEarlyHint,
                    onHoldStart = { onAction(OnboardingAction.PledgeHoldStarted) },
                    onHoldEnd = { onAction(OnboardingAction.PledgeHoldReleased) },
                )
                OnboardingStep.COMPLETE -> CompleteStepContent(
                    animationKey = currentStep,
                    guardedAppCount = state.appChoices.count { it.isSelected },
                    personalMantra = state.personalMantra,
                )
            }
        }
    }
}

@Composable
private fun OnboardingHeaderBar(
    step: OnboardingStep,
    progress: Float,
    canGoBack: Boolean,
    onBackTapped: () -> Unit,
) {
    val palette = VerdantTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareTagBadge(
                text = if (step == OnboardingStep.COMPLETE) {
                    stringResource(R.string.onboarding_badge_all_set)
                } else {
                    stringResource(R.string.onboarding_badge_step, step.stepNumber, step.totalVisibleSteps)
                },
                accentColor = palette.mossAccent,
                containerColor = palette.sageTint,
            )
            if (canGoBack) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(size = 6.dp))
                        .clickable(onClick = onBackTapped)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = palette.inkMuted,
                        modifier = Modifier.size(size = 18.dp),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_back),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.inkMuted,
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 5.dp)
                .clip(RoundedCornerShape(size = 3.dp)),
            color = palette.mossAccent,
            trackColor = palette.sageTint,
        )
    }
}

@Composable
private fun OnboardingBottomActionBar(
    step: OnboardingStep,
    revealTimeline: RevealTimeline,
    onAction: (OnboardingAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    val buttonLabelRes = when (step) {
        OnboardingStep.WELCOME -> R.string.onboarding_cta_get_started
        OnboardingStep.ROT_SIMULATOR -> R.string.onboarding_cta_see_time_cost
        OnboardingStep.TIME_AUDIT -> R.string.onboarding_cta_set_focus_plan
        OnboardingStep.GOAL_PICKER -> R.string.onboarding_cta_add_inspiration
        OnboardingStep.INSPIRATION_PHOTOS -> R.string.onboarding_cta_choose_apps
        OnboardingStep.APP_SELECTION -> R.string.onboarding_cta_set_up_pause
        OnboardingStep.PERMISSIONS -> R.string.onboarding_cta_continue_final
        OnboardingStep.COMMITMENT_PLEDGE -> null
        OnboardingStep.COMPLETE -> R.string.onboarding_cta_go_home
    }

    // Hidden (and not tappable) until every line and card on the step has finished appearing.
    var ctaReady by remember(step) { mutableStateOf(false) }
    LaunchedEffect(step) {
        withFrameNanos { } // let the step's first frame report its reveal times
        delay(revealTimeline.finishMillis + CTA_AFTER_CONTENT_MS)
        ctaReady = true
    }

    Box(modifier = Modifier.fillMaxWidth().heightIn(min = CTA_SLOT_HEIGHT_DP.dp)) {
        if (ctaReady) {
            OnboardingCta(step = step, buttonLabelRes = buttonLabelRes, onAction = onAction)
        }
    }
}

@Composable
private fun OnboardingCta(
    step: OnboardingStep,
    buttonLabelRes: Int?,
    onAction: (OnboardingAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    StaggeredFlyIn(
        animationKey = step,
        delayMillis = 0L,
        fromOffsetY = 18.dp,
    ) {
        if (buttonLabelRes == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_pledge_bottom_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
        } else {
            SquarePrimaryButton(
                text = stringResource(buttonLabelRes),
                onClick = {
                    if (step == OnboardingStep.COMPLETE) {
                        onAction(OnboardingAction.FinishOnboardingTapped)
                    } else {
                        onAction(OnboardingAction.NextStepTapped)
                    }
                },
                useMossAccent = step != OnboardingStep.ROT_SIMULATOR,
            )
        }
    }
}

@Composable
private fun OnboardingErrorView(
    message: String,
    onRetry: () -> Unit,
) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = palette.terracotta)
        Spacer(modifier = Modifier.height(height = 16.dp))
        SquarePrimaryButton(
            text = stringResource(R.string.onboarding_retry),
            onClick = onRetry,
            useMossAccent = true,
        )
    }
}
