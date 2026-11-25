package com.rotguard.app.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rotguard.app.storage.StarterTextCatalog

/** Shows a stored starter text (mantra, reminder, photo caption…) in the app language; user text as-is. */
@Composable
fun localizedStarterText(storedText: String): String =
    StarterTextCatalog.resFor(storedText)?.let { stringResource(it) } ?: storedText
