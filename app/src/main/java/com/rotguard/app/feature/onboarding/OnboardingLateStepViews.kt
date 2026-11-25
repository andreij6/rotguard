package com.rotguard.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.AppChoiceTile
import com.rotguard.app.designsystem.BotanicalArchedMedallion
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.BotanicalIllustration
import com.rotguard.app.designsystem.SquareHoldReservoir
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.StaggeredFlyIn
import com.rotguard.app.designsystem.TypewriterText
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.designsystem.typewriterDurationMillis
import com.rotguard.app.storage.AppPackageName
import kotlin.math.roundToInt

private const val HUNDRED_PERCENT = 100
private const val MAX_VISIBLE_ONBOARDING_APPS = 12
private const val STEP_GAP_MS = 220L
private const val GRID_ITEM_GAP_MS = 110L
private const val SECTION_GAP_MS = 320L
private const val APP_GRID_COLUMNS = 2

@Composable
internal fun AppSelectionStepContent(
    animationKey: Any,
    state: OnboardingUiState.Active,
    onAppToggled: (AppPackageName) -> Unit,
) {
    val palette = VerdantTheme.colors
    val headline = stringResource(R.string.onboarding_apps_headline)
    val subtitle = stringResource(R.string.onboarding_apps_subtitle)
    val subDelay = typewriterDurationMillis(headline) + STEP_GAP_MS
    val listStartDelay = subDelay + typewriterDurationMillis(subtitle) + STEP_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
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
        val appRows = state.appChoices.take(MAX_VISIBLE_ONBOARDING_APPS).chunked(APP_GRID_COLUMNS)
        appRows.forEachIndexed { rowIndex, rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = 10.dp)) {
                rowApps.forEachIndexed { columnIndex, app ->
                    StaggeredFlyIn(
                        animationKey = animationKey,
                        delayMillis = listStartDelay + ((rowIndex * APP_GRID_COLUMNS + columnIndex) * GRID_ITEM_GAP_MS),
                        modifier = Modifier.weight(1f),
                    ) {
                        AppChoiceTile(
                            packageName = app.packageName,
                            displayName = app.displayName,
                            selected = app.isSelected,
                            statusText = stringResource(
                                if (app.isSelected) {
                                    R.string.onboarding_apps_will_pause
                                } else {
                                    R.string.onboarding_apps_opens_now
                                },
                            ),
                            onToggle = { onAppToggled(app.packageName) },
                            badgeText = stringResource(R.string.onboarding_apps_badge_suggested)
                                .takeIf { app.isRecommended },
                        )
                    }
                }
                repeat(APP_GRID_COLUMNS - rowApps.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun PermissionsStepContent(
    animationKey: Any,
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
) {
    val palette = VerdantTheme.colors
    val headline = stringResource(R.string.onboarding_permissions_headline)
    val subtitle = stringResource(R.string.onboarding_permissions_subtitle)
    val subDelay = typewriterDurationMillis(headline) + STEP_GAP_MS
    val cardOneDelay = subDelay + typewriterDurationMillis(subtitle) + SECTION_GAP_MS

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
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardOneDelay) {
            PermissionDisclosureCard(
                title = stringResource(R.string.onboarding_permissions_detect_title),
                description = stringResource(R.string.onboarding_permissions_detect_body),
                granted = accessibilityGranted,
                buttonLabel = stringResource(R.string.onboarding_permissions_detect_button),
                onClick = onOpenAccessibility,
            )
        }
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardOneDelay + SECTION_GAP_MS) {
            PermissionDisclosureCard(
                title = stringResource(R.string.onboarding_permissions_overlay_title),
                description = stringResource(R.string.onboarding_permissions_overlay_body),
                granted = overlayGranted,
                buttonLabel = stringResource(R.string.onboarding_permissions_overlay_button),
                onClick = onOpenOverlay,
            )
        }
    }
}

