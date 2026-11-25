package com.rotguard.app.feature.shield

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.InspirationPhotoThumbnail
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.feature.shield.celebration.CELEBRATION_VARIANTS
import com.rotguard.app.feature.shield.celebration.CelebrationBurst

private const val STAT_STRIP_ALPHA = 0.68f
private const val ECHO_PHOTO_DP = 36
private const val RETURNING_BAR_WIDTH_DP = 170

@Composable
fun InlineCelebrationView(
    state: ShieldOverlayUiState.CelebratingExit,
    onFinishNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinishNow,
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))

        CelebrationBurst(variant = CELEBRATION_VARIANTS[state.celebrationVariantIndex.mod(CELEBRATION_VARIANTS.size)])

        InlineCelebrationSummary(state = state)

        InlineCelebrationFooter(progressFraction = state.celebrationProgressFraction)
    }
}

@Composable
private fun InlineCelebrationSummary(state: ShieldOverlayUiState.CelebratingExit) {
    val palette = VerdantTheme.colors
    val summaryText = pluralStringResource(
        R.plurals.shield_minutes_saved,
        state.estimatedMinutesSaved,
        state.estimatedMinutesSaved,
    ) + "  ·  " + pluralStringResource(
        R.plurals.shield_stopped_today,
        state.todayUrgesStoppedCount,
        state.todayUrgesStoppedCount,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.shield_nice_choice, state.appDisplayName),
            style = MaterialTheme.typography.displaySmall,
            color = palette.cream,
            textAlign = TextAlign.Center,
        )

        Surface(
            shape = RoundedCornerShape(size = 8.dp),
            color = palette.canopyCard.copy(alpha = STAT_STRIP_ALPHA),
            border = BorderStroke(width = 1.dp, color = palette.canopyStroke),
        ) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.sunGold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        if (state.inspirationVisual != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InspirationPhotoThumbnail(
                    filePath = state.inspirationVisual.filePath,
                    bundledDrawableName = state.inspirationVisual.bundledDrawableName,
                    contentDescription = localizedStarterText(state.inspirationVisual.title),
                    borderColor = palette.canopyStroke,
                    modifier = Modifier.size(size = ECHO_PHOTO_DP.dp),
                )
                Text(
                    text = stringResource(R.string.shield_back_to_what_matters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.canopyMuted,
                )
            }
        }
    }
}

@Composable
private fun InlineCelebrationFooter(progressFraction: Float) {
    val palette = VerdantTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.shield_returning_home),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.canopyMuted,
        )
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .width(width = RETURNING_BAR_WIDTH_DP.dp)
                .height(height = 4.dp)
                .clip(RoundedCornerShape(size = 2.dp)),
            color = palette.sunGold,
            trackColor = palette.canopyStroke,
        )
    }
}
