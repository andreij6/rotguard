package com.rotguard.app.feature.onboarding

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InspirationPhotoEntity
import com.rotguard.app.storage.InspirationPhotoFileStore
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.InstalledAppCatalog
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.PersonalReminderEntity
import com.rotguard.app.storage.ReminderCategory
import com.rotguard.app.storage.StarterTextCatalog
import com.rotguard.app.storage.SystemPermissionInspector
import com.rotguard.app.storage.UserGoalId
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

data class InitialOnboardingPayload(
    val appChoices: List<OnboardingAppChoice>,
    val permissions: PermissionStatusSnapshot,
    val existingDailyHours: Int,
    val existingGoalIds: Set<UserGoalId>,
    val existingPeakWindow: String,
    val existingMantra: String,
    val inspirationPhotos: List<OnboardingInspirationPreview>,
)

data class OnboardingCommitmentRequest(
    val dailyDoomscrollHours: Int,
    val selectedGoalIds: Set<UserGoalId>,
    val peakWindow: String,
    val personalMantra: String,
    val appChoices: List<OnboardingAppChoice>,
)

interface OnboardingDataSource {
    suspend fun loadInitialOnboardingData(): InitialOnboardingPayload
    suspend fun addOnboardingInspirationPhoto(uriString: String): OnboardingInspirationPreview
    suspend fun removeOnboardingInspirationPhoto(photoId: InspirationPhotoId)
    suspend fun setOnboardingInspirationPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean)
    suspend fun saveOnboardingCommitmentToDatabase(request: OnboardingCommitmentRequest)
    fun inspectCurrentPermissions(): PermissionStatusSnapshot
}

/**
 * Persistence and device-catalog gateway for the onboarding calibration flow.
 *
 * **Responsibility & Boundaries**
 * - Loads installed launchable apps, existing profile preferences, starter inspiration photos,
 *   and current system permissions.
 * - Commits the user's daily budget, focus goals, personal reminder, inspiration photos, and
 *   blocked apps to local storage.
 *
 * **Side Effects**
 * - Reads `PackageManager` launchable activities and reads/writes profile, blocked apps, and
 *   inspiration photos tables plus private image storage.
 *
 * **Invariants**
 * - At least one recommended distraction target (such as YouTube) is pre-selected on fresh setup when installed.
 * - Starter inspiration photos are seeded once if the user has not set any yet.
 *
 * **Anti-Patterns**
 * - Never trigger haptics or UI state transitions directly from this repository.
 */
