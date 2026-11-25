package com.rotguard.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.SquareOutlineButton
import com.rotguard.app.designsystem.VerdantTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Apps the user put on a break from a repeat pause screen, each with a way to end the break early. */
@Composable
internal fun AppBreaksCard(
    breaks: List<AppBreak>,
    onResume: (String) -> Unit,
) {
    val palette = VerdantTheme.colors
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    BotanicalCard(warm = true, borderColor = palette.warmBorder) {
        Text(
            text = stringResource(R.string.home_break_title),
            style = MaterialTheme.typography.labelMedium,
            color = palette.terracotta,
        )
        breaks.forEach { appBreak ->
            val until = Instant.ofEpochMilli(appBreak.untilEpochMillis).atZone(ZoneId.systemDefault())
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_break_until, appBreak.appName, timeFormat.format(until)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.ink,
                )
                SquareOutlineButton(
                    text = stringResource(R.string.home_break_resume),
                    onClick = { onResume(appBreak.packageName) },
                    borderColor = palette.mossAccent,
                    textColor = palette.mossAccent,
                )
            }
        }
    }
}
