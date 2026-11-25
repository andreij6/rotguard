package com.rotguard.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.window.SplashScreenView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotguard.app.designsystem.RotGuardTheme
import com.rotguard.app.storage.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: RotGuardAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_RotGuard) // leave the splash launch theme (manifest) behind
        super.onCreate(savedInstanceState)
        holdSplashUntilLaunchStateReady()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener(::playSplashExit)
        }
        enableEdgeToEdge()
        setContent {
            val appState by appViewModel.uiState.collectAsStateWithLifecycle()
            val useDark = when (appState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(useDark) {
                val barStyle = if (useDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            RotGuardTheme(themeMode = appState.themeMode) {
                RotGuardNavGraph(launchState = appState.launchState)
            }
        }
    }

    /** Keeps the splash up (no first frame) until the profile read decides onboarding vs. home. */
    private fun holdSplashUntilLaunchStateReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val ready = appViewModel.uiState.value.launchState is RotGuardLaunchState.Ready
                    if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return ready
                }
            },
        )
    }

    /** Lets the shield finish building, then lifts and fades it into the app. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun playSplashExit(splashView: SplashScreenView) {
        val iconStartMillis = splashView.iconAnimationStart?.toEpochMilli() ?: 0L // wall clock, not uptime
        val iconDurationMillis = splashView.iconAnimationDuration?.toMillis() ?: 0L
        val waitMillis = (iconStartMillis + iconDurationMillis - System.currentTimeMillis())
            .coerceIn(0L, iconDurationMillis)
        splashView.iconView?.animate()
            ?.scaleX(EXIT_ICON_SCALE)
            ?.scaleY(EXIT_ICON_SCALE)
            ?.alpha(0f)
            ?.setStartDelay(waitMillis)
            ?.setDuration(EXIT_ICON_MILLIS)
            ?.setInterpolator(AccelerateInterpolator())
        splashView.animate()
            .alpha(0f)
            .setStartDelay(waitMillis + EXIT_BACKGROUND_DELAY_MILLIS)
            .setDuration(EXIT_BACKGROUND_MILLIS)
            .withEndAction { splashView.remove() }
    }

    private companion object {
        const val EXIT_ICON_SCALE = 1.35f
        const val EXIT_ICON_MILLIS = 350L
        const val EXIT_BACKGROUND_DELAY_MILLIS = 100L
        const val EXIT_BACKGROUND_MILLIS = 400L
    }
}
