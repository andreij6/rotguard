package com.rotguard.app.feature.apps

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.HoldDurationMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardedAppsReducerTest {

    @Test
    fun `reduce filters apps by search query and toggles guard count`() {
        val loaded = GuardedAppsReducer.reduce(
            GuardedAppsUiState.Loading,
            GuardedAppsInternalEvent.Loaded(
                globalHoldDurationMillis = HoldDurationMillis(4_000L),
                apps = listOf(
                    GuardedAppRow(AppPackageName("com.google.android.youtube"), "YouTube", true, true, 4),
                    GuardedAppRow(AppPackageName("com.android.chrome"), "Chrome", false, true, 4),
                ),
            ),
        ) as GuardedAppsUiState.Ready

        assertEquals(1, loaded.guardedCount)
        val searched = GuardedAppsReducer.reduce(
            loaded,
            GuardedAppsInternalEvent.SearchUpdated("tube"),
        ) as GuardedAppsUiState.Ready
        assertEquals(1, searched.filteredApps.size)
        assertEquals("YouTube", searched.filteredApps.first().displayName)

        val toggled = GuardedAppsReducer.reduce(
            searched,
            GuardedAppsInternalEvent.AppToggled(AppPackageName("com.google.android.youtube")),
        ) as GuardedAppsUiState.Ready
        assertEquals(0, toggled.guardedCount)
        assertFalse(toggled.filteredApps.first().isGuarded)
    }

    @Test
    fun `reduce updates effectiveHoldSeconds across apps when global hold duration changes`() {
        val loaded = GuardedAppsReducer.reduce(
            GuardedAppsUiState.Loading,
            GuardedAppsInternalEvent.Loaded(
                globalHoldDurationMillis = HoldDurationMillis(4_000L),
                apps = listOf(
                    GuardedAppRow(AppPackageName("com.google.android.youtube"), "YouTube", true, true, 4),
                ),
            ),
        )

        val updated = GuardedAppsReducer.reduce(
            loaded,
            GuardedAppsInternalEvent.GlobalHoldDurationUpdated(HoldDurationMillis(10_000L)),
        ) as GuardedAppsUiState.Ready

        assertEquals(10_000L, updated.globalHoldDurationMillis.value)
        assertTrue(updated.allApps.all { it.effectiveHoldSeconds == 10 })
    }
}
