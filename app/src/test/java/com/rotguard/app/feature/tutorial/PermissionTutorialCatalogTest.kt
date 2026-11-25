package com.rotguard.app.feature.tutorial

import com.rotguard.app.feature.tutorial.GuidedPermission.ACCESSIBILITY
import com.rotguard.app.feature.tutorial.GuidedPermission.OVERLAY
import com.rotguard.app.feature.tutorial.GuidedPermission.USAGE_ACCESS
import com.rotguard.app.feature.tutorial.RestrictedSettingStage.LOCK_SHOWN
import com.rotguard.app.feature.tutorial.RestrictedSettingStage.NOT_SEEN
import com.rotguard.app.feature.tutorial.RestrictedSettingStage.UNLOCK_SHOWN
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionTutorialCatalogTest {

    private fun plan(
        permission: GuidedPermission,
        sdk: Int,
        sideloaded: Boolean = false,
        stage: RestrictedSettingStage = NOT_SEEN,
    ) = PermissionTutorialCatalog.selectTutorialPlan(permission, sdk, sideloaded, stage)

    @Test
    fun `accessibility uses legacy clip through Android 10 and modern from 11`() {
        assertEquals(TutorialVideo.ACCESSIBILITY_LEGACY, plan(ACCESSIBILITY, sdk = 29).video)
        assertEquals(TutorialVideo.ACCESSIBILITY, plan(ACCESSIBILITY, sdk = 30).video)
        assertEquals(SettingsDestination.ACCESSIBILITY, plan(ACCESSIBILITY, sdk = 34).destination)
    }

    @Test
    fun `sideloaded install on Android 13+ walks lock then unlock then normal`() {
        val lock = plan(ACCESSIBILITY, sdk = 33, sideloaded = true, stage = NOT_SEEN)
        assertEquals(TutorialVideo.ACCESSIBILITY_RESTRICTED, lock.video)
        assertEquals(SettingsDestination.ACCESSIBILITY, lock.destination)
        assertEquals(LOCK_SHOWN, lock.nextRestrictedStage)

        val unlock = plan(ACCESSIBILITY, sdk = 35, sideloaded = true, stage = lock.nextRestrictedStage)
        assertEquals(TutorialVideo.ACCESSIBILITY_UNLOCK, unlock.video)
        assertEquals(SettingsDestination.APP_DETAILS, unlock.destination)
        assertEquals(UNLOCK_SHOWN, unlock.nextRestrictedStage)

        val normal = plan(ACCESSIBILITY, sdk = 36, sideloaded = true, stage = unlock.nextRestrictedStage)
        assertEquals(TutorialVideo.ACCESSIBILITY, normal.video)
        assertEquals(UNLOCK_SHOWN, normal.nextRestrictedStage)
    }

    @Test
    fun `restricted flow is skipped for store installs and before Android 13`() {
        assertEquals(TutorialVideo.ACCESSIBILITY, plan(ACCESSIBILITY, sdk = 34, sideloaded = false).video)
        assertEquals(TutorialVideo.ACCESSIBILITY, plan(ACCESSIBILITY, sdk = 32, sideloaded = true).video)
        assertEquals(NOT_SEEN, plan(ACCESSIBILITY, sdk = 32, sideloaded = true).nextRestrictedStage)
    }

    @Test
    fun `overlay switches to the app list clip on Android 11`() {
        assertEquals(TutorialVideo.OVERLAY_LEGACY, plan(OVERLAY, sdk = 29).video)
        assertEquals(TutorialVideo.OVERLAY, plan(OVERLAY, sdk = 30).video)
        assertEquals(SettingsDestination.OVERLAY, plan(OVERLAY, sdk = 26).destination)
    }

    @Test
    fun `usage access switches to App usage data clip on Android 16 and leaves the restricted stage alone`() {
        assertEquals(TutorialVideo.USAGE_ACCESS, plan(USAGE_ACCESS, sdk = 36).video)
        val usage = plan(USAGE_ACCESS, sdk = 34, sideloaded = true, stage = LOCK_SHOWN)
        assertEquals(TutorialVideo.USAGE_ACCESS_LEGACY, usage.video)
        assertEquals(SettingsDestination.USAGE_ACCESS, usage.destination)
        assertEquals(LOCK_SHOWN, usage.nextRestrictedStage)
    }
}
