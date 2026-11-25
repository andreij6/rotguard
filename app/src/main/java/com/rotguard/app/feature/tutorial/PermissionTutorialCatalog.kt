package com.rotguard.app.feature.tutorial

import android.os.Build

/** Special-access permissions RotGuard walks the user through in system Settings. */
enum class GuidedPermission { ACCESSIBILITY, OVERLAY, USAGE_ACCESS }

/** Narrated clips in res/raw, rendered by tools/tutorials/render.mjs (one per Settings layout). */
enum class TutorialVideo {
    ACCESSIBILITY,
    ACCESSIBILITY_LEGACY,
    ACCESSIBILITY_RESTRICTED,
    ACCESSIBILITY_UNLOCK,
    OVERLAY,
    OVERLAY_LEGACY,
    USAGE_ACCESS,
    USAGE_ACCESS_LEGACY,
}

/** Which Settings page the tutorial opens alongside the PiP video. */
enum class SettingsDestination { ACCESSIBILITY, OVERLAY, USAGE_ACCESS, APP_DETAILS }

/**
 * Progress through Android 13+'s "Restricted setting" lock, which blocks the accessibility switch for apps
 * installed from a file. The ⋮ > "Allow restricted settings" item only appears after the user has hit the lock once.
 */
enum class RestrictedSettingStage { NOT_SEEN, LOCK_SHOWN, UNLOCK_SHOWN }

data class PermissionTutorialPlan(
    val video: TutorialVideo,
    val destination: SettingsDestination,
    val nextRestrictedStage: RestrictedSettingStage,
)

/**
 * Picks the tutorial clip and Settings page for a permission on this device.
 *
 * - Accessibility: Android 8–10 vs 11+ ("Downloaded services" + OK vs "Downloaded apps" + Allow). On 13+ for
 *   sideloaded installs: first the lock walkthrough, then the App info unlock, then the normal clip.
 * - Overlay: Android 8–10 open RotGuard's own page; 11+ ignore the package and open the full app list.
 * - Usage access: same list-then-toggle flow everywhere; Android 16 renames it "App usage data".
 *
 * Pure: no Android calls, so it is unit-tested directly.
 */
object PermissionTutorialCatalog {

    fun selectTutorialPlan(
        permission: GuidedPermission,
        sdkInt: Int,
        isSideloadedInstall: Boolean,
        restrictedStage: RestrictedSettingStage,
    ): PermissionTutorialPlan = when (permission) {
        GuidedPermission.OVERLAY -> PermissionTutorialPlan(
            video = if (sdkInt >= Build.VERSION_CODES.R) TutorialVideo.OVERLAY else TutorialVideo.OVERLAY_LEGACY,
            destination = SettingsDestination.OVERLAY,
            nextRestrictedStage = restrictedStage,
        )
        GuidedPermission.USAGE_ACCESS -> PermissionTutorialPlan(
            video = if (sdkInt >= Build.VERSION_CODES.BAKLAVA) {
                TutorialVideo.USAGE_ACCESS
            } else {
                TutorialVideo.USAGE_ACCESS_LEGACY
            },
            destination = SettingsDestination.USAGE_ACCESS,
            nextRestrictedStage = restrictedStage,
        )
        GuidedPermission.ACCESSIBILITY -> selectAccessibilityPlan(sdkInt, isSideloadedInstall, restrictedStage)
    }

    private fun selectAccessibilityPlan(
        sdkInt: Int,
        isSideloadedInstall: Boolean,
        restrictedStage: RestrictedSettingStage,
    ): PermissionTutorialPlan {
        val normal = PermissionTutorialPlan(
            video = if (sdkInt >= Build.VERSION_CODES.R) {
                TutorialVideo.ACCESSIBILITY
            } else {
                TutorialVideo.ACCESSIBILITY_LEGACY
            },
            destination = SettingsDestination.ACCESSIBILITY,
            nextRestrictedStage = restrictedStage,
        )
        if (sdkInt < Build.VERSION_CODES.TIRAMISU || !isSideloadedInstall) return normal
        return when (restrictedStage) {
            RestrictedSettingStage.NOT_SEEN -> PermissionTutorialPlan(
                video = TutorialVideo.ACCESSIBILITY_RESTRICTED,
                destination = SettingsDestination.ACCESSIBILITY,
                nextRestrictedStage = RestrictedSettingStage.LOCK_SHOWN,
            )
            RestrictedSettingStage.LOCK_SHOWN -> PermissionTutorialPlan(
                video = TutorialVideo.ACCESSIBILITY_UNLOCK,
                destination = SettingsDestination.APP_DETAILS,
                nextRestrictedStage = RestrictedSettingStage.UNLOCK_SHOWN,
            )
            RestrictedSettingStage.UNLOCK_SHOWN -> normal
        }
    }
}
