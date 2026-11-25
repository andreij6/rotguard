package com.rotguard.app.feature.shield

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.VerdantTheme

private const val CHIP_CORNER_DP = 6
private const val DISABLED_CHIPS_ALPHA = 0.4f

/**
 * Shown on the 2nd+ pause screen for an app within 5 minutes: tick to stop pausing it for 30 min / 1 hr / 2 hr.
 * The break only starts if the user then holds to continue; ending it early lives on the Home screen.
 */
@Composable
internal fun TakeABreakOption(
    appName: String,
    selectedMinutes: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = VerdantTheme.colors
    val checked = selectedMinutes != null
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox) { tick ->
                    onSelect(if (tick) ShieldReducer.GRACE_OPTIONS_MINUTES.first() else null)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.sunGold,
                    uncheckedColor = palette.canopyMuted,
                    checkmarkColor = palette.canopy,
                ),
            )
            Text(
                text = stringResource(R.string.shield_break_checkbox, appName),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.cream,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (checked) 1f else DISABLED_CHIPS_ALPHA),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            ShieldReducer.GRACE_OPTIONS_MINUTES.forEach { minutes ->
                val selected = minutes == selectedMinutes
                Surface(
                    onClick = { onSelect(minutes) },
                    enabled = checked,
                    shape = RoundedCornerShape(size = CHIP_CORNER_DP.dp),
                    color = if (selected) palette.sunGold.copy(alpha = 0.18f) else palette.canopyCard,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) palette.sunGold else palette.canopyStroke,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(breakLabelRes(minutes)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) palette.sunGold else palette.cream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
        if (checked) {
            Text(
                text = stringResource(R.string.shield_break_hint),
                style = MaterialTheme.typography.bodySmall,
                color = palette.canopyMuted,
            )
        }
    }
}

private fun breakLabelRes(minutes: Int): Int = when (minutes) {
    ShieldReducer.GRACE_OPTIONS_MINUTES[0] -> R.string.shield_break_30
    ShieldReducer.GRACE_OPTIONS_MINUTES[1] -> R.string.shield_break_60
    else -> R.string.shield_break_120
}
