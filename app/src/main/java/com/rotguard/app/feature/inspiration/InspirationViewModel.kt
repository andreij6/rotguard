package com.rotguard.app.feature.inspiration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.R
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PersonalReminderId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates Inspiration photos, categorized personal reminders, and pause-screen preview actions.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream and one-shot [effects] for navigating back or launching
 *   the pause-screen preview.
 *
 * **Side Effects**
 * - Reads/writes photos, personal reminders, and preferences via [InspirationDataSource] and
 *   emits haptic feedback via [HapticConductor].
 *
 * **Invariants**
 * - Always rethrows [CancellationException] inside coroutine catch blocks.
 * - Uses linear `suspend` calls for storage mutations.
 *
 * **Anti-Patterns**
 * - Never open system image files or launch `Intent`s directly inside the ViewModel.
 */
@HiltViewModel
class InspirationViewModel @Inject constructor(
    private val dataSource: InspirationDataSource,
    private val hapticConductor: HapticConductor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InspirationUiState>(InspirationUiState.Loading)
    val uiState: StateFlow<InspirationUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<InspirationEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<InspirationEffect> = _effects.asSharedFlow()

    init {
        loadInspiration()
    }

    fun onAction(action: InspirationAction) {
        when (action) {
            is InspirationAction.PhotoPickerReturned,
            is InspirationAction.PhotosPickerReturned,
            is InspirationAction.PhotoEnabledToggled,
            is InspirationAction.PendingCaptionChanged,
            InspirationAction.ConfirmAddPhotoTapped,
            InspirationAction.CancelAddPhotoTapped,
            is InspirationAction.DeletePhotoTapped,
            -> handlePhotoAction(action)
            is InspirationAction.ReminderCategorySelected,
            is InspirationAction.ReminderHintTapped,
            is InspirationAction.NewReminderDraftChanged,
            InspirationAction.AddReminderTapped,
            is InspirationAction.DeleteReminderTapped,
            -> handleReminderAction(action)
            InspirationAction.RefreshRequested,
            InspirationAction.RetryTapped,
            -> loadInspiration()
            InspirationAction.ShowPhotosOnBlockerToggled -> toggleShowPhotos()
            InspirationAction.VaryBlockerPromptsToggled -> toggleVaryPrompts()
            InspirationAction.PreviewBlockerTapped -> emitWithTick(InspirationEffect.LaunchBlockerPreview)
            InspirationAction.BackTapped -> emitWithTick(InspirationEffect.NavigateBack)
        }
    }

    private fun handlePhotoAction(action: InspirationAction) {
        when (action) {
            is InspirationAction.PhotoPickerReturned -> {
                hapticConductor.playSelectionTick()
                applyEvent(InspirationInternalEvent.PendingPhotoSelected(action.uriString))
            }
            is InspirationAction.PendingCaptionChanged -> {
                applyEvent(InspirationInternalEvent.PendingCaptionUpdated(action.caption))
            }
            InspirationAction.ConfirmAddPhotoTapped -> confirmAddPhoto()
            InspirationAction.CancelAddPhotoTapped -> {
                hapticConductor.playSelectionTick()
                applyEvent(InspirationInternalEvent.PendingPhotoCleared)
            }
            is InspirationAction.DeletePhotoTapped -> deletePhoto(action.photoId)
            is InspirationAction.PhotosPickerReturned -> when (action.uriStrings.size) {
                0 -> Unit
                1 -> handlePhotoAction(InspirationAction.PhotoPickerReturned(action.uriStrings.first()))
                else -> addPhotosDirectly(action.uriStrings)
            }
            is InspirationAction.PhotoEnabledToggled -> togglePhotoEnabled(action.photoId)
            else -> Unit
        }
    }

    private fun addPhotosDirectly(uriStrings: List<String>) {
        applyEvent(InspirationInternalEvent.PhotoSaveStarted)
        viewModelScope.launch {
            var failures = 0
            uriStrings.forEach { uri ->
                try {
                    applyEvent(InspirationInternalEvent.PhotoAdded(dataSource.addPersonalPhoto(uri, "")))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    failures++
                } catch (_: IllegalStateException) {
                    failures++
                }
            }
            if (failures > 0) {
                applyEvent(InspirationInternalEvent.PhotoSaveFailed(R.string.inspiration_error_photo_save))
            } else {
                hapticConductor.playVictoryChord()
            }
        }
    }

    private fun togglePhotoEnabled(photoId: InspirationPhotoId) {
        val ready = _uiState.value as? InspirationUiState.Ready ?: return
        val photo = ready.photos.firstOrNull { it.id == photoId } ?: return
        val enabled = !photo.isEnabled
        hapticConductor.playSelectionTick()
        applyEvent(InspirationInternalEvent.PhotoEnabledUpdated(photoId, enabled))
        viewModelScope.launch {
            try {
                dataSource.setPhotoEnabled(photoId, enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                loadInspiration()
            }
        }
    }

    private fun handleReminderAction(action: InspirationAction) {
        when (action) {
            is InspirationAction.ReminderCategorySelected -> {
                hapticConductor.playSelectionTick()
                applyEvent(InspirationInternalEvent.ReminderCategoryUpdated(action.category))
            }
            is InspirationAction.ReminderHintTapped -> {
                hapticConductor.playSelectionTick()
                applyEvent(InspirationInternalEvent.NewReminderDraftUpdated(action.hintText))
            }
            is InspirationAction.NewReminderDraftChanged -> {
                applyEvent(InspirationInternalEvent.NewReminderDraftUpdated(action.reminderText))
            }
            InspirationAction.AddReminderTapped -> addReminder()
            is InspirationAction.DeleteReminderTapped -> deleteReminder(action.reminderId)
            else -> Unit
        }
    }

    private fun loadInspiration() {
        applyEvent(InspirationInternalEvent.LoadStarted)
        viewModelScope.launch {
            try {
                val snapshot = dataSource.loadInspirationSnapshot()
                applyEvent(InspirationInternalEvent.Loaded(snapshot))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(InspirationInternalEvent.LoadFailed(R.string.inspiration_error_load))
            } catch (_: IllegalStateException) {
                applyEvent(InspirationInternalEvent.LoadFailed(R.string.inspiration_error_load))
            }
        }
    }

    private fun confirmAddPhoto() {
        val ready = _uiState.value as? InspirationUiState.Ready ?: return
        val uri = ready.pendingPickedPhotoUri ?: return
        if (ready.isSavingPhoto) return

        applyEvent(InspirationInternalEvent.PhotoSaveStarted)
        viewModelScope.launch {
            try {
                val saved = dataSource.addPersonalPhoto(uri, ready.pendingPhotoCaptionDraft)
                hapticConductor.playVictoryChord()
                applyEvent(InspirationInternalEvent.PhotoAdded(saved))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(InspirationInternalEvent.PhotoSaveFailed(R.string.inspiration_error_photo_save))
            } catch (_: IllegalStateException) {
                applyEvent(InspirationInternalEvent.PhotoSaveFailed(R.string.inspiration_error_photo_save))
            }
        }
    }

    private fun deletePhoto(photoId: InspirationPhotoId) {
        hapticConductor.playSelectionTick()
        applyEvent(InspirationInternalEvent.PhotoRemoved(photoId))
        viewModelScope.launch {
            try {
                dataSource.deletePhoto(photoId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                loadInspiration()
            }
        }
    }

    private fun addReminder() {
        val ready = _uiState.value as? InspirationUiState.Ready ?: return
        val trimmed = ready.newReminderDraft.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                val added = dataSource.addPersonalReminder(trimmed, ready.selectedReminderCategory)
                if (added != null) {
                    hapticConductor.playVictoryChord()
                    applyEvent(InspirationInternalEvent.ReminderAdded(added))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Keep typed draft intact on transient write failure.
            }
        }
    }

    private fun deleteReminder(reminderId: PersonalReminderId) {
        hapticConductor.playSelectionTick()
        applyEvent(InspirationInternalEvent.ReminderRemoved(reminderId))
        viewModelScope.launch {
            try {
                dataSource.deleteReminder(reminderId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                loadInspiration()
            }
        }
    }

    private fun toggleShowPhotos() {
        val ready = _uiState.value as? InspirationUiState.Ready ?: return
        val nextEnabled = !ready.showPhotosOnBlocker
        hapticConductor.playSelectionTick()
        applyEvent(InspirationInternalEvent.ShowPhotosUpdated(nextEnabled))
        viewModelScope.launch {
            try {
                dataSource.updateShowPhotosOnBlocker(nextEnabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(InspirationInternalEvent.ShowPhotosUpdated(ready.showPhotosOnBlocker))
            }
        }
    }

    private fun toggleVaryPrompts() {
        val ready = _uiState.value as? InspirationUiState.Ready ?: return
        val nextEnabled = !ready.varyBlockerPrompts
        hapticConductor.playSelectionTick()
        applyEvent(InspirationInternalEvent.VaryPromptsUpdated(nextEnabled))
        viewModelScope.launch {
            try {
                dataSource.updateVaryBlockerPrompts(nextEnabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                applyEvent(InspirationInternalEvent.VaryPromptsUpdated(ready.varyBlockerPrompts))
            }
        }
    }

    private fun applyEvent(event: InspirationInternalEvent) {
        _uiState.value = InspirationReducer.reduce(_uiState.value, event)
    }

    private fun emitWithTick(effect: InspirationEffect) {
        hapticConductor.playSelectionTick()
        _effects.tryEmit(effect)
    }

    private companion object {
        const val EFFECT_BUFFER_CAPACITY = 8
    }
}
