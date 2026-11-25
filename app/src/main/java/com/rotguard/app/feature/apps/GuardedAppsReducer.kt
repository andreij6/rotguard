package com.rotguard.app.feature.apps

import com.rotguard.app.storage.HoldDurationMillis

/**
 * Pure deterministic reducer for the Guarded Apps rules and search list.
 *
 * **Responsibility & Boundaries**
 * - Filters installed apps by [GuardedAppsUiState.Ready.searchQuery], toggles per-app guard flags,
 *   and updates global friction hold presets.
 *
 * **Side Effects**
 * - None. Must never read `PackageManager` or write Room DAOs.
 *
 * **Invariants**
 * - `guardedCount` always matches `allApps.count { it.isGuarded }`.
 * - `filteredApps` preserves guarded/recommended sort ordering while matching query substrings.
 *
 * **Anti-Patterns**
 * - Never launch search debounce timers or mutate lists in place inside [reduce].
 */
object GuardedAppsReducer {

    private const val MILLIS_PER_SECOND = 1_000L

    fun reduce(state: GuardedAppsUiState, event: GuardedAppsInternalEvent): GuardedAppsUiState {
        return when (event) {
            GuardedAppsInternalEvent.LoadStarted -> GuardedAppsUiState.Loading
            is GuardedAppsInternalEvent.LoadFailed -> GuardedAppsUiState.Error(event.messageRes)
            is GuardedAppsInternalEvent.Loaded -> GuardedAppsUiState.Ready(
                searchQuery = "",
                globalHoldDurationMillis = event.globalHoldDurationMillis,
                allApps = event.apps,
                filteredApps = event.apps,
                guardedCount = event.apps.count { it.isGuarded },
            )
            is GuardedAppsInternalEvent.SearchUpdated -> reduceSearch(state, event.query)
            is GuardedAppsInternalEvent.GlobalHoldDurationUpdated -> reduceGlobalHold(state, event.durationMillis)
            is GuardedAppsInternalEvent.AppToggled -> reduceAppToggle(state, event.packageName.value)
        }
    }

    private fun reduceSearch(state: GuardedAppsUiState, query: String): GuardedAppsUiState {
        val ready = state as? GuardedAppsUiState.Ready ?: return state
        return ready.copy(
            searchQuery = query,
            filteredApps = filterApps(ready.allApps, query),
        )
    }

    private fun reduceGlobalHold(
        state: GuardedAppsUiState,
        durationMillis: HoldDurationMillis,
    ): GuardedAppsUiState {
        val ready = state as? GuardedAppsUiState.Ready ?: return state
        val holdSec = (durationMillis.value / MILLIS_PER_SECOND).toInt().coerceAtLeast(1)
        val updated = ready.allApps.map { it.copy(effectiveHoldSeconds = holdSec) }
        return ready.copy(
            globalHoldDurationMillis = durationMillis,
            allApps = updated,
            filteredApps = filterApps(updated, ready.searchQuery),
        )
    }

    private fun reduceAppToggle(
        state: GuardedAppsUiState,
        targetPackage: String,
    ): GuardedAppsUiState {
        val ready = state as? GuardedAppsUiState.Ready ?: return state
        val updated = ready.allApps.map { row ->
            if (row.packageName.value == targetPackage) row.copy(isGuarded = !row.isGuarded) else row
        }
        return ready.copy(
            allApps = updated,
            filteredApps = filterApps(updated, ready.searchQuery),
            guardedCount = updated.count { it.isGuarded },
        )
    }

    private fun filterApps(apps: List<GuardedAppRow>, query: String): List<GuardedAppRow> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return apps
        return apps.filter {
            it.displayName.lowercase().contains(trimmed) ||
                it.packageName.value.lowercase().contains(trimmed)
        }
    }
}
