package com.rotguard.app.storage

import com.rotguard.app.R

object ReminderDefaults {

    /** Tap-to-fill suggestions; resolved in the UI so the inserted text is in the app language. */
    val HINT_SUGGESTIONS: Map<ReminderCategory, List<Int>> = mapOf(
        ReminderCategory.PERSONAL_GOAL to listOf(
            R.string.hint_protect_sleep,
            R.string.starter_mantra_create_first,
            R.string.hint_read_ten_pages,
            R.string.hint_finish_one_task,
        ),
        ReminderCategory.INSPIRATION to listOf(
            R.string.starter_reminder_fully_present,
            R.string.hint_small_choices,
            R.string.hint_future_self,
        ),
        ReminderCategory.REFOCUS_CUE to listOf(
            R.string.starter_reminder_unclench,
            R.string.hint_stand_up,
            R.string.hint_notice_urge,
            R.string.hint_return_to_task,
        ),
        ReminderCategory.CUSTOM to listOf(
            R.string.hint_feel_better,
            R.string.hint_on_purpose,
        ),
    )

    suspend fun ensureStarterRemindersSeeded(
        userProfileDao: UserProfileDao,
        personalReminderDao: PersonalReminderDao,
        nowEpochMillis: Long,
    ): UserProfileEntity {
        val profile = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        if (!profile.defaultRemindersSeeded && personalReminderDao.countRemindersOnce() == 0) {
            val primaryGoal = profile.personalMantra.trim()
                .ifBlank { StarterTextCatalog.MANTRA_OUTLAST_FEED }
            val starters = listOf(
                PersonalReminderEntity(
                    text = primaryGoal,
                    categoryName = ReminderCategory.PERSONAL_GOAL.name,
                    createdAtEpochMillis = nowEpochMillis,
                ),
                PersonalReminderEntity(
                    text = StarterTextCatalog.REMINDER_FULLY_PRESENT,
                    categoryName = ReminderCategory.INSPIRATION.name,
                    createdAtEpochMillis = nowEpochMillis,
                ),
                PersonalReminderEntity(
                    text = StarterTextCatalog.REMINDER_UNCLENCH,
                    categoryName = ReminderCategory.REFOCUS_CUE.name,
                    createdAtEpochMillis = nowEpochMillis,
                ),
            )
            personalReminderDao.insertReminders(starters)
            val updated = profile.copy(id = 1, defaultRemindersSeeded = true)
            userProfileDao.upsertProfile(updated)
            return updated
        }
        return profile
    }
}
