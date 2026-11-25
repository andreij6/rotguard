package com.rotguard.app.feature.inspiration

import com.rotguard.app.R
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PersonalReminderId
import com.rotguard.app.storage.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InspirationReducerTest {

    @Test
    fun `reduce manages inline photo staging, multiple categorized reminders, and toggles`() {
        var state: InspirationUiState = InspirationReducer.reduce(
            InspirationUiState.Loading,
            InspirationInternalEvent.Loaded(
                InspirationSnapshot(
                    photos = listOf(
                        InspirationPhotoItem(
                            id = InspirationPhotoId(1L),
                            title = "Morning Trail",
                            caption = "Early sunlight",
                            filePath = null,
                            bundledDrawableName = "photo_alpine_sunrise",
                        ),
                    ),
                    reminders = listOf(
                        PersonalReminderItem(
                            id = PersonalReminderId(10L),
                            text = "Protect focus.",
                            category = ReminderCategory.PERSONAL_GOAL,
                        ),
                    ),
                    showPhotosOnBlocker = true,
                    varyBlockerPrompts = true,
                ),
            ),
        )

        state = InspirationReducer.reduce(state, InspirationInternalEvent.PendingPhotoSelected("content://img/42"))
        val staged = state as InspirationUiState.Ready
        assertEquals("content://img/42", staged.pendingPickedPhotoUri)

        val longCaption = "A".repeat(120)
        state = InspirationReducer.reduce(state, InspirationInternalEvent.PendingCaptionUpdated(longCaption))
        assertEquals(80, (state as InspirationUiState.Ready).pendingPhotoCaptionDraft.length)

        val newPhoto = InspirationPhotoItem(
            id = InspirationPhotoId(99L),
            title = "My Inspiration",
            caption = "Family dinner",
            filePath = "/files/inspiration/photo_99.jpg",
            bundledDrawableName = null,
        )
        state = InspirationReducer.reduce(state, InspirationInternalEvent.PhotoAdded(newPhoto))
        val afterAdd = state as InspirationUiState.Ready
        assertEquals(2, afterAdd.photos.size)
        assertNull(afterAdd.pendingPickedPhotoUri)

        state = InspirationReducer.reduce(
            state,
            InspirationInternalEvent.ReminderCategoryUpdated(ReminderCategory.REFOCUS_CUE),
        )
        state = InspirationReducer.reduce(
            state,
            InspirationInternalEvent.NewReminderDraftUpdated("B".repeat(120)),
        )
        assertEquals(80, (state as InspirationUiState.Ready).newReminderDraft.length)
        assertEquals(ReminderCategory.REFOCUS_CUE, state.selectedReminderCategory)

        state = InspirationReducer.reduce(
            state,
            InspirationInternalEvent.ReminderAdded(
                PersonalReminderItem(
                    id = PersonalReminderId(20L),
                    text = "Put phone down and exhale.",
                    category = ReminderCategory.REFOCUS_CUE,
                ),
            ),
        )
        val afterReminderAdd = state as InspirationUiState.Ready
        assertEquals(2, afterReminderAdd.reminders.size)
        assertEquals("", afterReminderAdd.newReminderDraft)
        assertTrue(afterReminderAdd.reminderAddedHintVisible)

        state = InspirationReducer.reduce(state, InspirationInternalEvent.ReminderRemoved(PersonalReminderId(10L)))
        assertEquals(1, (state as InspirationUiState.Ready).reminders.size)

        state = InspirationReducer.reduce(state, InspirationInternalEvent.ShowPhotosUpdated(false))
        assertFalse((state as InspirationUiState.Ready).showPhotosOnBlocker)
        assertTrue(state.varyBlockerPrompts)
    }

    @Test
    fun `photo save failure exposes a string resource and a new attempt clears it`() {
        val loaded = InspirationReducer.reduce(
            InspirationUiState.Loading,
            InspirationInternalEvent.Loaded(
                InspirationSnapshot(
                    photos = emptyList(),
                    reminders = emptyList(),
                    showPhotosOnBlocker = true,
                    varyBlockerPrompts = true,
                ),
            ),
        )
        val failed = InspirationReducer.reduce(
            loaded,
            InspirationInternalEvent.PhotoSaveFailed(R.string.inspiration_error_photo_save),
        ) as InspirationUiState.Ready
        assertEquals(R.string.inspiration_error_photo_save, failed.photoErrorRes)
        assertFalse(failed.isSavingPhoto)

        val retrying = InspirationReducer.reduce(failed, InspirationInternalEvent.PhotoSaveStarted)
            as InspirationUiState.Ready
        assertNull(retrying.photoErrorRes)
    }
}
