package com.rotguard.app.feature.shield

import android.content.Context
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.rotguard.app.R
import com.rotguard.app.designsystem.RotGuardTheme
import com.rotguard.app.storage.ThemeMode

internal class FrictionOverlayWindow(
    private val context: Context,
    private val controller: FrictionSessionController,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootContainer: BackInterceptFrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun attach(onBackPressed: () -> Unit) {
        if (rootContainer != null) return

        val themedContext = ContextThemeWrapper(context, R.style.Theme_RotGuard)
        val owner = OverlayLifecycleOwner()
        val container = BackInterceptFrameLayout(themedContext, onBackPressed).apply {
            isFocusableInTouchMode = true
            requestFocus()
        }
        val composeView = ComposeView(themedContext).apply {
            setContent {
                val state by controller.uiState.collectAsStateWithLifecycle()
                val activeThemeMode = when (val current = state) {
                    is ShieldOverlayUiState.Blocking -> current.themeMode
                    is ShieldOverlayUiState.CelebratingExit -> current.themeMode
                    ShieldOverlayUiState.Hidden -> ThemeMode.SYSTEM
                }
                RotGuardTheme(themeMode = activeThemeMode) {
                    FrictionOverlayScreen(
                        state = state,
                        onAction = controller::onAction,
                    )
                }
            }
        }
        container.addView(composeView)
        container.setViewTreeLifecycleOwner(owner)
        container.setViewTreeViewModelStoreOwner(owner)
        container.setViewTreeSavedStateRegistryOwner(owner)

        owner.onAttach()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(container, params)
            rootContainer = container
            lifecycleOwner = owner
        } catch (_: WindowManager.BadTokenException) {
            owner.onDetach()
        } catch (_: SecurityException) {
            owner.onDetach()
        }
    }

    fun detach() {
        val container = rootContainer ?: return
        rootContainer = null
        val owner = lifecycleOwner
        lifecycleOwner = null
        try {
            windowManager.removeViewImmediate(container)
        } catch (_: IllegalArgumentException) {
            // View was already detached by WindowManager.
        } finally {
            owner?.onDetach()
        }
    }

    private class BackInterceptFrameLayout(
        context: Context,
        private val onBackPressed: () -> Unit,
    ) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackPressed()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }
}
