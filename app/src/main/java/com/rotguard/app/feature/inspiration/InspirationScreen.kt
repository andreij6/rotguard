package com.rotguard.app.feature.inspiration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme

private const val SQUARE_RADIUS_DP = 6
private const val COMPOSER_THUMB_DP = 76
private const val MAX_PHOTOS_PER_PICK = 10

@Composable
fun InspirationScreen(
    state: InspirationUiState,
    onAction: (InspirationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PHOTOS_PER_PICK),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            onAction(InspirationAction.PhotosPickerReturned(selectedUris.map { it.toString() }))
        }
    }
    val launchPicker = {
        pickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    VerdantCanvasBackdrop(modifier = modifier) {
        when (state) {
            InspirationUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
            is InspirationUiState.Error -> InspirationErrorView(
                message = stringResource(state.messageRes),
                onRetry = { onAction(InspirationAction.RetryTapped) },
            )
            is InspirationUiState.Ready -> InspirationReadyContent(
                state = state,
                onAction = onAction,
                onAddPhotoClick = launchPicker,
            )
        }
    }
}

@Composable
private fun InspirationReadyContent(
    state: InspirationUiState.Ready,
    onAction: (InspirationAction) -> Unit,
    onAddPhotoClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(InspirationTab.REMINDERS) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        InspirationHeader(onBackClick = { onAction(InspirationAction.BackTapped) })
        InspirationTabBar(
            selected = selectedTab,
            reminderCount = state.reminders.size,
            photoCount = state.photos.size,
            onSelect = { selectedTab = it },
        )
        when (selectedTab) {
            InspirationTab.REMINDERS -> InspirationRemindersSection(state = state, onAction = onAction)
            InspirationTab.PHOTOS -> InspirationPhotosTab(state, onAction, onAddPhotoClick)
            InspirationTab.PAUSE_SCREEN -> {
                PauseOptionsCard(
                    showPhotos = state.showPhotosOnBlocker,
                    varyPrompts = state.varyBlockerPrompts,
                    onToggleShowPhotos = { onAction(InspirationAction.ShowPhotosOnBlockerToggled) },
                    onToggleVaryPrompts = { onAction(InspirationAction.VaryBlockerPromptsToggled) },
                )
                SquarePrimaryButton(
                    text = stringResource(R.string.inspiration_preview_pause),
                    onClick = { onAction(InspirationAction.PreviewBlockerTapped) },
                    useMossAccent = true,
                )
            }
        }
        Spacer(modifier = Modifier.height(height = 12.dp))
    }
}

@Composable
private fun InspirationPhotosTab(
    state: InspirationUiState.Ready,
    onAction: (InspirationAction) -> Unit,
    onAddPhotoClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    if (state.pendingPickedPhotoUri != null) {
        PendingPhotoComposerCard(
            photoUriString = state.pendingPickedPhotoUri,
            captionDraft = state.pendingPhotoCaptionDraft,
            isSaving = state.isSavingPhoto,
            onCaptionChanged = { onAction(InspirationAction.PendingCaptionChanged(it)) },
            onConfirmSave = { onAction(InspirationAction.ConfirmAddPhotoTapped) },
            onCancel = { onAction(InspirationAction.CancelAddPhotoTapped) },
        )
    }
    if (state.photoErrorRes != null) {
        Text(
            text = stringResource(state.photoErrorRes),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.terracotta,
        )
    }
    if (state.photos.isEmpty()) {
        Text(
            text = stringResource(R.string.inspiration_photos_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
    }
    InspirationPhotoGrid(
        photos = state.photos,
        onAddPhotoClick = onAddPhotoClick,
        onDeletePhoto = { onAction(InspirationAction.DeletePhotoTapped(it)) },
        onTogglePhoto = { onAction(InspirationAction.PhotoEnabledToggled(it)) },
    )
}

@Composable
private fun InspirationHeader(
    onBackClick: () -> Unit,
) {
    val palette = VerdantTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(size = SQUARE_RADIUS_DP.dp))
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = palette.mossAccent,
                    modifier = Modifier.size(size = 18.dp),
                )
                Text(
                    text = stringResource(R.string.inspiration_back),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.mossAccent,
                )
            }
        }
        Text(
            text = stringResource(R.string.inspiration_title),
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
        )
        Text(
            text = stringResource(R.string.inspiration_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
    }
}

@Composable
private fun PendingPhotoComposerCard(
    photoUriString: String,
    captionDraft: String,
    isSaving: Boolean,
    onCaptionChanged: (String) -> Unit,
    onConfirmSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(warm = true, borderColor = palette.mossAccent) {
        Text(
            text = stringResource(R.string.inspiration_caption_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = photoUriString.toUri(),
                contentDescription = stringResource(R.string.inspiration_selected_preview_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size = COMPOSER_THUMB_DP.dp)
                    .clip(RoundedCornerShape(size = SQUARE_RADIUS_DP.dp)),
            )
            OutlinedTextField(
                value = captionDraft,
                onValueChange = onCaptionChanged,
                placeholder = { Text(stringResource(R.string.inspiration_caption_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(size = SQUARE_RADIUS_DP.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.mossAccent,
                    unfocusedBorderColor = palette.cardBorder,
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        ) {
            SquareOutlineButton(
                text = stringResource(R.string.inspiration_cancel),
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            )
            SquarePrimaryButton(
                text = stringResource(if (isSaving) R.string.inspiration_saving else R.string.inspiration_save_photo),
                onClick = onConfirmSave,
                enabled = !isSaving,
                useMossAccent = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PauseOptionsCard(
    showPhotos: Boolean,
    varyPrompts: Boolean,
    onToggleShowPhotos: () -> Unit,
    onToggleVaryPrompts: () -> Unit,
) {
    val palette = VerdantTheme.colors
    BotanicalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.inspiration_show_photos_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(R.string.inspiration_show_photos_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
            Switch(
                checked = showPhotos,
                onCheckedChange = { onToggleShowPhotos() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.mossBtnInk,
                    checkedTrackColor = palette.mossAccent,
                ),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = palette.rule)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.inspiration_vary_prompts_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(R.string.inspiration_vary_prompts_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                )
            }
            Switch(
                checked = varyPrompts,
                onCheckedChange = { onToggleVaryPrompts() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.mossBtnInk,
                    checkedTrackColor = palette.mossAccent,
                ),
            )
        }
    }
}

@Composable
private fun InspirationErrorView(
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
            text = stringResource(R.string.inspiration_retry),
            onClick = onRetry,
            useMossAccent = true,
        )
    }
}
