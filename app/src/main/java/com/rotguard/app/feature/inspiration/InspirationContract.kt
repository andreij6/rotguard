package com.rotguard.app.feature.inspiration

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PersonalReminderId
import com.rotguard.app.storage.ReminderCategory

@Immutable
data class InspirationPhotoItem(
    val id: InspirationPhotoId,
    val title: String,
    val caption: String,
    val filePath: String?,
    val bundledDrawableName: String?,
    val isEnabled: Boolean = true,
) {
    /** Bundled stock scenes can only be shown/hidden; the user's own photos can be deleted. */
    val isStock: Boolean get() = bundledDrawableName != null
}

@Immutable
data class PersonalReminderItem(
    val id: PersonalReminderId,
    val text: String,
    val category: ReminderCategory,
)

data class InspirationSnapshot(
    val photos: List<InspirationPhotoItem>,
    val reminders: List<PersonalReminderItem>,
    val showPhotosOnBlocker: Boolean,
    val varyBlockerPrompts: Boolean,
)

sealed interface InspirationUiState {
    data object Loading : InspirationUiState

    @Immutable
    data class Ready(
        val photos: List<InspirationPhotoItem>,
        val reminders: List<PersonalReminderItem>,
        val selectedReminderCategory: ReminderCategory,
        val newReminderDraft: String,
        val reminderAddedHintVisible: Boolean,
        val showPhotosOnBlocker: Boolean,
        val varyBlockerPrompts: Boolean,
        val pendingPickedPhotoUri: String?,
        val pendingPhotoCaptionDraft: String,
        val isSavingPhoto: Boolean,
        @StringRes val photoErrorRes: Int?,
    ) : InspirationUiState

    data class Error(@StringRes val messageRes: Int) : InspirationUiState
}

sealed interface InspirationAction {
    data object RefreshRequested : InspirationAction
    data class PhotoPickerReturned(val uriString: String) : InspirationAction
    /** One photo opens the caption composer; several are added straight away with the default caption. */
    data class PhotosPickerReturned(val uriStrings: List<String>) : InspirationAction
    data class PhotoEnabledToggled(val photoId: InspirationPhotoId) : InspirationAction
    data class PendingCaptionChanged(val caption: String) : InspirationAction
    data object ConfirmAddPhotoTapped : InspirationAction
    data object CancelAddPhotoTapped : InspirationAction
    data class DeletePhotoTapped(val photoId: InspirationPhotoId) : InspirationAction
    data class ReminderCategorySelected(val category: ReminderCategory) : InspirationAction
    data class ReminderHintTapped(val hintText: String) : InspirationAction
    data class NewReminderDraftChanged(val reminderText: String) : InspirationAction
    data object AddReminderTapped : InspirationAction
    data class DeleteReminderTapped(val reminderId: PersonalReminderId) : InspirationAction
    data object ShowPhotosOnBlockerToggled : InspirationAction
    data object VaryBlockerPromptsToggled : InspirationAction
    data object PreviewBlockerTapped : InspirationAction
    data object BackTapped : InspirationAction
    data object RetryTapped : InspirationAction
}

sealed interface InspirationEffect {
    data object NavigateBack : InspirationEffect
    data object LaunchBlockerPreview : InspirationEffect
}

sealed interface InspirationInternalEvent {
    data object LoadStarted : InspirationInternalEvent
    data class Loaded(val snapshot: InspirationSnapshot) : InspirationInternalEvent
    data class LoadFailed(@StringRes val messageRes: Int) : InspirationInternalEvent
    data class PendingPhotoSelected(val uriString: String) : InspirationInternalEvent
    data class PendingCaptionUpdated(val caption: String) : InspirationInternalEvent
    data object PendingPhotoCleared : InspirationInternalEvent
    data object PhotoSaveStarted : InspirationInternalEvent
    data class PhotoAdded(val photo: InspirationPhotoItem) : InspirationInternalEvent
    data class PhotoSaveFailed(@StringRes val messageRes: Int) : InspirationInternalEvent
    data class PhotoRemoved(val photoId: InspirationPhotoId) : InspirationInternalEvent
    data class PhotoEnabledUpdated(val photoId: InspirationPhotoId, val enabled: Boolean) : InspirationInternalEvent
    data class ReminderCategoryUpdated(val category: ReminderCategory) : InspirationInternalEvent
    data class NewReminderDraftUpdated(val text: String) : InspirationInternalEvent
    data class ReminderAdded(val reminder: PersonalReminderItem) : InspirationInternalEvent
    data class ReminderRemoved(val reminderId: PersonalReminderId) : InspirationInternalEvent
    data class ShowPhotosUpdated(val enabled: Boolean) : InspirationInternalEvent
    data class VaryPromptsUpdated(val enabled: Boolean) : InspirationInternalEvent
}
