package com.rotguard.app.feature.shield

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalArchedMedallion
import com.rotguard.app.designsystem.BotanicalIllustration
import com.rotguard.app.designsystem.InspirationPhotoThumbnail
import com.rotguard.app.designsystem.SquareHoldReservoir
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import java.util.Locale
import kotlin.math.ceil

private const val PHOTO_WIDTH_DP = 172
private const val PHOTO_HEIGHT_DP = 178
private const val MINI_PHOTO_DP = 44
private const val RING_SIZE_DP = 176
private const val RING_OUTER_SCALE = 1.04f
private const val RING_INNER_SCALE = 0.96f
private const val BREATHE_RING_MS = 2200
private const val FULL_CIRCLE_DEGREES = 360f
private const val TOP_ANGLE_DEGREES = -90f
private const val MILLIS_PER_SECOND_FLOAT = 1000f

/** The continue button stays locked (padlock + countdown) this long so a reflex tap can't land on it. */
private const val HOLD_UNLOCK_DELAY_MS = 1_000L
private const val TENTHS_DIVISOR = 10
private const val OVERLAY_TRANSITION_MS = 280
private const val PROMPT_FADE_MS = 240
private const val SLIDE_OFFSET_DIVISOR = 6
private const val PROMPT_CARD_CORNER_DP = 14

@Composable
fun FrictionOverlayScreen(
    state: ShieldOverlayUiState,
    onAction: (ShieldAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is ShieldOverlayUiState.Hidden) return
    val palette = VerdantTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canopy)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                fadeIn(tween(durationMillis = OVERLAY_TRANSITION_MS))
                    .togetherWith(fadeOut(tween(durationMillis = OVERLAY_TRANSITION_MS)))
            },
            label = "blockerToCelebrationTransition",
        ) { targetState ->
            when (targetState) {
                ShieldOverlayUiState.Hidden -> Unit
                is ShieldOverlayUiState.Blocking -> BlockingContent(
                    state = targetState,
                    onAction = onAction,
                )
                is ShieldOverlayUiState.CelebratingExit -> InlineCelebrationView(
                    state = targetState,
                    onFinishNow = { onAction(ShieldAction.CelebrationFinishNowTapped) },
                )
            }
        }
    }
}

