package com.rotguard.app.feature.shield

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rotguard.app.designsystem.RotGuardTheme
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FrictionFallbackActivity : ComponentActivity() {

    @Inject
    lateinit var sessionController: FrictionSessionController

    @Inject
    lateinit var shieldDataSource: ShieldDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    sessionController.onAction(ShieldAction.ProtectBrainExitTapped)
                }
            },
        )

        val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)
        if (isTestMode || sessionController.uiState.value is ShieldOverlayUiState.Hidden) {
            val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: DEFAULT_DEMO_PACKAGE
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: DEFAULT_DEMO_NAME
            launchDemoSession(AppPackageName(pkg), appName, isTestMode)
        }

        // Never leave this screen behind once its session ends (closed, continued or dismissed); otherwise it
        // resurfaces later on top of RotGuard's task.
        lifecycleScope.launch {
            sessionController.uiState.first { it !is ShieldOverlayUiState.Hidden }
            sessionController.uiState.first { it is ShieldOverlayUiState.Hidden }
            finish()
        }

        setContent {
            val uiState by sessionController.uiState.collectAsStateWithLifecycle()
            val activeThemeMode = when (val current = uiState) {
                is ShieldOverlayUiState.Blocking -> current.themeMode
                is ShieldOverlayUiState.CelebratingExit -> current.themeMode
                ShieldOverlayUiState.Hidden -> ThemeMode.SYSTEM
            }
            RotGuardTheme(themeMode = activeThemeMode) {
                FrictionOverlayScreen(
                    state = uiState,
                    onAction = sessionController::onAction,
                )
            }
        }
    }

    private fun launchDemoSession(
        packageName: AppPackageName,
        appName: String,
        isTestMode: Boolean,
    ) {
        lifecycleScope.launch {
            val decision = shieldDataSource.loadDemoBlockingDecision(packageName, appName)
            sessionController.startBlockingSession(
                decision = decision,
                onExitToHome = {
                    if (!isTestMode) {
                        navigateHome()
                    }
                    finish()
                },
                onContinueToApp = { finish() },
                countsTowardStats = !isTestMode,
            )
        }
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    companion object {
        const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        private const val DEFAULT_DEMO_PACKAGE = "com.google.android.youtube"
        private const val DEFAULT_DEMO_NAME = "YouTube"

        fun createDemoIntent(context: Context): Intent {
            return Intent(context, FrictionFallbackActivity::class.java).apply {
                putExtra(EXTRA_IS_TEST_MODE, true)
                putExtra(EXTRA_PACKAGE_NAME, DEFAULT_DEMO_PACKAGE)
                putExtra(EXTRA_APP_NAME, DEFAULT_DEMO_NAME)
            }
        }

        fun createFallbackIntent(
            context: Context,
            packageName: AppPackageName,
            appDisplayName: String,
        ): Intent {
            return Intent(context, FrictionFallbackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_IS_TEST_MODE, false)
                putExtra(EXTRA_PACKAGE_NAME, packageName.value)
                putExtra(EXTRA_APP_NAME, appDisplayName)
            }
        }
    }
}
