package com.rotguard.app.storage

object InspirationDefaults {

    const val DRAWABLE_ALPINE_SUNRISE = "photo_alpine_sunrise"
    const val DRAWABLE_CEDAR_GROVE = "photo_cedar_grove"
    const val DRAWABLE_COASTAL_HORIZON = "photo_coastal_horizon"
    const val DRAWABLE_MISTY_LAKE = "photo_misty_lake"
    const val DRAWABLE_DESERT_DUNES = "photo_desert_dunes"
    const val DRAWABLE_STARRY_MEADOW = "photo_starry_meadow"
    const val DRAWABLE_ROLLING_HILLS = "photo_rolling_hills"
    const val DRAWABLE_SNOW_PEAKS = "photo_snow_peaks"
    const val DRAWABLE_LAVENDER_FIELD = "photo_lavender_field"

    private const val SORT_SECOND = 2

    fun bundledStarterInspirationPhotos(nowEpochMillis: Long): List<InspirationPhotoEntity> = listOf(
        InspirationPhotoEntity(
            title = "Morning Trail",
            caption = "Early sunlight before the noise",
            bundledDrawableName = DRAWABLE_ALPINE_SUNRISE,
            sortOrder = 0,
            createdAtEpochMillis = nowEpochMillis,
        ),
        InspirationPhotoEntity(
            title = "Redwood Path",
            caption = "Slow hours outdoors",
            bundledDrawableName = DRAWABLE_CEDAR_GROVE,
            sortOrder = 1,
            createdAtEpochMillis = nowEpochMillis,
        ),
        InspirationPhotoEntity(
            title = "Slow Evenings",
            caption = "Rest without the glowing feed",
            bundledDrawableName = DRAWABLE_COASTAL_HORIZON,
            sortOrder = SORT_SECOND,
            createdAtEpochMillis = nowEpochMillis,
        ),
    ) + listOf(
        Triple(DRAWABLE_MISTY_LAKE, "Misty Lake", "A quiet morning before the day starts"),
        Triple(DRAWABLE_DESERT_DUNES, "Desert Dunes", "Space to think, nowhere to scroll"),
        Triple(DRAWABLE_STARRY_MEADOW, "Starry Meadow", "Screens off, stars on"),
        Triple(DRAWABLE_ROLLING_HILLS, "Rolling Hills", "A long walk instead of a long feed"),
        Triple(DRAWABLE_SNOW_PEAKS, "Snow Peaks", "Climb something real"),
        Triple(DRAWABLE_LAVENDER_FIELD, "Lavender Field", "Slow down and breathe it in"),
    ).mapIndexed { index, (drawable, title, caption) ->
        InspirationPhotoEntity(
            title = title,
            caption = caption,
            bundledDrawableName = drawable,
            sortOrder = SORT_SECOND + 1 + index,
            createdAtEpochMillis = nowEpochMillis,
        )
    }

    suspend fun ensureStarterInspirationPhotosSeeded(
        userProfileDao: UserProfileDao,
        inspirationPhotoDao: InspirationPhotoDao,
        nowEpochMillis: Long,
    ): UserProfileEntity {
        val profile = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        // Add any stock photo this install doesn't have yet (first run, or new ones shipped in an update).
        val present = inspirationPhotoDao.getAllPhotosOnce().mapNotNull { it.bundledDrawableName }.toSet()
        val missing = bundledStarterInspirationPhotos(nowEpochMillis).filter { it.bundledDrawableName !in present }
        if (missing.isNotEmpty()) inspirationPhotoDao.insertPhotos(missing)
        if (!profile.defaultInspirationSeeded) {
            val updated = profile.copy(id = 1, defaultInspirationSeeded = true)
            userProfileDao.upsertProfile(updated)
            return updated
        }
        return profile
    }
}
