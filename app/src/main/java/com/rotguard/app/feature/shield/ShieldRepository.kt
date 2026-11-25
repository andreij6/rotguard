package com.rotguard.app.feature.shield

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.AppSnoozeDao
import com.rotguard.app.storage.AppSnoozeEntity
import com.rotguard.app.storage.BrainDecayStage
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InterceptEventDao
import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.ReminderCategory
import com.rotguard.app.storage.ReminderDefaults
import com.rotguard.app.storage.ThemeMode
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

interface ShieldDataSource {
    suspend fun loadShieldEvaluationContext(): ShieldEvaluationContext

    /** Stop pausing [packageName] until [untilEpochMillis] (the user can end it early from Home). */
    suspend fun snoozeApp(packageName: AppPackageName, untilEpochMillis: Long)
    suspend fun loadDemoBlockingDecision(
        packageName: AppPackageName,
        appDisplayName: String,
    ): ShieldDecision.ShowOverlay

    suspend fun recordInterceptOutcomeToDatabase(
        packageName: AppPackageName,
        appDisplayName: String,
        outcome: InterceptOutcome,
    )

    suspend fun recordBlockerRotationUsed(
        variationIndex: Int,
        inspirationIndex: Int,
        reminderId: Long = -1L,
    )
}

/**
 * Room-backed evaluation and intercept-logging repository for the live friction shield.
 *
 * **Responsibility & Boundaries**
 * - Loads current user profile preferences, guarded apps, rotated blocker prompts, Inspiration
 *   photo selection, randomly selected personal reminder, and today's intercept history for
 *   [ShieldReducer.evaluateForegroundTransition].
 * - Records [InterceptOutcome.RESISTED] and [InterceptOutcome.BYPASSED] events and rotation IDs.
 *
 * **Side Effects**
 * - Reads `user_profile`, `guarded_apps`, `inspiration_photos`, `personal_reminders`, and
 *   `intercept_events`, and writes rotation state and new rows to `intercept_events`.
 *
 * **Invariants**
 * - Always resolves today's event window from [ClockProvider.startOfTodayEpochMillis].
 * - Resisted intercepts credit `5` estimated minutes saved; bypassed intercepts credit `0`.
 *
 * **Anti-Patterns**
 * - Never display overlays, execute accessibility global actions, or play haptics from this class.
 */
