package com.rotguard.app.feature.shield

import com.rotguard.app.storage.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockerPromptCatalogTest {

    @Test
    fun `catalog contains exactly 20 distinct prompt variations`() {
        assertEquals(20, BlockerPromptCatalog.VARIATIONS.size)
        val uniqueOpenings = BlockerPromptCatalog.VARIATIONS.map { it.openingRes }.toSet()
        assertEquals(20, uniqueOpenings.size)
    }

    @Test
    fun `selectPromptVariation rotates without repeating previous index`() {
        for (previousIndex in 0 until BlockerPromptCatalog.VARIATIONS.size) {
            val selected = BlockerPromptCatalog.selectPromptVariation(
                varyPrompts = true,
                lastVariationIndex = previousIndex,
                seed = previousIndex.toLong(),
            )
            assertNotEquals(previousIndex, selected.index)
        }
    }

    @Test
    fun `selectPromptVariation returns index 0 when varyPrompts disabled`() {
        val selected = BlockerPromptCatalog.selectPromptVariation(
            varyPrompts = false,
            lastVariationIndex = 7,
            seed = 999L,
        )
        assertEquals(0, selected.index)
    }

    @Test
    fun `selectNextInspirationPhotoIndex handles empty, single, and wrapping lists`() {
        assertEquals(-1, BlockerPromptCatalog.selectNextInspirationPhotoIndex(photoCount = 0, lastIndex = -1))
        assertEquals(0, BlockerPromptCatalog.selectNextInspirationPhotoIndex(photoCount = 1, lastIndex = 0))
        assertEquals(1, BlockerPromptCatalog.selectNextInspirationPhotoIndex(photoCount = 3, lastIndex = 0))
        assertEquals(0, BlockerPromptCatalog.selectNextInspirationPhotoIndex(photoCount = 3, lastIndex = 2))
    }

    @Test
    fun `selectRandomReminder handles empty, single, and avoids immediate repeats across seeds`() {
        assertNull(BlockerPromptCatalog.selectRandomReminder(emptyList(), -1L, 123L))

        val single = listOf(BlockerReminderOption(1L, "Create first", ReminderCategory.PERSONAL_GOAL))
        assertEquals(1L, BlockerPromptCatalog.selectRandomReminder(single, 1L, 99L)?.id)

        val options = listOf(
            BlockerReminderOption(10L, "Goal A", ReminderCategory.PERSONAL_GOAL),
            BlockerReminderOption(20L, "Inspiration B", ReminderCategory.INSPIRATION),
            BlockerReminderOption(30L, "Refocus C", ReminderCategory.REFOCUS_CUE),
        )
        val pickedIds = (1L..30L).map { seed ->
            val picked = BlockerPromptCatalog.selectRandomReminder(options, lastReminderId = 20L, seed = seed)!!
            assertNotEquals(20L, picked.id)
            picked.id
        }.toSet()
        assertEquals(setOf(10L, 30L), pickedIds)
    }
}
