package com.rotguard.app.feature.tutorial

import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import com.rotguard.app.R
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.SystemPermissionInspector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Plays a narrated "how to turn this on" clip in picture-in-picture while the user is in system Settings.
 *
 * Flow: open -> enter PiP -> launch the Settings page -> poll the permission every second -> once granted,
 * bring RotGuard back to the front and close. Runs in its own task (manifest taskAffinity) so PiP never drags
 * the main app along. Without PiP support it plays full screen with an "Open Settings" button instead.
 */
@AndroidEntryPoint
class PermissionTutorialActivity : ComponentActivity() {

    @Inject
    lateinit var permissionInspector: SystemPermissionInspector

    private lateinit var videoView: VideoView
    private lateinit var openSettingsButton: Button
    private lateinit var permission: GuidedPermission
    private lateinit var plan: PermissionTutorialPlan
    private var player: MediaPlayer? = null
    private var narrationPlayed = false
    private var pipPending = false
    private var settingsLaunched = false
    private val grantPoller = Handler(Looper.getMainLooper())
    private val pollGrant = object : Runnable {
        override fun run() {
            if (isGranted(permissionInspector.inspectPermissions())) {
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                finish()
            } else {
                grantPoller.postDelayed(this, GRANT_POLL_INTERVAL_MILLIS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoView = VideoView(this)
        openSettingsButton = Button(this).apply {
            setText(R.string.tutorial_open_settings)
            setOnClickListener { onOpenSettingsTapped() }
        }
        val bottomMarginPx = (BUTTON_BOTTOM_MARGIN_DP * resources.displayMetrics.density).toInt()
        setContentView(
            FrameLayout(this).apply {
                fitsSystemWindows = true
                setBackgroundColor(android.graphics.Color.BLACK)
                addView(videoView, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
                addView(
                    openSettingsButton,
                    FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                        .apply { bottomMargin = bottomMarginPx },
                )
            },
        )
        videoView.setOnPreparedListener { mediaPlayer ->
            player = mediaPlayer
            applyVolume()
        }
        // Narrate once, then keep looping silently so the steps stay on screen without repeating the voice.
        videoView.setOnCompletionListener {
            narrationPlayed = true
            applyVolume()
            videoView.start()
        }
        startTutorial(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startTutorial(intent)
    }

    override fun onResume() {
        super.onResume()
        if (pipPending) {
            pipPending = false
            enterPipOrShowFallback()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            openSettingsButton.visibility = View.GONE
            if (!settingsLaunched) launchSettings()
        } else if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            finish() // the user swiped the PiP window away
        } else {
            openSettingsButton.visibility = View.VISIBLE // the user expanded it to full screen
        }
    }

    override fun onDestroy() {
        grantPoller.removeCallbacks(pollGrant)
        super.onDestroy()
    }

    private fun startTutorial(intent: Intent) {
        permission = intent.getStringExtra(EXTRA_PERMISSION)
            ?.let { name -> GuidedPermission.entries.firstOrNull { it.name == name } }
            ?: run { finish(); return }
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val stage = prefs.getString(PREF_RESTRICTED_STAGE, null)
            ?.let { name -> RestrictedSettingStage.entries.firstOrNull { it.name == name } }
            ?: RestrictedSettingStage.NOT_SEEN
        plan = PermissionTutorialCatalog.selectTutorialPlan(
            permission = permission,
            sdkInt = Build.VERSION.SDK_INT,
            isSideloadedInstall = isSideloadedInstall(),
            restrictedStage = stage,
        )
        prefs.edit { putString(PREF_RESTRICTED_STAGE, plan.nextRestrictedStage.name) }

        narrationPlayed = false
        settingsLaunched = false
        videoView.setVideoURI("android.resource://$packageName/${plan.video.rawRes()}".toUri())
        videoView.start()
        grantPoller.removeCallbacks(pollGrant)
        grantPoller.postDelayed(pollGrant, GRANT_POLL_INTERVAL_MILLIS)

        if (isInPictureInPictureMode) {
            launchSettings()
        } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            enterPipOrShowFallback()
        } else {
            pipPending = true // PiP can only be entered once resumed
        }
    }

    private fun enterPipOrShowFallback() {
        val entered = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            runCatching {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(VIDEO_WIDTH, VIDEO_HEIGHT)).build(),
                )
            }.getOrDefault(false)
        openSettingsButton.visibility = if (entered) View.GONE else View.VISIBLE
    }

    private fun onOpenSettingsTapped() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            settingsLaunched = false
            enterPipOrShowFallback()
            if (openSettingsButton.visibility == View.GONE) return
        }
        launchSettings() // no PiP (disabled for RotGuard, or unsupported): just open Settings
        finish()
    }

    private fun launchSettings() {
        settingsLaunched = true
        val packageUri = "package:$packageName".toUri()
        val candidates = when (plan.destination) {
            SettingsDestination.ACCESSIBILITY -> listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            SettingsDestination.OVERLAY -> listOf(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            )
            SettingsDestination.USAGE_ACCESS -> listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            SettingsDestination.APP_DETAILS -> listOf(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
        }
        candidates.firstOrNull { candidate ->
            try {
                startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    private fun applyVolume() {
        val volume = if (narrationPlayed) 0f else 1f
        runCatching { player?.setVolume(volume, volume) }
    }

    private fun isGranted(snapshot: PermissionStatusSnapshot): Boolean = when (permission) {
        GuidedPermission.ACCESSIBILITY -> snapshot.accessibilityEnabled
        GuidedPermission.OVERLAY -> snapshot.overlayEnabled
        GuidedPermission.USAGE_ACCESS -> snapshot.usageStatsEnabled
    }

    /** Android 13+ only locks accessibility for apps installed from a file, not from a store or adb. */
    private fun isSideloadedInstall(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val source = runCatching { packageManager.getInstallSourceInfo(packageName).packageSource }.getOrNull()
        return source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
            source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
    }

    private fun TutorialVideo.rawRes(): Int = when (this) {
        TutorialVideo.ACCESSIBILITY -> R.raw.tutorial_accessibility
        TutorialVideo.ACCESSIBILITY_LEGACY -> R.raw.tutorial_accessibility_legacy
        TutorialVideo.ACCESSIBILITY_RESTRICTED -> R.raw.tutorial_accessibility_restricted
        TutorialVideo.ACCESSIBILITY_UNLOCK -> R.raw.tutorial_accessibility_unlock
        TutorialVideo.OVERLAY -> R.raw.tutorial_overlay
        TutorialVideo.OVERLAY_LEGACY -> R.raw.tutorial_overlay_legacy
        TutorialVideo.USAGE_ACCESS -> R.raw.tutorial_usage_access
        TutorialVideo.USAGE_ACCESS_LEGACY -> R.raw.tutorial_usage_access_legacy
    }

    companion object {
        private const val EXTRA_PERMISSION = "extra_guided_permission"
        private const val PREFS_NAME = "permission_tutorial"
        private const val PREF_RESTRICTED_STAGE = "restricted_setting_stage"
        private const val GRANT_POLL_INTERVAL_MILLIS = 1_000L
        private const val VIDEO_WIDTH = 9
        private const val VIDEO_HEIGHT = 16
        private const val BUTTON_BOTTOM_MARGIN_DP = 32
        private const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT

        fun createIntent(context: Context, permission: GuidedPermission): Intent =
            Intent(context, PermissionTutorialActivity::class.java)
                .putExtra(EXTRA_PERMISSION, permission.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
