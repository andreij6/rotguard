package com.rotguard.app.feature.inspiration

import app.cash.turbine.test
import com.rotguard.app.R
import com.rotguard.app.storage.HapticConductor
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PersonalReminderId
import com.rotguard.app.storage.ReminderCategory
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InspirationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `adds photo, uses reminder hint with category, adds reminder, and emits preview effect`() = runTest {
        val dataSource = FakeInspirationDataSource()
        val viewModel = InspirationViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        viewModel.onAction(InspirationAction.PhotoPickerReturned("content://picked/photo"))
        viewModel.onAction(InspirationAction.PendingCaptionChanged("Weekend hike"))
        viewModel.onAction(InspirationAction.ConfirmAddPhotoTapped)
        advanceUntilIdle()

        viewModel.onAction(InspirationAction.ReminderCategorySelected(ReminderCategory.REFOCUS_CUE))
        viewModel.onAction(InspirationAction.ReminderHintTapped("Take 3 slow breaths."))
        viewModel.onAction(InspirationAction.AddReminderTapped)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as InspirationUiState.Ready
        assertEquals(1, ready.photos.size)
        assertEquals(1, ready.reminders.size)
        assertEquals(ReminderCategory.REFOCUS_CUE, ready.reminders.first().category)
        assertEquals("Take 3 slow breaths.", ready.reminders.first().text)
        assertEquals("Take 3 slow breaths." to ReminderCategory.REFOCUS_CUE, dataSource.savedReminders.first())

        viewModel.effects.test {
            viewModel.onAction(InspirationAction.PreviewBlockerTapped)
            assertEquals(InspirationEffect.LaunchBlockerPreview, awaitItem())
        }
    }

    @Test
    fun `load failure transitions to Error and retry recovers`() = runTest {
        val dataSource = FakeInspirationDataSource(shouldFail = true)
        val viewModel = InspirationViewModel(dataSource, NoOpHapticConductor())
        advanceUntilIdle()

        assertEquals(InspirationUiState.Error(R.string.inspiration_error_load), viewModel.uiState.value)

        dataSource.shouldFail = false
        viewModel.onAction(InspirationAction.RetryTapped)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is InspirationUiState.Ready)
    }

    private class FakeInspirationDataSource(var shouldFail: Boolean = false) : InspirationDataSource {
        val savedReminders = mutableListOf<Pair<String, ReminderCategory>>()
        var showPhotos = true
        var varyPrompts = true

        override suspend fun loadInspirationSnapshot(): InspirationSnapshot {
            if (shouldFail) throw IOException("Failed loading inspiration")
            return InspirationSnapshot(
                photos = emptyList(),
                reminders = emptyList(),
                showPhotosOnBlocker = showPhotos,
                varyBlockerPrompts = varyPrompts,
            )
        }

        override suspend fun addPersonalPhoto(uriString: String, caption: String): InspirationPhotoItem {
            return InspirationPhotoItem(
                id = InspirationPhotoId(10L),
                title = "My Inspiration",
                caption = caption.ifBlank { "Reminder" },
                filePath = "/local/10.jpg",
                bundledDrawableName = null,
            )
        }

        override suspend fun deletePhoto(photoId: InspirationPhotoId) = Unit

        override suspend fun addPersonalReminder(text: String, category: ReminderCategory): PersonalReminderItem {
            savedReminders += text to category
            return PersonalReminderItem(
                id = PersonalReminderId(savedReminders.size.toLong()),
                text = text,
                category = category,
            )
        }

        override suspend fun setPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean) = Unit

        override suspend fun deleteReminder(reminderId: PersonalReminderId) = Unit
        override suspend fun updateShowPhotosOnBlocker(enabled: Boolean) {
            showPhotos = enabled
        }
        override suspend fun updateVaryBlockerPrompts(enabled: Boolean) {
            varyPrompts = enabled
        }
    }

    private class NoOpHapticConductor : HapticConductor {
        override fun playSelectionTick() = Unit
        override fun playWarningPulse(intensity: Float) = Unit
        override fun playHoldProgressTick(progressFraction: Float) = Unit
        override fun playRotDecayShudder(stageIndex: Int) = Unit
        override fun playVictoryChord() = Unit
        override fun playReleaseSoftTick() = Unit
    }
}
