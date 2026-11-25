package com.rotguard.app.feature.onboarding

import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.InspirationPhotoThumbnail
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.StaggeredFlyIn
import com.rotguard.app.designsystem.TypewriterText
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.designsystem.typewriterDurationMillis
import com.rotguard.app.storage.InspirationPhotoId

private const val MAX_PHOTOS_PER_PICK = 10
private const val STEP_GAP_MS = 220L
private const val SMALL_THUMB_WIDTH_DP = 76
private const val SMALL_THUMB_HEIGHT_DP = 56
private const val MAX_PREVIEW_PHOTOS = 4
private const val SQUARE_CORNER_DP = 6

@Composable
internal fun OnboardingInspirationStepContent(
    animationKey: Any,
    state: OnboardingUiState.Active,
    onPhotoPicked: (String) -> Unit,
    onRemovePhoto: (InspirationPhotoId) -> Unit,
    onTogglePhoto: (InspirationPhotoId) -> Unit,
) {
    val palette = VerdantTheme.colors
    val photos = state.inspirationPhotos
    val isAddingPhoto = state.isAddingInspirationPhoto
    val errorMessageRes = state.inspirationPhotoErrorRes
    val view = LocalView.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PHOTOS_PER_PICK),
    ) { uris ->
        uris.forEach { onPhotoPicked(it.toString()) }
    }

    val headline = stringResource(R.string.onboarding_inspiration_headline)
    val subtitle = stringResource(R.string.onboarding_inspiration_subtitle)
    val subDelay = typewriterDurationMillis(headline) + STEP_GAP_MS
    val actionDelay = subDelay + typewriterDurationMillis(subtitle) + STEP_GAP_MS

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
        StaggeredFlyIn(animationKey = animationKey, delayMillis = actionDelay) {
            SquareOutlineButton(
                text = stringResource(
                    if (isAddingPhoto) R.string.onboarding_inspiration_adding else R.string.onboarding_inspiration_add,
                ),
                enabled = !isAddingPhoto,
                borderColor = palette.mossAccent,
                textColor = palette.mossAccent,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
        if (errorMessageRes != null) {
            Text(
                text = stringResource(errorMessageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.terracotta,
            )
        }
        photos.take(MAX_PREVIEW_PHOTOS).forEachIndexed { index, photo ->
            StaggeredFlyIn(
                animationKey = animationKey,
                delayMillis = actionDelay + ((index + 1) * STEP_GAP_MS),
            ) {
                OnboardingPhotoRow(
                    photo = photo,
                    onRemove = { onRemovePhoto(photo.id) },
                    onToggle = { onTogglePhoto(photo.id) },
                )
            }
        }
    }
}

@Composable
private fun OnboardingPhotoRow(
    photo: OnboardingInspirationPreview,
    onRemove: () -> Unit,
    onToggle: () -> Unit,
) {
    val palette = VerdantTheme.colors
    val title = localizedStarterText(photo.title)
    BotanicalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InspirationPhotoThumbnail(
                filePath = photo.filePath,
                bundledDrawableName = photo.bundledDrawableName,
                contentDescription = title,
                modifier = Modifier.size(width = SMALL_THUMB_WIDTH_DP.dp, height = SMALL_THUMB_HEIGHT_DP.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = localizedStarterText(photo.caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
            // Stock scenes toggle Showing/Hidden; only the user's own photos can be removed.
            val accent = when {
                !photo.isStock -> palette.terracotta
                photo.isEnabled -> palette.mossAccent
                else -> palette.inkMuted
            }
            Surface(
                shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
                color = if (photo.isStock) palette.sageTint else palette.terracottaSoft,
                border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.4f)),
                onClick = if (photo.isStock) onToggle else onRemove,
            ) {
                Text(
                    text = stringResource(
                        when {
                            !photo.isStock -> R.string.onboarding_inspiration_remove
                            photo.isEnabled -> R.string.photo_toggle_showing
                            else -> R.string.photo_toggle_hidden
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
