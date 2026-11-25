package com.rotguard.app.storage

import androidx.annotation.StringRes
import com.rotguard.app.R

/**
 * Built-in English texts that end up stored in Room: the default mantra and goal mantras, the
 * starter reminders, the bundled inspiration photo titles/captions and the peak-window choice.
 *
 * The stored English value doubles as a stable key. As long as the user never edits it, the UI
 * shows it in the current app language via [resFor]; once edited it is the user's own text and
 * shown as-is. This keeps rows written by older (English-only) builds working with no migration,
 * and a starter text follows the user when they switch language later.
 */
object StarterTextCatalog {

    const val MANTRA_OUTLAST_FEED = "Build things that outlast the feed."
    const val MANTRA_PROTECT_REST = "Protect tonight's rest and tomorrow's energy."
    const val MANTRA_CREATE_FIRST = "Create before you consume."
    const val MANTRA_BE_PRESENT_HERE = "Be where your feet are right now."
    const val MANTRA_TEN_PAGES = "Ten pages beats fifty reels."
    const val REMINDER_FULLY_PRESENT = "Be fully present with the people in front of you."
    const val REMINDER_UNCLENCH = "Unclench your jaw, drop your shoulders, exhale."
    const val PEAK_LATE_NIGHT = "Late Night (9pm - 1am)"
    const val PERSONAL_PHOTO_TITLE = "My Inspiration"
    const val PERSONAL_PHOTO_CAPTION = "What matters more than scrolling"

    private val RES_BY_STORED_ENGLISH: Map<String, Int> = mapOf(
        MANTRA_OUTLAST_FEED to R.string.starter_mantra_outlast_feed,
        MANTRA_PROTECT_REST to R.string.starter_mantra_protect_rest,
        MANTRA_CREATE_FIRST to R.string.starter_mantra_create_first,
        MANTRA_BE_PRESENT_HERE to R.string.starter_mantra_be_present_here,
        MANTRA_TEN_PAGES to R.string.starter_mantra_ten_pages,
        REMINDER_FULLY_PRESENT to R.string.starter_reminder_fully_present,
        REMINDER_UNCLENCH to R.string.starter_reminder_unclench,
        "Protect tonight's sleep and tomorrow's energy." to R.string.hint_protect_sleep,
        "Read 10 pages before opening any feed." to R.string.hint_read_ten_pages,
        "Finish one meaningful task first." to R.string.hint_finish_one_task,
        "Small daily choices shape the year ahead." to R.string.hint_small_choices,
        "Do something your future self will thank you for." to R.string.hint_future_self,
        "Stand up, drink water, and look away from the screen." to R.string.hint_stand_up,
        "Notice the urge — you don't have to follow it." to R.string.hint_notice_urge,
        "Close this app and return to what you started." to R.string.hint_return_to_task,
        "What would feel better than scrolling right now?" to R.string.hint_feel_better,
        "Check in: did I open this on purpose?" to R.string.hint_on_purpose,
        "Morning Trail" to R.string.starter_photo_morning_trail_title,
        "Early sunlight before the noise" to R.string.starter_photo_morning_trail_caption,
        "Redwood Path" to R.string.starter_photo_redwood_title,
        "Slow hours outdoors" to R.string.starter_photo_redwood_caption,
        "Slow Evenings" to R.string.starter_photo_evenings_title,
        "Rest without the glowing feed" to R.string.starter_photo_evenings_caption,
        "Misty Lake" to R.string.starter_photo_misty_lake_title,
        "A quiet morning before the day starts" to R.string.starter_photo_misty_lake_caption,
        "Desert Dunes" to R.string.starter_photo_desert_dunes_title,
        "Space to think, nowhere to scroll" to R.string.starter_photo_desert_dunes_caption,
        "Starry Meadow" to R.string.starter_photo_starry_meadow_title,
        "Screens off, stars on" to R.string.starter_photo_starry_meadow_caption,
        "Rolling Hills" to R.string.starter_photo_rolling_hills_title,
        "A long walk instead of a long feed" to R.string.starter_photo_rolling_hills_caption,
        "Snow Peaks" to R.string.starter_photo_snow_peaks_title,
        "Climb something real" to R.string.starter_photo_snow_peaks_caption,
        "Lavender Field" to R.string.starter_photo_lavender_field_title,
        "Slow down and breathe it in" to R.string.starter_photo_lavender_field_caption,
        "Morning Wake-Up" to R.string.peak_morning,
        "Midday Slump" to R.string.peak_midday,
        "Between Classes / Meetings" to R.string.peak_between,
        "After Work / School" to R.string.peak_after_work,
        PEAK_LATE_NIGHT to R.string.peak_late_night,
        PERSONAL_PHOTO_TITLE to R.string.starter_photo_personal_title,
        PERSONAL_PHOTO_CAPTION to R.string.starter_photo_personal_caption,
    )

    /** The translated resource for a stored starter text, or null when the user wrote their own. */
    @StringRes
    fun resFor(storedText: String): Int? = RES_BY_STORED_ENGLISH[storedText.trim()]
}