@Singleton
class ShieldRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val guardedAppDao: GuardedAppDao,
    private val interceptEventDao: InterceptEventDao,
    private val inspirationPhotoDao: InspirationPhotoDao,
    private val personalReminderDao: PersonalReminderDao,
    private val clockProvider: ClockProvider,
    private val appSnoozeDao: AppSnoozeDao,
) : ShieldDataSource {

    override suspend fun snoozeApp(packageName: AppPackageName, untilEpochMillis: Long) {
        appSnoozeDao.upsertSnooze(AppSnoozeEntity(packageName.value, untilEpochMillis))
    }

    override suspend fun loadShieldEvaluationContext(): ShieldEvaluationContext {
        val nowEpoch = clockProvider.nowEpochMillis()
        InspirationDefaults.ensureStarterInspirationPhotosSeeded(
            userProfileDao = userProfileDao,
            inspirationPhotoDao = inspirationPhotoDao,
            nowEpochMillis = nowEpoch.value,
        )
        val profile = ReminderDefaults.ensureStarterRemindersSeeded(
            userProfileDao = userProfileDao,
            personalReminderDao = personalReminderDao,
            nowEpochMillis = nowEpoch.value,
        )
        val guardedApps = guardedAppDao.getAllAppsOnce().associateBy { it.packageName }
        val todayEvents = interceptEventDao.getEventsSinceOnce(clockProvider.startOfTodayEpochMillis().value)
        val attemptsByPkg = todayEvents.groupingBy { it.packageName }.eachCount()
        // ponytail: uses today's events only, so a repeat that straddles midnight isn't offered a break.
        val recentAttemptsByPkg = todayEvents
            .filter { it.timestampEpochMillis >= nowEpoch.value - ShieldReducer.GRACE_OFFER_WINDOW_MS }
            .groupingBy { it.packageName }
            .eachCount()
        appSnoozeDao.deleteExpired(nowEpoch.value)
        val snoozes = appSnoozeDao.getActiveSnoozesOnce(nowEpoch.value)
            .associate { it.packageName to it.untilEpochMillis }
        val todayResistedCount = todayEvents.count { it.outcomeName == InterceptOutcome.RESISTED.name }
        val brainHealth = ShieldReducer.computeBrainHealthFromEvents(todayEvents)

        val selectedPrompt = BlockerPromptCatalog.selectPromptVariation(
            varyPrompts = profile.varyBlockerPrompts,
            lastVariationIndex = profile.lastBlockerVariationIndex,
            seed = nowEpoch.value + todayEvents.size,
        )
        val chosenPhotoResult = resolveBlockerPhoto(profile)
        val chosenReminder = resolveRandomReminder(profile, nowEpoch.value, todayEvents.size)

        return ShieldEvaluationContext(
            shieldEnabled = profile.shieldEnabled,
            personalMantra = chosenReminder?.text ?: profile.personalMantra,
            globalHoldDurationMillis = HoldDurationMillis(profile.globalHoldDurationMillis),
            sessionGraceDurationMillis = HoldDurationMillis(profile.sessionGraceDurationMillis),
            guardedAppsByPackage = guardedApps,
            todayAttemptsByPackage = attemptsByPkg,
            brainHealth = brainHealth,
            nowEpochMillis = nowEpoch,
            themeMode = ThemeMode.fromStorageName(profile.themeModeName),
            selectedVariationIndex = selectedPrompt.index,
            openingPromptRes = selectedPrompt.templates.openingRes,
            secondThoughtPromptRes = selectedPrompt.templates.secondThoughtRes,
            selectedInspirationIndex = chosenPhotoResult.first,
            inspirationVisual = chosenPhotoResult.second,
            todayResistedCount = todayResistedCount,
            selectedReminderId = chosenReminder?.id ?: -1L,
            reminderCategoryBadgeRes = chosenReminder?.category?.badgeLabelRes,
            recentAttemptsByPackage = recentAttemptsByPkg,
            snoozedUntilByPackage = snoozes,
        )
    }

    override suspend fun loadDemoBlockingDecision(
        packageName: AppPackageName,
        appDisplayName: String,
    ): ShieldDecision.ShowOverlay {
        val context = loadShieldEvaluationContext()
        val configuredHold = context.guardedAppsByPackage[packageName.value]?.customHoldDurationMillis
            ?: context.globalHoldDurationMillis.value
        val attempts = (context.todayAttemptsByPackage[packageName.value] ?: 0) + 1
        recordBlockerRotationUsed(
            variationIndex = context.selectedVariationIndex,
            inspirationIndex = context.selectedInspirationIndex,
            reminderId = context.selectedReminderId,
        )
        return ShieldDecision.ShowOverlay(
            packageName = packageName,
            appDisplayName = appDisplayName,
            holdDurationMillis = HoldDurationMillis(configuredHold),
            brainHealth = context.brainHealth,
            brainStage = BrainDecayStage.fromHealthPercent(context.brainHealth),
            personalMantra = context.personalMantra,
            todayAttemptsCount = attempts,
            themeMode = context.themeMode,
            variationIndex = context.selectedVariationIndex,
            openingPromptRes = context.openingPromptRes,
            secondThoughtPromptRes = context.secondThoughtPromptRes,
            inspirationIndex = context.selectedInspirationIndex,
            inspirationVisual = context.inspirationVisual,
            todayResistedCount = context.todayResistedCount,
            selectedReminderId = context.selectedReminderId,
            reminderCategoryBadgeRes = context.reminderCategoryBadgeRes,
        )
    }

    override suspend fun recordInterceptOutcomeToDatabase(
        packageName: AppPackageName,
        appDisplayName: String,
        outcome: InterceptOutcome,
    ) {
        val minutesSaved = if (outcome == InterceptOutcome.RESISTED) MINUTES_SAVED_ON_RESIST else 0
        interceptEventDao.insertEvent(
            InterceptEventEntity(
                packageName = packageName.value,
                appDisplayName = appDisplayName,
                timestampEpochMillis = clockProvider.nowEpochMillis().value,
                outcomeName = outcome.name,
                estimatedMinutesSaved = minutesSaved,
            ),
        )
    }

    override suspend fun recordBlockerRotationUsed(
        variationIndex: Int,
        inspirationIndex: Int,
        reminderId: Long,
    ) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        val updatedPhotoIdx = if (inspirationIndex >= 0) {
            inspirationIndex
        } else {
            current.lastInspirationPhotoIndex
        }
        val updatedReminderId = if (reminderId > 0L) {
            reminderId
        } else {
            current.lastReminderIdShown
        }
        userProfileDao.upsertProfile(
            current.copy(
                id = 1,
                lastBlockerVariationIndex = variationIndex,
                lastInspirationPhotoIndex = updatedPhotoIdx,
                lastReminderIdShown = updatedReminderId,
            ),
        )
    }

    private suspend fun resolveBlockerPhoto(
        profile: UserProfileEntity,
    ): Pair<Int, BlockerInspirationVisual?> {
        val photos = if (profile.showInspirationPhotosOnBlocker) {
            inspirationPhotoDao.getEnabledPhotosOnce()
        } else {
            emptyList()
        }
        val selectedPhotoIdx = BlockerPromptCatalog.selectNextInspirationPhotoIndex(
            photoCount = photos.size,
            lastIndex = profile.lastInspirationPhotoIndex,
        )
        val visual = photos.getOrNull(selectedPhotoIdx)?.let { entity ->
            BlockerInspirationVisual(
                title = entity.title,
                caption = entity.caption,
                filePath = entity.filePath,
                bundledDrawableName = entity.bundledDrawableName,
            )
        }
        return selectedPhotoIdx to visual
    }

    private suspend fun resolveRandomReminder(
        profile: UserProfileEntity,
        nowEpochMs: Long,
        eventCount: Int,
    ): BlockerReminderOption? {
        val options = personalReminderDao.getAllRemindersOnce().map { entity ->
            BlockerReminderOption(
                id = entity.id,
                text = entity.text,
                category = ReminderCategory.fromStorageName(entity.categoryName),
            )
        }
        val seed = nowEpochMs xor (eventCount.toLong() * SEED_MULTIPLIER) xor
            (profile.lastReminderIdShown * ALT_SEED_MULTIPLIER)
        return BlockerPromptCatalog.selectRandomReminder(
            reminders = options,
            lastReminderId = profile.lastReminderIdShown,
            seed = seed,
        )
    }

    private companion object {
        const val MINUTES_SAVED_ON_RESIST = 5
        const val SEED_MULTIPLIER = 31L
        const val ALT_SEED_MULTIPLIER = 17L
    }
}
