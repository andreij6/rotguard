package com.rotguard.app.feature.inspiration

import com.rotguard.app.storage.ReminderCategory

/**
 * Pure deterministic reducer for Inspiration photos and rotating personal reminders.
 *
 * **Responsibility & Boundaries**
 * - Owns all state transitions across loading, inline photo captioning, photo add/remove,
 *   categorized reminder creation/deletion, and pause-screen preferences.
 *
 * **Side Effects**
 * - None. Must never read files, query Room DAOs, launch photo pickers, or trigger haptics.
 *
 * **Invariants**
 * - Reminder drafts and photo captions are bounded to 80 characters for pause-screen legibility.
 * - Loading/ready/error remain mutually exclusive sealed branches.
 *
 * **Anti-Patterns**
 * - Never copy image streams or mutate state flags directly inside [reduce].
 */
object InspirationReducer {

    private const val MAX_TEXT_CHARS = 80

    fun reduce(state: InspirationUiState, event: InspirationInternalEvent): InspirationUiState {
        return when (event) {
            InspirationInternalEvent.LoadStarted -> {
                if (state is InspirationUiState.Ready) state else InspirationUiState.Loading
            }
            is InspirationInternalEvent.LoadFailed -> InspirationUiState.Error(event.messageRes)
            is InspirationInternalEvent.Loaded -> InspirationUiState.Ready(
                photos = event.snapshot.photos,
                reminders = event.snapshot.reminders,
                selectedReminderCategory = ReminderCategory.PERSONAL_GOAL,
                newReminderDraft = "",
                reminderAddedHintVisible = false,
                showPhotosOnBlocker = event.snapshot.showPhotosOnBlocker,
                varyBlockerPrompts = event.snapshot.varyBlockerPrompts,
                pendingPickedPhotoUri = null,
                pendingPhotoCaptionDraft = "",
                isSavingPhoto = false,
                photoErrorRes = null,
            )
            else -> reduceReadyState(state, event)
        }
    }

    private fun reduceReadyState(
        state: InspirationUiState,
        event: InspirationInternalEvent,
    ): InspirationUiState {
        val ready = state as? InspirationUiState.Ready ?: return state
        return when (event) {
            is InspirationInternalEvent.PendingPhotoSelected,
            is InspirationInternalEvent.PendingCaptionUpdated,
            InspirationInternalEvent.PendingPhotoCleared,
            InspirationInternalEvent.PhotoSaveStarted,
            is InspirationInternalEvent.PhotoAdded,
            is InspirationInternalEvent.PhotoSaveFailed,
            is InspirationInternalEvent.PhotoRemoved,
            is InspirationInternalEvent.PhotoEnabledUpdated,
            -> reducePhotoEvents(ready, event)
            is InspirationInternalEvent.ReminderCategoryUpdated,
            is InspirationInternalEvent.NewReminderDraftUpdated,
            is InspirationInternalEvent.ReminderAdded,
            is InspirationInternalEvent.ReminderRemoved,
            -> reduceReminderEvents(ready, event)
            is InspirationInternalEvent.ShowPhotosUpdated -> ready.copy(showPhotosOnBlocker = event.enabled)
            is InspirationInternalEvent.VaryPromptsUpdated -> ready.copy(varyBlockerPrompts = event.enabled)
            InspirationInternalEvent.LoadStarted,
            is InspirationInternalEvent.Loaded,
            is InspirationInternalEvent.LoadFailed,
            -> ready
        }
    }

    private fun reducePhotoEvents(
        ready: InspirationUiState.Ready,
        event: InspirationInternalEvent,
    ): InspirationUiState.Ready = when (event) {
        is InspirationInternalEvent.PendingPhotoSelected -> ready.copy(
            pendingPickedPhotoUri = event.uriString,
            pendingPhotoCaptionDraft = "",
            photoErrorRes = null,
        )
        is InspirationInternalEvent.PendingCaptionUpdated -> ready.copy(
            pendingPhotoCaptionDraft = event.caption.take(MAX_TEXT_CHARS),
        )
        InspirationInternalEvent.PendingPhotoCleared -> ready.copy(
            pendingPickedPhotoUri = null,
            pendingPhotoCaptionDraft = "",
            isSavingPhoto = false,
        )
        InspirationInternalEvent.PhotoSaveStarted -> ready.copy(
            isSavingPhoto = true,
            photoErrorRes = null,
        )
        is InspirationInternalEvent.PhotoAdded -> ready.copy(
            photos = listOf(event.photo) + ready.photos,
            pendingPickedPhotoUri = null,
            pendingPhotoCaptionDraft = "",
            isSavingPhoto = false,
            photoErrorRes = null,
        )
        is InspirationInternalEvent.PhotoSaveFailed -> ready.copy(
            isSavingPhoto = false,
            photoErrorRes = event.messageRes,
        )
        is InspirationInternalEvent.PhotoRemoved -> ready.copy(
            photos = ready.photos.filterNot { it.id == event.photoId && !it.isStock },
            photoErrorRes = null,
        )
        is InspirationInternalEvent.PhotoEnabledUpdated -> ready.copy(
            photos = ready.photos.map { if (it.id == event.photoId) it.copy(isEnabled = event.enabled) else it },
        )
        else -> ready
    }

    private fun reduceReminderEvents(
        ready: InspirationUiState.Ready,
        event: InspirationInternalEvent,
    ): InspirationUiState.Ready = when (event) {
        is InspirationInternalEvent.ReminderCategoryUpdated -> ready.copy(
            selectedReminderCategory = event.category,
            reminderAddedHintVisible = false,
        )
        is InspirationInternalEvent.NewReminderDraftUpdated -> ready.copy(
            newReminderDraft = event.text.take(MAX_TEXT_CHARS),
            reminderAddedHintVisible = false,
        )
        is InspirationInternalEvent.ReminderAdded -> ready.copy(
            reminders = listOf(event.reminder) + ready.reminders,
            newReminderDraft = "",
            reminderAddedHintVisible = true,
        )
        is InspirationInternalEvent.ReminderRemoved -> ready.copy(
            reminders = ready.reminders.filterNot { it.id == event.reminderId },
            reminderAddedHintVisible = false,
        )
        else -> ready
    }
}
