package com.rotguard.app.feature.shield

import androidx.annotation.StringRes
import com.rotguard.app.R
import com.rotguard.app.storage.ReminderCategory
import kotlin.math.abs
import kotlin.random.Random

data class BlockerReminderOption(
    val id: Long,
    val text: String,
    val category: ReminderCategory,
)

/** String resources for one prompt pair; `%1$s` in either string is the guarded app's name. */
data class BlockerPromptTemplate(
    @StringRes val openingRes: Int,
    @StringRes val secondThoughtRes: Int,
)

data class SelectedBlockerPrompt(
    val index: Int,
    val templates: BlockerPromptTemplate,
)

object BlockerPromptCatalog {

    @StringRes val DEFAULT_OPENING: Int = R.string.blocker_opening_1

    @StringRes val DEFAULT_SECOND_THOUGHT: Int = R.string.blocker_second_1

    val VARIATIONS: List<BlockerPromptTemplate> = listOf(
        BlockerPromptTemplate(R.string.blocker_opening_1, R.string.blocker_second_1),
        BlockerPromptTemplate(R.string.blocker_opening_2, R.string.blocker_second_2),
        BlockerPromptTemplate(R.string.blocker_opening_3, R.string.blocker_second_3),
        BlockerPromptTemplate(R.string.blocker_opening_4, R.string.blocker_second_4),
        BlockerPromptTemplate(R.string.blocker_opening_5, R.string.blocker_second_5),
        BlockerPromptTemplate(R.string.blocker_opening_6, R.string.blocker_second_6),
        BlockerPromptTemplate(R.string.blocker_opening_7, R.string.blocker_second_7),
        BlockerPromptTemplate(R.string.blocker_opening_8, R.string.blocker_second_8),
        BlockerPromptTemplate(R.string.blocker_opening_9, R.string.blocker_second_9),
        BlockerPromptTemplate(R.string.blocker_opening_10, R.string.blocker_second_10),
        BlockerPromptTemplate(R.string.blocker_opening_11, R.string.blocker_second_11),
        BlockerPromptTemplate(R.string.blocker_opening_12, R.string.blocker_second_12),
        BlockerPromptTemplate(R.string.blocker_opening_13, R.string.blocker_second_13),
        BlockerPromptTemplate(R.string.blocker_opening_14, R.string.blocker_second_14),
        BlockerPromptTemplate(R.string.blocker_opening_15, R.string.blocker_second_15),
        BlockerPromptTemplate(R.string.blocker_opening_16, R.string.blocker_second_16),
        BlockerPromptTemplate(R.string.blocker_opening_17, R.string.blocker_second_17),
        BlockerPromptTemplate(R.string.blocker_opening_18, R.string.blocker_second_18),
        BlockerPromptTemplate(R.string.blocker_opening_19, R.string.blocker_second_19),
        BlockerPromptTemplate(R.string.blocker_opening_20, R.string.blocker_second_20),
    )

    fun selectPromptVariation(
        varyPrompts: Boolean,
        lastVariationIndex: Int,
        seed: Long,
    ): SelectedBlockerPrompt {
        if (!varyPrompts || VARIATIONS.isEmpty()) {
            return SelectedBlockerPrompt(index = 0, templates = VARIATIONS.first())
        }
        val count = VARIATIONS.size
        val rawIndex = abs(seed % count.toLong()).toInt()
        val chosenIndex = if (rawIndex == lastVariationIndex) {
            (rawIndex + 1) % count
        } else {
            rawIndex
        }
        return SelectedBlockerPrompt(index = chosenIndex, templates = VARIATIONS[chosenIndex])
    }

    fun selectNextInspirationPhotoIndex(photoCount: Int, lastIndex: Int): Int {
        if (photoCount <= 0) return -1
        if (photoCount == 1) return 0
        val next = (lastIndex + 1).mod(photoCount)
        return if (next == lastIndex) (lastIndex + 1).mod(photoCount) else next
    }

    fun selectRandomReminder(
        reminders: List<BlockerReminderOption>,
        lastReminderId: Long,
        seed: Long,
    ): BlockerReminderOption? {
        if (reminders.isEmpty()) return null
        if (reminders.size == 1) return reminders.first()
        val distinctCandidates = reminders.filter { it.id != lastReminderId }.ifEmpty { reminders }
        val index = Random(seed).nextInt(distinctCandidates.size)
        return distinctCandidates[index]
    }
}
