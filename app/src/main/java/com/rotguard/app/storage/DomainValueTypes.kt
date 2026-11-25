package com.rotguard.app.storage

import androidx.annotation.StringRes
import com.rotguard.app.R

@JvmInline
value class AppPackageName(val value: String)

@JvmInline
value class TimestampEpochMillis(val value: Long)

@JvmInline
value class HoldDurationMillis(val value: Long)

@JvmInline
value class BrainHealthPercent(val value: Int)

@JvmInline
value class DailyScreenMinutes(val value: Int)

@JvmInline
value class InterceptEventId(val value: Long)

@JvmInline
value class InspirationPhotoId(val value: Long)

@JvmInline
value class PersonalReminderId(val value: Long)

@JvmInline
value class UserGoalId(val value: String)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromStorageName(name: String): ThemeMode {
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}

enum class ReminderCategory(
    @StringRes val displayNameRes: Int,
    @StringRes val badgeLabelRes: Int,
    @StringRes val inputPlaceholderRes: Int,
) {
    PERSONAL_GOAL(
        displayNameRes = R.string.reminder_category_personal_goal,
        badgeLabelRes = R.string.reminder_badge_personal_goal,
        inputPlaceholderRes = R.string.reminder_placeholder_personal_goal,
    ),
    INSPIRATION(
        displayNameRes = R.string.reminder_category_inspiration,
        badgeLabelRes = R.string.reminder_badge_inspiration,
        inputPlaceholderRes = R.string.reminder_placeholder_inspiration,
    ),
    REFOCUS_CUE(
        displayNameRes = R.string.reminder_category_refocus_cue,
        badgeLabelRes = R.string.reminder_badge_refocus_cue,
        inputPlaceholderRes = R.string.reminder_placeholder_refocus_cue,
    ),
    CUSTOM(
        displayNameRes = R.string.reminder_category_custom,
        badgeLabelRes = R.string.reminder_badge_custom,
        inputPlaceholderRes = R.string.reminder_placeholder_custom,
    ),
    ;

    companion object {
        fun fromStorageName(name: String): ReminderCategory {
            return entries.firstOrNull { it.name == name } ?: PERSONAL_GOAL
        }
    }
}

enum class BrainDecayStage(
    val levelIndex: Int,
    @StringRes val displayTitleRes: Int,
    @StringRes val reelRangeLabelRes: Int,
    @StringRes val neuroscienceFactRes: Int,
    @StringRes val hapticSignatureLabelRes: Int,
) {
    THRIVING(
        levelIndex = 0,
        displayTitleRes = R.string.stage_1_title,
        reelRangeLabelRes = R.string.stage_1_reels,
        neuroscienceFactRes = R.string.stage_1_fact,
        hapticSignatureLabelRes = R.string.stage_1_haptic,
    ),
    ALERT(
        levelIndex = 1,
        displayTitleRes = R.string.stage_2_title,
        reelRangeLabelRes = R.string.stage_2_reels,
        neuroscienceFactRes = R.string.stage_2_fact,
        hapticSignatureLabelRes = R.string.stage_2_haptic,
    ),
    STRAINED(
        levelIndex = 2,
        displayTitleRes = R.string.stage_3_title,
        reelRangeLabelRes = R.string.stage_3_reels,
        neuroscienceFactRes = R.string.stage_3_fact,
        hapticSignatureLabelRes = R.string.stage_3_haptic,
    ),
    CRACKING(
        levelIndex = 3,
        displayTitleRes = R.string.stage_4_title,
        reelRangeLabelRes = R.string.stage_4_reels,
        neuroscienceFactRes = R.string.stage_4_fact,
        hapticSignatureLabelRes = R.string.stage_4_haptic,
    ),
    ROTTED(
        levelIndex = 4,
        displayTitleRes = R.string.stage_5_title,
        reelRangeLabelRes = R.string.stage_5_reels,
        neuroscienceFactRes = R.string.stage_5_fact,
        hapticSignatureLabelRes = R.string.stage_5_haptic,
    ),
    ;

    companion object {
        private const val THRESHOLD_THRIVING = 80
        private const val THRESHOLD_ALERT = 60
        private const val THRESHOLD_STRAINED = 40
        private const val THRESHOLD_CRACKING = 20

        fun fromLevelIndex(index: Int): BrainDecayStage {
            return entries.firstOrNull { it.levelIndex == index } ?: THRIVING
        }

        fun fromHealthPercent(percent: BrainHealthPercent): BrainDecayStage = when {
            percent.value >= THRESHOLD_THRIVING -> THRIVING
            percent.value >= THRESHOLD_ALERT -> ALERT
            percent.value >= THRESHOLD_STRAINED -> STRAINED
            percent.value >= THRESHOLD_CRACKING -> CRACKING
            else -> ROTTED
        }
    }
}

enum class InterceptOutcome {
    RESISTED,
    BYPASSED,
}