@Composable
private fun PermissionDisclosureCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(
        borderColor = if (granted) palette.mossAccent else palette.warmBorder,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            SquareTagBadge(
                text = if (granted) {
                    stringResource(R.string.onboarding_permissions_badge_active)
                } else {
                    stringResource(R.string.onboarding_permissions_badge_needed)
                },
                accentColor = if (granted) palette.mossAccent else palette.terracotta,
                containerColor = if (granted) palette.sageTint else palette.terracottaSoft,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
        if (!granted) {
            SquareOutlineButton(
                text = buttonLabel,
                onClick = onClick,
                borderColor = palette.mossAccent,
                textColor = palette.mossAccent,
            )
        }
    }
}

@Composable
internal fun CommitmentPledgeStepContent(
    animationKey: Any,
    holdProgressFraction: Float,
    isHolding: Boolean,
    releasedEarlyHint: Boolean,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val palette = VerdantTheme.colors
    val percent = (holdProgressFraction * HUNDRED_PERCENT).roundToInt()
    val headline = stringResource(R.string.onboarding_pledge_headline)
    val subtitle = stringResource(R.string.onboarding_pledge_subtitle)
    val subDelay = typewriterDurationMillis(headline) + STEP_GAP_MS
    val reservoirDelay = subDelay + typewriterDurationMillis(subtitle) + SECTION_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        StaggeredFlyIn(animationKey = animationKey, delayMillis = 40L) {
            BotanicalArchedMedallion(
                illustration = BotanicalIllustration.SPROUT,
                contentDescription = stringResource(R.string.onboarding_pledge_illustration_cd),
                size = 180.dp,
            )
        }
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
            animationKey = animationKey,
            startDelayMillis = 90L,
            textAlign = TextAlign.Center,
        )
        TypewriterText(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
            animationKey = animationKey,
            startDelayMillis = subDelay,
            textAlign = TextAlign.Center,
        )
        StaggeredFlyIn(animationKey = animationKey, delayMillis = reservoirDelay) {
            SquareHoldReservoir(
                progressFraction = holdProgressFraction,
                isHolding = isHolding,
                idleLabel = stringResource(R.string.onboarding_pledge_idle_label),
                holdingLabel = stringResource(R.string.onboarding_pledge_holding_label, percent),
                onHoldStart = onHoldStart,
                onHoldEnd = onHoldEnd,
                onCanopy = false,
            )
        }
        if (releasedEarlyHint) {
            Text(
                text = stringResource(R.string.onboarding_pledge_released_early),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.terracotta,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CompleteStepContent(
    animationKey: Any,
    guardedAppCount: Int,
    personalMantra: String,
) {
    val palette = VerdantTheme.colors
    val headline = stringResource(R.string.onboarding_complete_headline)
    val cardDelay = typewriterDurationMillis(headline) + SECTION_GAP_MS

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        StaggeredFlyIn(animationKey = animationKey, delayMillis = 40L) {
            BotanicalArchedMedallion(
                illustration = BotanicalIllustration.TERRARIUM,
                contentDescription = stringResource(R.string.onboarding_complete_illustration_cd),
                size = 192.dp,
            )
        }
        TypewriterText(
            text = headline,
            style = MaterialTheme.typography.displaySmall,
            color = palette.mossAccent,
            animationKey = animationKey,
            startDelayMillis = 100L,
            textAlign = TextAlign.Center,
        )
        StaggeredFlyIn(animationKey = animationKey, delayMillis = cardDelay) {
            BotanicalCard(warm = true, borderColor = palette.warmBorder) {
                Text(
                    text = stringResource(R.string.onboarding_complete_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mossAccent,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.onboarding_complete_guarded_apps,
                        guardedAppCount,
                        guardedAppCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(
                        R.string.onboarding_complete_mantra_quote,
                        localizedStarterText(personalMantra),
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.inkMuted,
                )
            }
        }
    }
}
