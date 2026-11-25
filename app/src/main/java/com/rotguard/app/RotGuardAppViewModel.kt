package com.rotguard.app

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotguard.app.storage.ThemeMode
import com.rotguard.app.storage.UserProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RotGuardLaunchState {
    data object Loading : RotGuardLaunchState
    data class Ready(val showOnboardingFirst: Boolean) : RotGuardLaunchState
}

@Immutable
data class RotGuardAppState(
    val launchState: RotGuardLaunchState = RotGuardLaunchState.Loading,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/**
 * Observes the persisted user profile to resolve initial navigation and live theme changes.
 *
 * **Responsibility & Boundaries**
 * - Exposes a single [uiState] stream containing the cold-start destination flag and current [ThemeMode].
 *
 * **Side Effects**
 * - Collects `user_profile` updates from [UserProfileDao].
 *
 * **Invariants**
 * - Locks `showOnboardingFirst` after the first profile emission so toggling settings does not reset navigation.
 * - Defaults safely to onboarding on first install (`profile == null`) or transient read errors.
 * - Always rethrows [CancellationException].
 *
 * **Anti-Patterns**
 * - Never query Room synchronously on the main thread during `MainActivity.onCreate`.
 */
@HiltViewModel
class RotGuardAppViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RotGuardAppState())
    val uiState: StateFlow<RotGuardAppState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                userProfileDao.observeProfile().collect { profile ->
                    val current = _uiState.value
                    val resolvedLaunch = when (current.launchState) {
                        RotGuardLaunchState.Loading -> {
                            val completed = profile?.onboardingCompleted == true
                            RotGuardLaunchState.Ready(showOnboardingFirst = !completed)
                        }
                        is RotGuardLaunchState.Ready -> current.launchState
                    }
                    val resolvedTheme = profile?.themeModeName?.let(ThemeMode::fromStorageName)
                        ?: ThemeMode.SYSTEM
                    _uiState.value = RotGuardAppState(
                        launchState = resolvedLaunch,
                        themeMode = resolvedTheme,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.value = RotGuardAppState(
                    launchState = RotGuardLaunchState.Ready(showOnboardingFirst = true),
                    themeMode = ThemeMode.SYSTEM,
                )
            } catch (_: IllegalStateException) {
                _uiState.value = RotGuardAppState(
                    launchState = RotGuardLaunchState.Ready(showOnboardingFirst = true),
                    themeMode = ThemeMode.SYSTEM,
                )
            }
        }
    }
}
