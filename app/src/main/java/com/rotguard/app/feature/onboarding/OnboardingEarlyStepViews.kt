package com.rotguard.app.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalArchedMedallion
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.BotanicalIllustration
import com.rotguard.app.designsystem.SquareCheckbox
import com.rotguard.app.designsystem.SquareSegmentButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.StaggeredFlyIn
import com.rotguard.app.designsystem.TextEntryModal
import com.rotguard.app.designsystem.TypewriterText
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.animatedCount
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.designsystem.typewriterDurationMillis
import com.rotguard.app.storage.BrainDecayStage

private const val TENTHS_DIVISOR = 10f
private const val DEFAULT_DAILY_HOURS_CAP = 6
private const val STEP_STAGGER_GAP_MS = 320L
private const val SMALL_STEP_GAP_MS = 220L

@Composable
internal fun WelcomeStepContent(animationKey: Any) {
    val palette = VerdantTheme.colors
    val headline = stringResource(R.string.onboarding_welcome_headline)
    val subtitle = stringResource(R.string.onboarding_welcome_subtitle)
    val headlineMs = typewriterDurationMillis(headline)
    val subtitleDelay = headlineMs + STEP_STAGGER_GAP_MS
    val cardDelay = subtitleDelay + typewriterDurationMillis(subtitle) + STEP_STAGGER_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        StaggeredFlyIn(animationKey = animationKey, delayMillis = 40L, fromOffsetY = 26.dp) {
            BotanicalArchedMedallion(
                illustration = BotanicalIllustration.SPROUT,
                contentDescription = stringResource(R.string.onboarding_welcome_illustration_cd),
                size = 184.dp,
            )
        }
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.displaySmall,
            color = palette.ink,
            animationKey = animationKey,
            startDelayMillis = 100L,
            textAlign = TextAlign.Center,
        )
        TypewriterText(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.inkMuted,
            animationKey = animationKey,
            startDelayMillis = subtitleDelay,
            textAlign = TextAlign.Center,
        )
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardDelay, fromOffsetY = 26.dp) {
            BotanicalCard {
                HighlightBulletRow(R.string.onboarding_highlight_pause_title, R.string.onboarding_highlight_pause_body)
                HighlightBulletRow(R.string.onboarding_highlight_hold_title, R.string.onboarding_highlight_hold_body)
                HighlightBulletRow(
                    R.string.onboarding_highlight_private_title,
                    R.string.onboarding_highlight_private_body,
                )
            }
        }
    }
}

