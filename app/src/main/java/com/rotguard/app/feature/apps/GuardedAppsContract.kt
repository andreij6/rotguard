package com.rotguard.app.feature.apps

import androidx.annotation.StringRes
import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.HoldDurationMillis

data class GuardedAppRow(
    val packageName: AppPackageName,
    val displayName: String,
    val isGuarded: Boolean,
    val isRecommended: Boolean,
    val effectiveHoldSeconds: Int,
)

sealed interface GuardedAppsUiState {
    data object Loading : GuardedAppsUiState

    data class Ready(
        val searchQuery: String,
        val globalHoldDurationMillis: HoldDurationMillis,
        val allApps: List<GuardedAppRow>,
        val filteredApps: List<GuardedAppRow>,
        val guardedCount: Int,
    ) : GuardedAppsUiState

    data class Error(@StringRes val messageRes: Int) : GuardedAppsUiState
}

sealed interface GuardedAppsAction {
    data class SearchQueryChanged(val query: String) : GuardedAppsAction
    data class GlobalHoldDurationSelected(val durationMillis: HoldDurationMillis) : GuardedAppsAction
    data class AppGuardToggled(val packageName: AppPackageName) : GuardedAppsAction
    data object RetryTapped : GuardedAppsAction
}

sealed interface GuardedAppsInternalEvent {
    data object LoadStarted : GuardedAppsInternalEvent
    data class Loaded(
        val globalHoldDurationMillis: HoldDurationMillis,
        val apps: List<GuardedAppRow>,
    ) : GuardedAppsInternalEvent
    data class SearchUpdated(val query: String) : GuardedAppsInternalEvent
    data class GlobalHoldDurationUpdated(val durationMillis: HoldDurationMillis) : GuardedAppsInternalEvent
    data class AppToggled(val packageName: AppPackageName) : GuardedAppsInternalEvent
    data class LoadFailed(@StringRes val messageRes: Int) : GuardedAppsInternalEvent
}
