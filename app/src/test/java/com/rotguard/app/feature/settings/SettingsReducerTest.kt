package com.rotguard.app.feature.settings

import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsReducerTest {

    @Test
    fun `reduce updates permissions, theme mode, hold duration, grace window, and mantra hint`() {
        val loaded = SettingsReducer.reduce(
            SettingsUiState.Loading,
            SettingsInternalEvent.Loaded(
                SettingsSnapshot(
                    accessibilityGranted = false,
                    overlayGranted = false,
                    usageStatsGranted = false,
                    globalHoldDurationMillis = HoldDurationMillis(4_000L),
                    sessionGraceDurationMillis = HoldDurationMillis(0L),
                    hapticsEnabled = true,
                    personalMantra = "Focus",
                    themeMode = ThemeMode.LIGHT,
                    inspirationPhotoCount = 3,
                ),
            ),
        )
        val permsUpdated = SettingsReducer.reduce(
            loaded,
            SettingsInternalEvent.PermissionsUpdated(true, true, false),
        ) as SettingsUiState.Ready
        assertTrue(permsUpdated.accessibilityGranted)
        assertTrue(permsUpdated.overlayGranted)
        assertEquals(ThemeMode.LIGHT, permsUpdated.themeMode)
        assertEquals(3, permsUpdated.inspirationPhotoCount)

        val darkTheme = SettingsReducer.reduce(
            permsUpdated,
            SettingsInternalEvent.ThemeModeUpdated(ThemeMode.DARK),
        ) as SettingsUiState.Ready
        assertEquals(ThemeMode.DARK, darkTheme.themeMode)

        val draftUpdated = SettingsReducer.reduce(
            darkTheme,
            SettingsInternalEvent.MantraDraftUpdated("Ship quality work"),
        ) as SettingsUiState.Ready
        assertFalse(draftUpdated.mantraSavedHintVisible)

        val saved = SettingsReducer.reduce(draftUpdated, SettingsInternalEvent.MantraSaved) as SettingsUiState.Ready
        assertTrue(saved.mantraSavedHintVisible)
        assertEquals("Ship quality work", saved.personalMantraDraft)
    }
}