@Composable
internal fun RotSimulatorStepContent(
    animationKey: Any,
    stage: BrainDecayStage,
    onStageSelected: (BrainDecayStage) -> Unit,
) {
    val palette = VerdantTheme.colors
    val stageColor = stageAccentColor(stage)
    val headline = stringResource(R.string.onboarding_simulator_headline)
    val subtitle = stringResource(R.string.onboarding_simulator_subtitle)
    val subDelay = typewriterDurationMillis(headline) + SMALL_STEP_GAP_MS
    val visualDelay = subDelay + typewriterDurationMillis(subtitle) + SMALL_STEP_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
    ) {
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
            animationKey = animationKey,
        )
        TypewriterText(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
            animationKey = animationKey,
            startDelayMillis = subDelay,
        )
        StaggeredFlyIn(animationKey = animationKey, delayMillis = visualDelay) {
            RotStageInfoCard(stage = stage, stageColor = stageColor)
        }
        StaggeredFlyIn(animationKey = animationKey, delayMillis = visualDelay + STEP_STAGGER_GAP_MS) {
            RotStageSelectorRow(selectedStage = stage, stageColor = stageColor, onStageSelected = onStageSelected)
        }
        StaggeredFlyIn(animationKey = animationKey, delayMillis = visualDelay + (STEP_STAGGER_GAP_MS * 2)) {
            Text(
                text = stringResource(
                    R.string.onboarding_simulator_vibration,
                    stringResource(stage.hapticSignatureLabelRes),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = stageColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RotStageInfoCard(stage: BrainDecayStage, stageColor: Color) {
    val palette = VerdantTheme.colors
    BotanicalCard(warm = true, borderColor = stageColor.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareTagBadge(
                text = stringResource(stage.displayTitleRes).uppercase(),
                accentColor = stageColor,
                containerColor = stageColor.copy(alpha = 0.14f),
            )
            Text(
                text = stringResource(stage.reelRangeLabelRes),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
        }
        Text(
            text = stringResource(stage.neuroscienceFactRes),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
    }
}

@Composable
private fun RotStageSelectorRow(
    selectedStage: BrainDecayStage,
    stageColor: Color,
    onStageSelected: (BrainDecayStage) -> Unit,
) {
    val palette = VerdantTheme.colors
    val stageLabels = listOf("0", "50", "100", "150", "200+")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        BrainDecayStage.entries.forEachIndexed { index, entry ->
            SquareSegmentButton(
                label = stageLabels.getOrNull(index) ?: stringResource(entry.reelRangeLabelRes),
                selected = entry == selectedStage,
                selectedFill = stageColor,
                selectedInk = palette.mossBtnInk,
                onClick = { onStageSelected(entry) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun TimeAuditStepContent(
    animationKey: Any,
    dailyHours: Int,
    daysLostPerYear: Int,
    wakingYearsTenths: Int,
    onHoursSelected: (Int) -> Unit,
) {
    val palette = VerdantTheme.colors
    val wakingYears = wakingYearsTenths / TENTHS_DIVISOR
    val hourChoices = listOf(1, 2, 3, 4, DEFAULT_DAILY_HOURS_CAP)
    val headline = stringResource(R.string.onboarding_audit_headline)
    val cardDelay = typewriterDurationMillis(headline) + STEP_STAGGER_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
            animationKey = animationKey,
        )
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardDelay) {
            BotanicalCard(warm = true, borderColor = palette.warmBorder) {
                Text(
                    text = pluralStringResource(R.plurals.onboarding_audit_hours_a_day, dailyHours, dailyHours),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.terracotta,
                )
                Text(
                    text = stringResource(
                        R.string.onboarding_audit_waking_years,
                        animatedCount(wakingYearsTenths) / TENTHS_DIVISOR,
                    ),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                    color = palette.terracotta,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.onboarding_audit_days_lost,
                        daysLostPerYear,
                        daysLostPerYear,
                        wakingYears,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
        }
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardDelay + STEP_STAGGER_GAP_MS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                hourChoices.forEach { hours ->
                    SquareSegmentButton(
                        label = if (hours >= DEFAULT_DAILY_HOURS_CAP) {
                            stringResource(R.string.onboarding_audit_hours_chip_max, hours)
                        } else {
                            stringResource(R.string.onboarding_audit_hours_chip, hours)
                        },
                        selected = hours == dailyHours,
                        onClick = { onHoursSelected(hours) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardDelay + (STEP_STAGGER_GAP_MS * 2)) {
            Text(
                text = stringResource(R.string.onboarding_audit_footer),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalPickerStepContent(
    animationKey: Any,
    state: OnboardingUiState.Active,
    onAction: (OnboardingAction) -> Unit,
) {
    val palette = VerdantTheme.colors
    val headline = stringResource(R.string.onboarding_goals_headline)
    val subtitle = stringResource(R.string.onboarding_goals_subtitle)
    val subDelay = typewriterDurationMillis(headline) + SMALL_STEP_GAP_MS
    val firstItemDelay = subDelay + typewriterDurationMillis(subtitle) + SMALL_STEP_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
            animationKey = animationKey,
        )
        TypewriterText(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
            animationKey = animationKey,
            startDelayMillis = subDelay,
        )
        state.availableGoals.forEachIndexed { index, goal ->
            StaggeredFlyIn(
                animationKey = animationKey,
                delayMillis = firstItemDelay + (index * SMALL_STEP_GAP_MS),
            ) {
                GoalSelectionCard(
                    goal = goal,
                    selected = goal.id in state.selectedGoalIds,
                    onClick = { onAction(OnboardingAction.GoalToggled(goal.id)) },
                )
            }
        }
        val footerDelay = firstItemDelay + (state.availableGoals.size * SMALL_STEP_GAP_MS)
        StaggeredFlyIn(animationKey = animationKey, delayMillis = footerDelay) {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_goals_peak_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mossAccent,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    state.peakWindowOptions.forEach { window ->
                        SquareSegmentButton(
                            label = localizedStarterText(window),
                            selected = window == state.selectedPeakWindow,
                            onClick = { onAction(OnboardingAction.PeakWindowSelected(window)) },
                            modifier = Modifier.fillMaxWidth(fraction = 0.48f),
                        )
                    }
                }
                PersonalReminderEditor(
                    mantra = localizedStarterText(state.personalMantra),
                    onMantraChanged = { onAction(OnboardingAction.PersonalMantraChanged(it)) },
                )
            }
        }
    }
}

/**
 * The first personal reminder, shown as a tap-to-edit card. Typing happens in [TextEntryModal], which rides above
 * the keyboard; an inline field this low on the step was hidden behind it.
 */
@Composable
private fun PersonalReminderEditor(
    mantra: String,
    onMantraChanged: (String) -> Unit,
) {
    val palette = VerdantTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        Text(
            text = stringResource(R.string.onboarding_goals_mantra_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        BotanicalCard(onClick = { editing = true }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mantra.ifBlank { stringResource(R.string.onboarding_goals_mantra_empty_hint) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mantra.isBlank()) palette.inkMuted else palette.ink,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.onboarding_goals_mantra_label),
                    tint = palette.mossAccent,
                    modifier = Modifier.size(size = 20.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.onboarding_goals_mantra_support),
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkMuted,
        )
    }
    if (editing) {
        TextEntryModal(
            title = stringResource(R.string.onboarding_goals_mantra_label),
            value = mantra,
            onValueChange = onMantraChanged,
            doneLabel = stringResource(R.string.onboarding_goals_mantra_done),
            onDismiss = { editing = false },
            supportingText = stringResource(R.string.onboarding_goals_mantra_support),
        )
    }
}

@Composable
private fun GoalSelectionCard(
    goal: OnboardingGoalOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(
        warm = selected,
        borderColor = if (selected) palette.mossAccent else palette.cardBorder,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(goal.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(goal.subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
            SquareCheckbox(checked = selected, onCheckedChange = onClick)
        }
    }
}

@Composable
private fun HighlightBulletRow(@StringRes titleRes: Int, @StringRes subtitleRes: Int) {
    val palette = VerdantTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(size = 8.dp)
                .clip(RoundedCornerShape(size = 2.dp))
                .background(palette.mossAccent),
        )
        Column {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
            )
        }
    }
}

@Composable
private fun stageAccentColor(stage: BrainDecayStage): Color {
    val palette = VerdantTheme.colors
    return when (stage) {
        BrainDecayStage.THRIVING -> palette.mossAccent
        BrainDecayStage.ALERT, BrainDecayStage.STRAINED -> palette.sunGold
        BrainDecayStage.CRACKING, BrainDecayStage.ROTTED -> palette.terracotta
    }
}