@Composable
private fun BlockingContent(
    state: ShieldOverlayUiState.Blocking,
    onAction: (ShieldAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    val totalSec = (state.holdDurationMillis.value / MILLIS_PER_SECOND_FLOAT).toInt().coerceAtLeast(1)
    val remainingSecTenths = (
        (1f - state.holdProgressFraction) * state.holdDurationMillis.value / MILLIS_PER_SECOND_FLOAT * TENTHS_DIVISOR
        ).let { tenths -> ceil(tenths.toDouble()).toInt().coerceAtLeast(0) }
    val remainingSecLabel = String.format(Locale.getDefault(), "%.1f", remainingSecTenths / TENTHS_DIVISOR.toFloat())
    val appName = state.appDisplayName.ifBlank { stringResource(R.string.blocker_this_app) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        BlockerTopStatusBar(
            appDisplayName = appName,
            todayAttemptsCount = state.todayAttemptsCount,
            idleSecondsLeft = state.idleSecondsLeft,
        )

        BlockerFocalStage(
            isHolding = state.isHolding,
            holdProgressFraction = state.holdProgressFraction,
            remainingTimeLabel = stringResource(R.string.shield_seconds_short, remainingSecLabel),
            inspirationVisual = state.inspirationVisual,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PromptTransitionCard(
                openingPrompt = stringResource(state.openingPromptRes, appName),
                secondThoughtPrompt = stringResource(state.secondThoughtPromptRes, appName),
                showSecondThought = state.hasTransitionedToSecondThought,
                personalMantra = localizedStarterText(state.personalMantra),
                reminderCategoryBadge = state.reminderCategoryBadgeRes?.let { stringResource(it) },
            )

            SquarePrimaryButton(
                text = stringResource(R.string.shield_close_app, appName),
                onClick = { onAction(ShieldAction.ProtectBrainExitTapped) },
                fillOverride = palette.cream,
                inkOverride = palette.canopy,
            )

            if (state.offerGracePeriod) {
                TakeABreakOption(
                    appName = appName,
                    selectedMinutes = state.graceMinutes,
                    onSelect = { onAction(ShieldAction.GraceMinutesSelected(it)) },
                )
            }

            SquareHoldReservoir(
                progressFraction = state.holdProgressFraction,
                isHolding = state.isHolding,
                idleLabel = pluralStringResource(R.plurals.shield_hold_to_continue, totalSec, totalSec),
                holdingLabel = stringResource(R.string.shield_keep_holding, remainingSecLabel),
                onHoldStart = { onAction(ShieldAction.HoldStarted) },
                onHoldEnd = { onAction(ShieldAction.HoldReleased) },
                onCanopy = true,
                lockMillis = HOLD_UNLOCK_DELAY_MS,
            )

            Text(
                text = if (state.releasedEarlyHint) {
                    stringResource(R.string.shield_released_early_hint)
                } else {
                    stringResource(R.string.shield_close_above_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.releasedEarlyHint) palette.sunGold else palette.canopyMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BlockerTopStatusBar(
    appDisplayName: String,
    todayAttemptsCount: Int,
    idleSecondsLeft: Int?,
) {
    val palette = VerdantTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .clip(CircleShape)
                    .background(palette.sunGold),
            )
            Text(
                text = stringResource(R.string.shield_pause_app, appDisplayName),
                style = MaterialTheme.typography.titleMedium,
                color = palette.cream,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.shield_open_count_today, todayAttemptsCount),
                style = MaterialTheme.typography.labelMedium,
                color = palette.canopyMuted,
            )
            IdleAutoCloseCountdown(secondsLeft = idleSecondsLeft)
        }
    }
}

/** "Closes in 7s" with a draining ring; appears for the last stretch before the idle auto-close. */
@Composable
private fun IdleAutoCloseCountdown(secondsLeft: Int?) {
    val palette = VerdantTheme.colors
    val totalSeconds = (FrictionSessionController.IDLE_AUTO_CLOSE_MS -
        FrictionSessionController.IDLE_COUNTDOWN_SHOWN_AFTER_MS) / MILLIS_PER_SECOND_FLOAT
    AnimatedVisibility(visible = secondsLeft != null, enter = fadeIn(), exit = fadeOut()) {
        val shown = secondsLeft ?: 0
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = palette.sunGold.copy(alpha = 0.16f),
            border = BorderStroke(width = 1.dp, color = palette.sunGold),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    progress = { shown / totalSeconds },
                    modifier = Modifier.size(size = 14.dp),
                    color = palette.sunGold,
                    trackColor = palette.sunGold.copy(alpha = 0.25f),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.shield_idle_closing_in, shown),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.sunGold,
                )
            }
        }
    }
}