@Singleton
class OnboardingRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val guardedAppDao: GuardedAppDao,
    private val installedAppCatalog: InstalledAppCatalog,
    private val permissionInspector: SystemPermissionInspector,
    private val inspirationPhotoDao: InspirationPhotoDao,
    private val personalReminderDao: PersonalReminderDao,
    private val photoFileStore: InspirationPhotoFileStore,
    private val clockProvider: ClockProvider,
) : OnboardingDataSource {

    override suspend fun loadInitialOnboardingData(): InitialOnboardingPayload {
        val existingProfile = InspirationDefaults.ensureStarterInspirationPhotosSeeded(
            userProfileDao = userProfileDao,
            inspirationPhotoDao = inspirationPhotoDao,
            nowEpochMillis = clockProvider.nowEpochMillis().value,
        )
        val existingGuarded = guardedAppDao.getAllAppsOnce().associateBy { it.packageName }
        val installed = installedAppCatalog.queryLaunchableApps()

        val choices = if (installed.isNotEmpty()) {
            installed.map { record ->
                val stored = existingGuarded[record.packageName.value]
                OnboardingAppChoice(
                    packageName = record.packageName,
                    displayName = record.displayName,
                    isRecommended = record.isRecommendedBrainrotTarget,
                    isSelected = stored?.isGuarded ?: record.isRecommendedBrainrotTarget,
                )
            }
        } else {
            defaultFallbackAppChoices(existingGuarded)
        }

        val parsedGoals = existingProfile.selectedGoalIdsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { UserGoalId(it) }
            .toSet()

        val photos = inspirationPhotoDao.getAllPhotosOnce().map { entity ->
            OnboardingInspirationPreview(
                id = InspirationPhotoId(entity.id),
                title = entity.title,
                caption = entity.caption,
                filePath = entity.filePath,
                bundledDrawableName = entity.bundledDrawableName,
                isEnabled = entity.isEnabled,
            )
        }

        return InitialOnboardingPayload(
            appChoices = choices,
            permissions = permissionInspector.inspectPermissions(),
            existingDailyHours = existingProfile.dailyDoomscrollHours,
            existingGoalIds = parsedGoals.ifEmpty { setOf(UserGoalId("deep_work"), UserGoalId("better_sleep")) },
            existingPeakWindow = existingProfile.peakRotWindow,
            existingMantra = existingProfile.personalMantra,
            inspirationPhotos = photos,
        )
    }

    override suspend fun addOnboardingInspirationPhoto(uriString: String): OnboardingInspirationPreview {
        val storedPath = photoFileStore.copyUriToPrivateStorage(uriString)
        val entity = InspirationPhotoEntity(
            title = PERSONAL_PHOTO_TITLE,
            caption = DEFAULT_PHOTO_CAPTION,
            filePath = storedPath,
            sortOrder = -1,
            createdAtEpochMillis = clockProvider.nowEpochMillis().value,
        )
        val rowId = inspirationPhotoDao.insertPhoto(entity)
        return OnboardingInspirationPreview(
            id = InspirationPhotoId(rowId),
            title = entity.title,
            caption = entity.caption,
            filePath = storedPath,
        )
    }

    override suspend fun removeOnboardingInspirationPhoto(photoId: InspirationPhotoId) {
        val existing = inspirationPhotoDao.getPhotoByIdOnce(photoId.value)
        if (existing?.bundledDrawableName != null) return // stock photos are hidden, never deleted
        inspirationPhotoDao.deletePhotoById(photoId.value)
        photoFileStore.deletePrivatePhotoFile(existing?.filePath)
    }

    override suspend fun setOnboardingInspirationPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean) {
        inspirationPhotoDao.setPhotoEnabled(photoId.value, enabled)
    }

    override suspend fun saveOnboardingCommitmentToDatabase(request: OnboardingCommitmentRequest) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        val normalizedMantra = request.personalMantra.trim().ifBlank {
            StarterTextCatalog.MANTRA_OUTLAST_FEED
        }
        val goalCsv = request.selectedGoalIds.joinToString(",") { it.value }
        val nowMs = clockProvider.nowEpochMillis().value
        userProfileDao.upsertProfile(
            current.copy(
                id = 1,
                onboardingCompleted = true,
                dailyDoomscrollHours = request.dailyDoomscrollHours,
                selectedGoalIdsCsv = goalCsv,
                peakRotWindow = request.peakWindow,
                personalMantra = normalizedMantra,
                shieldEnabled = true,
                defaultRemindersSeeded = true,
            ),
        )

        if (personalReminderDao.countRemindersOnce() == 0) {
            val goalSuggestions = OnboardingReducer.DEFAULT_GOALS
                .filter { it.id in request.selectedGoalIds }
                .map { it.suggestedMantra }
                .filter { it != normalizedMantra }
                .take(2)
            val remindersToSeed = buildList {
                add(
                    PersonalReminderEntity(
                        text = normalizedMantra,
                        categoryName = ReminderCategory.PERSONAL_GOAL.name,
                        createdAtEpochMillis = nowMs,
                    ),
                )
                goalSuggestions.forEach { mantra ->
                    add(
                        PersonalReminderEntity(
                            text = mantra,
                            categoryName = ReminderCategory.PERSONAL_GOAL.name,
                            createdAtEpochMillis = nowMs,
                        ),
                    )
                }
                add(
                    PersonalReminderEntity(
                        text = StarterTextCatalog.REMINDER_UNCLENCH,
                        categoryName = ReminderCategory.REFOCUS_CUE.name,
                        createdAtEpochMillis = nowMs,
                    ),
                )
            }
            personalReminderDao.insertReminders(remindersToSeed)
        }

        val existingMap = guardedAppDao.getAllAppsOnce().associateBy { it.packageName }
        val entities = request.appChoices.map { choice ->
            val prior = existingMap[choice.packageName.value]
            GuardedAppEntity(
                packageName = choice.packageName.value,
                displayName = choice.displayName,
                isGuarded = choice.isSelected,
                customHoldDurationMillis = prior?.customHoldDurationMillis,
                dailyLimitCount = prior?.dailyLimitCount ?: DEFAULT_DAILY_APP_LIMIT,
            )
        }
        if (entities.isNotEmpty()) {
            guardedAppDao.upsertApps(entities)
        }
    }

    override fun inspectCurrentPermissions(): PermissionStatusSnapshot {
        return permissionInspector.inspectPermissions()
    }

    private fun defaultFallbackAppChoices(
        existingGuarded: Map<String, GuardedAppEntity>,
    ): List<OnboardingAppChoice> {
        val presets = listOf(
            "com.google.android.youtube" to "YouTube",
            "com.instagram.android" to "Instagram",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.reddit.frontpage" to "Reddit",
        )
        return presets.map { (pkg, name) ->
            OnboardingAppChoice(
                packageName = AppPackageName(pkg),
                displayName = name,
                isRecommended = true,
                isSelected = existingGuarded[pkg]?.isGuarded ?: true,
            )
        }
    }

    private companion object {
        const val DEFAULT_DAILY_APP_LIMIT = 5
        const val PERSONAL_PHOTO_TITLE = StarterTextCatalog.PERSONAL_PHOTO_TITLE
        const val DEFAULT_PHOTO_CAPTION = StarterTextCatalog.PERSONAL_PHOTO_CAPTION
    }
}
