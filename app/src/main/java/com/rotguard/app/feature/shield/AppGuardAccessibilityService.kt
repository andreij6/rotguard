package com.rotguard.app.feature.shield

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.rotguard.app.storage.ClockProvider
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppGuardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var shieldDataSource: ShieldDataSource

    @Inject
    lateinit var sessionController: FrictionSessionController

    @Inject
    lateinit var clockProvider: ClockProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackerState = ShieldTrackerState()
    private var evaluationJob: Job? = null
    private var overlayWindow: FrictionOverlayWindow? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayWindow = FrictionOverlayWindow(this, sessionController)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPkg = event.packageName?.toString() ?: return

        evaluationJob?.cancel()
        evaluationJob = serviceScope.launch {
            try {
                val context = shieldDataSource.loadShieldEvaluationContext()
                val result = ShieldReducer.evaluateForegroundTransition(
                    trackerState = trackerState,
                    foregroundPackageName = foregroundPkg,
                    ownPackageName = packageName,
                    context = context,
                )
                trackerState = result.nextTrackerState
                val decision = result.decision
                if (decision is ShieldDecision.ShowOverlay) {
                    shieldDataSource.recordBlockerRotationUsed(
                        variationIndex = decision.variationIndex,
                        inspirationIndex = decision.inspirationIndex,
                        reminderId = decision.selectedReminderId,
                    )
                    presentShieldOverlay(
                        decision = decision,
                        sessionGraceDurationMs = context.sessionGraceDurationMillis.value,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Keep service resilient on transient Room I/O hiccups.
            } catch (_: IllegalStateException) {
                // Keep service resilient if state transitions mid-teardown.
            }
        }
    }

    override fun onInterrupt() {
        overlayWindow?.detach()
        sessionController.dismissSilently()
        trackerState = trackerState.copy(isOverlayShowing = false)
    }

    override fun onDestroy() {
        evaluationJob?.cancel()
        overlayWindow?.detach()
        sessionController.dismissSilently()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun presentShieldOverlay(
        decision: ShieldDecision.ShowOverlay,
        sessionGraceDurationMs: Long,
    ) {
        val onExit = {
            // Home first, then drop the overlay: otherwise the closing app is briefly back in front, sends a fresh
            // window event, and the pause screen pops up again over it.
            performGlobalAction(GLOBAL_ACTION_HOME)
            overlayWindow?.detach()
            trackerState = trackerState.copy(
                isOverlayShowing = false,
                activeGuardedApp = null,
                justClosedApp = decision.packageName,
            )
        }
        val onContinue = {
            overlayWindow?.detach()
            val updatedGraceMap = if (sessionGraceDurationMs > 0L) {
                val untilEpoch = clockProvider.nowEpochMillis().value + sessionGraceDurationMs
                trackerState.graceGrantedUntilEpochByApp + (decision.packageName.value to untilEpoch)
            } else {
                trackerState.graceGrantedUntilEpochByApp
            }
            trackerState = trackerState.copy(
                isOverlayShowing = false,
                activeGuardedApp = decision.packageName,
                graceGrantedUntilEpochByApp = updatedGraceMap,
            )
        }

        sessionController.startBlockingSession(
            decision = decision,
            onExitToHome = onExit,
            onContinueToApp = onContinue,
        )
        if (Settings.canDrawOverlays(this)) {
            overlayWindow?.attach(
                onBackPressed = { sessionController.onAction(ShieldAction.ProtectBrainExitTapped) },
            )
        } else {
            startActivity(
                FrictionFallbackActivity.createFallbackIntent(
                    context = this,
                    packageName = decision.packageName,
                    appDisplayName = decision.appDisplayName,
                ),
            )
        }
    }
}