@Composable
private fun BlockerFocalStage(
    isHolding: Boolean,
    holdProgressFraction: Float,
    remainingTimeLabel: String,
    inspirationVisual: BlockerInspirationVisual?,
) {
    val palette = VerdantTheme.colors
    AnimatedContent(
        targetState = isHolding,
        transitionSpec = {
            fadeIn(tween(durationMillis = PROMPT_FADE_MS))
                .togetherWith(fadeOut(tween(durationMillis = PROMPT_FADE_MS)))
        },
        label = "blockerVisualStage",
    ) { holding ->
        if (!holding) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
            ) {
                if (inspirationVisual != null) {
                    InspirationPhotoThumbnail(
                        filePath = inspirationVisual.filePath,
                        bundledDrawableName = inspirationVisual.bundledDrawableName,
                        contentDescription = localizedStarterText(inspirationVisual.title),
                        arched = true,
                        borderColor = palette.canopyStroke,
                        modifier = Modifier.size(width = PHOTO_WIDTH_DP.dp, height = PHOTO_HEIGHT_DP.dp),
                    )
                    Text(
                        text = localizedStarterText(inspirationVisual.caption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.canopyMuted,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    BotanicalArchedMedallion(
                        illustration = BotanicalIllustration.TERRARIUM,
                        contentDescription = stringResource(R.string.shield_pause_illustration),
                        size = PHOTO_WIDTH_DP.dp,
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 14.dp),
            ) {
                if (inspirationVisual != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    ) {
                        InspirationPhotoThumbnail(
                            filePath = inspirationVisual.filePath,
                            bundledDrawableName = inspirationVisual.bundledDrawableName,
                            contentDescription = localizedStarterText(inspirationVisual.title),
                            borderColor = palette.canopyStroke,
                            modifier = Modifier.size(size = MINI_PHOTO_DP.dp),
                        )
                        Text(
                            text = localizedStarterText(inspirationVisual.caption),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.canopyMuted,
                        )
                    }
                }
                BreathingHoldHaloRing(
                    progressFraction = holdProgressFraction,
                    remainingTimeLabel = remainingTimeLabel,
                )
            }
        }
    }
}

@Composable
private fun BreathingHoldHaloRing(
    progressFraction: Float,
    remainingTimeLabel: String,
) {
    val palette = VerdantTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "breathingHalo")
    val pulse by infiniteTransition.animateFloat(
        initialValue = RING_INNER_SCALE,
        targetValue = RING_OUTER_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATHE_RING_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingScale",
    )

    Box(
        modifier = Modifier
            .size(size = RING_SIZE_DP.dp)
            .scale(pulse),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 7.dp.toPx()
            drawCircle(color = palette.haloOuter)
            drawCircle(color = palette.haloMid, radius = size.minDimension * 0.40f)
            drawCircle(
                color = palette.canopyStroke,
                style = Stroke(width = strokePx),
            )
            drawArc(
                color = palette.sunGold,
                startAngle = TOP_ANGLE_DEGREES,
                sweepAngle = progressFraction * FULL_CIRCLE_DEGREES,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = remainingTimeLabel,
                style = MaterialTheme.typography.displaySmall,
                color = palette.cream,
            )
            Text(
                text = stringResource(R.string.shield_breathe_slowly),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.canopyMuted,
            )
        }
    }
}

@Composable
private fun PromptTransitionCard(
    openingPrompt: String,
    secondThoughtPrompt: String,
    showSecondThought: Boolean,
    personalMantra: String,
    reminderCategoryBadge: String?,
) {
    val palette = VerdantTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = PROMPT_CARD_CORNER_DP.dp),
        color = palette.canopyCard,
        border = BorderStroke(width = 1.dp, color = palette.canopyStroke),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = showSecondThought,
                transitionSpec = {
                    val enter = fadeIn(tween(durationMillis = PROMPT_FADE_MS)) +
                        slideInVertically(tween(durationMillis = PROMPT_FADE_MS)) { it / SLIDE_OFFSET_DIVISOR }
                    val exit = fadeOut(tween(durationMillis = PROMPT_FADE_MS)) +
                        slideOutVertically(tween(durationMillis = PROMPT_FADE_MS)) { -it / SLIDE_OFFSET_DIVISOR }
                    enter.togetherWith(exit)
                },
                label = "blockerPromptCrossfade",
            ) { useSecondThought ->
                Text(
                    text = if (useSecondThought) secondThoughtPrompt else openingPrompt,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.cream,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                if (!reminderCategoryBadge.isNullOrBlank()) {
                    Text(
                        text = reminderCategoryBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.canopyMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = stringResource(R.string.shield_quoted_mantra, personalMantra),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.sunGold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
