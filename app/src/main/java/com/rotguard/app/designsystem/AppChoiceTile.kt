package com.rotguard.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rotguard.app.storage.AppPackageName

private const val TILE_ICON_DP = 48

/**
 * One app in a 2-column picker grid: centered icon, name and status line. The optional [badgeText] chip and the
 * checkbox sit in the top corners so every tile in a row has the same height.
 */
@Composable
fun AppChoiceTile(
    packageName: AppPackageName,
    displayName: String,
    selected: Boolean,
    statusText: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    statusColor: Color = VerdantTheme.colors.inkMuted,
    badgeText: String? = null,
) {
    val palette = VerdantTheme.colors
    BotanicalCard(
        modifier = modifier,
        borderColor = if (selected) palette.mossAccent else palette.cardBorder,
        onClick = onToggle,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                Spacer(modifier = Modifier.height(height = 14.dp))
                InstalledAppIcon(packageName = packageName, displayName = displayName, size = TILE_ICON_DP.dp)
                Spacer(modifier = Modifier.height(height = 2.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (badgeText != null) {
                SquareTagBadge(
                    text = badgeText,
                    accentColor = palette.terracotta,
                    containerColor = palette.terracottaSoft,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            SquareCheckbox(
                checked = selected,
                onCheckedChange = onToggle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
