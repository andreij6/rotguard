package com.rotguard.app.feature.inspiration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotguard.app.R
import com.rotguard.app.designsystem.InspirationPhotoThumbnail
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.storage.InspirationPhotoId
import java.util.Locale

private const val GRID_COLUMNS = 2
private const val TILE_CORNER_DP = 14
private const val TAB_BAR_HEIGHT_DP = 48
private const val REMOVE_BUTTON_DP = 28
private const val REMOVE_SCRIM_ALPHA = 0.6f
private const val DASH_ON_PX = 14f
private const val DASH_OFF_PX = 10f
private const val ADD_GLYPH_SP = 34
private const val HIDDEN_PHOTO_ALPHA = 0.45f

/** The three parts of the Inspiration page, one per tab. */
internal enum class InspirationTab(val labelRes: Int) {
    REMINDERS(R.string.inspiration_tab_reminders),
    PHOTOS(R.string.inspiration_tab_photos),
    PAUSE_SCREEN(R.string.inspiration_tab_pause),
}

/** Pill tab bar in the option 5 nav style; counts show beside Reminders and Photos. */
@Composable
internal fun InspirationTabBar(
    selected: InspirationTab,
    reminderCount: Int,
    photoCount: Int,
    onSelect: (InspirationTab) -> Unit,
) {
    val palette = VerdantTheme.colors
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = TAB_BAR_HEIGHT_DP.dp)
            .clip(pill)
            .background(palette.card)
            .border(width = 1.dp, color = palette.cardBorder, shape = pill)
            .padding(all = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InspirationTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val count = when (tab) {
                InspirationTab.REMINDERS -> reminderCount
                InspirationTab.PHOTOS -> photoCount
                InspirationTab.PAUSE_SCREEN -> null
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(pill)
                    .background(if (isSelected) palette.sageTint else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab) },
                horizontalArrangement = Arrangement.spacedBy(space = 5.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) palette.ink else palette.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count != null) {
                    Text(
                        text = String.format(Locale.getDefault(), "%d", count),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) palette.mossAccent else palette.inkMuted,
                    )
                }
            }
        }
    }
}

/** Two-column grid: an "Add photo" tile first, then each photo with its caption and a remove button. */
@Composable
internal fun InspirationPhotoGrid(
    photos: List<InspirationPhotoItem>,
    onAddPhotoClick: () -> Unit,
    onDeletePhoto: (InspirationPhotoId) -> Unit,
    onTogglePhoto: (InspirationPhotoId) -> Unit,
) {
    val tiles: List<InspirationPhotoItem?> = listOf(null) + photos
    Column(verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
        tiles.chunked(GRID_COLUMNS).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                rowTiles.forEach { photo ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (photo == null) {
                            AddPhotoTile(onClick = onAddPhotoClick)
                        } else {
                            PhotoTile(
                                photo = photo,
                                onDelete = { onDeletePhoto(photo.id) },
                                onToggle = { onTogglePhoto(photo.id) },
                            )
                        }
                    }
                }
                repeat(GRID_COLUMNS - rowTiles.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    val palette = VerdantTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio = 1f)
            .clip(RoundedCornerShape(size = TILE_CORNER_DP.dp))
            .background(palette.sageTint.copy(alpha = 0.5f))
            .drawBehind {
                drawRoundRect(
                    color = palette.mossAccent,
                    cornerRadius = CornerRadius(TILE_CORNER_DP.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_PX, DASH_OFF_PX)),
                    ),
                )
            }
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "+", fontSize = ADD_GLYPH_SP.sp, color = palette.mossAccent)
        Text(
            text = stringResource(R.string.inspiration_add_photo_tile),
            style = MaterialTheme.typography.labelLarge,
            color = palette.mossAccent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoTile(photo: InspirationPhotoItem, onDelete: () -> Unit, onToggle: () -> Unit) {
    val palette = VerdantTheme.colors
    val title = localizedStarterText(photo.title)
    Column(
        modifier = Modifier.alpha(if (photo.isEnabled) 1f else HIDDEN_PHOTO_ALPHA),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1f),
        ) {
            InspirationPhotoThumbnail(
                filePath = photo.filePath,
                bundledDrawableName = photo.bundledDrawableName,
                contentDescription = title,
                borderColor = palette.cardBorder,
                modifier = Modifier.fillMaxSize(),
            )
            if (photo.isStock) {
                StockPhotoToggle(
                    enabled = photo.isEnabled,
                    onToggle = onToggle,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            } else {
                Surface(
                    onClick = onDelete,
                    shape = CircleShape,
                    color = palette.canopy.copy(alpha = REMOVE_SCRIM_ALPHA),
                    border = BorderStroke(width = 1.dp, color = palette.cream.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(all = 8.dp)
                        .size(size = REMOVE_BUTTON_DP.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.inspiration_remove_photo_cd),
                            tint = palette.cream,
                            modifier = Modifier.size(size = 16.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (photo.isEnabled) {
                localizedStarterText(photo.caption)
            } else {
                stringResource(R.string.photo_toggle_hidden)
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Round check on stock photos: filled = shown on the pause screen, empty = hidden. Stock photos can't be deleted. */
@Composable
private fun StockPhotoToggle(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val palette = VerdantTheme.colors
    val label = stringResource(if (enabled) R.string.photo_toggle_hide_cd else R.string.photo_toggle_show_cd)
    Surface(
        onClick = onToggle,
        shape = CircleShape,
        color = if (enabled) palette.mossAccent else palette.canopy.copy(alpha = REMOVE_SCRIM_ALPHA),
        border = BorderStroke(width = 1.5.dp, color = palette.cream.copy(alpha = if (enabled) 0.9f else 0.6f)),
        modifier = modifier
            .padding(all = 8.dp)
            .size(size = REMOVE_BUTTON_DP.dp)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (enabled) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = palette.cream,
                    modifier = Modifier.size(size = 16.dp),
                )
            }
        }
    }
}
