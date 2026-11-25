package com.rotguard.app.feature.inspiration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import com.rotguard.app.designsystem.BotanicalCard
import com.rotguard.app.designsystem.SquarePrimaryButton
import com.rotguard.app.designsystem.SquareSegmentButton
import com.rotguard.app.designsystem.SquareTagBadge
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.designsystem.localizedStarterText
import com.rotguard.app.storage.PersonalReminderId
import com.rotguard.app.storage.ReminderCategory
import com.rotguard.app.storage.ReminderDefaults

private const val SQUARE_CORNER_DP = 6

@Composable
internal fun InspirationRemindersSection(
    state: InspirationUiState.Ready,
    onAction: (InspirationAction) -> Unit,
) {
    val palette = VerdantTheme.colors

    BotanicalCard(warm = true, borderColor = palette.warmBorder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.inspiration_reminders_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            SquareTagBadge(
                text = pluralStringResource(
                    R.plurals.inspiration_reminders_saved_badge,
                    state.reminders.size,
                    state.reminders.size,
                ),
                accentColor = palette.mossAccent,
                containerColor = palette.sageTint,
            )
        }
        Text(
            text = stringResource(R.string.inspiration_reminders_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )

        ReminderCategoryChips(
            selectedCategory = state.selectedReminderCategory,
            onSelectCategory = { onAction(InspirationAction.ReminderCategorySelected(it)) },
        )

        ReminderHintsStrip(
            category = state.selectedReminderCategory,
            onHintTapped = { onAction(InspirationAction.ReminderHintTapped(it)) },
        )

        OutlinedTextField(
            value = state.newReminderDraft,
            onValueChange = { onAction(InspirationAction.NewReminderDraftChanged(it)) },
            placeholder = { Text(stringResource(state.selectedReminderCategory.inputPlaceholderRes)) },
            singleLine = true,
            shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.mossAccent,
                unfocusedBorderColor = palette.warmBorder,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
                unfocusedPlaceholderColor = palette.inkMuted,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        SquarePrimaryButton(
            text = stringResource(R.string.inspiration_add_reminder),
            onClick = { onAction(InspirationAction.AddReminderTapped) },
            enabled = state.newReminderDraft.trim().isNotEmpty(),
            useMossAccent = true,
        )

        if (state.reminderAddedHintVisible) {
            Text(
                text = stringResource(R.string.inspiration_reminder_added),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = palette.mossAccent,
            )
        }

        SavedRemindersList(
            state = state,
            onDeleteReminder = { onAction(InspirationAction.DeleteReminderTapped(it)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCategoryChips(
    selectedCategory: ReminderCategory,
    onSelectCategory: (ReminderCategory) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        ReminderCategory.entries.forEach { category ->
            SquareSegmentButton(
                label = stringResource(category.displayNameRes),
                selected = category == selectedCategory,
                onClick = { onSelectCategory(category) },
                modifier = Modifier.fillMaxWidth(fraction = 0.48f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderHintsStrip(
    category: ReminderCategory,
    onHintTapped: (String) -> Unit,
) {
    val palette = VerdantTheme.colors
    val suggestions = ReminderDefaults.HINT_SUGGESTIONS[category].orEmpty()
    if (suggestions.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        Text(
            text = stringResource(R.string.inspiration_ideas_label),
            style = MaterialTheme.typography.labelMedium,
            color = palette.mossAccent,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            suggestions.forEach { hintRes ->
                val hint = stringResource(hintRes)
                Surface(
                    shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
                    color = palette.card,
                    border = BorderStroke(width = 1.dp, color = palette.cardBorder),
                    onClick = { onHintTapped(hint) },
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRemindersList(
    state: InspirationUiState.Ready,
    onDeleteReminder: (PersonalReminderId) -> Unit,
) {
    val palette = VerdantTheme.colors
    val reminders = state.reminders
    if (reminders.isEmpty()) {
        Text(
            text = stringResource(R.string.inspiration_reminders_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        reminders.forEach { item ->
            Surface(
                shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
                color = palette.card,
                border = BorderStroke(width = 1.dp, color = palette.cardBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
                    ) {
                        Text(
                            text = stringResource(item.category.badgeLabelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mossAccent,
                        )
                        Text(
                            text = stringResource(
                                R.string.inspiration_reminder_quoted,
                                localizedStarterText(item.text),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
                        color = palette.terracottaSoft,
                        border = BorderStroke(width = 1.dp, color = palette.terracotta.copy(alpha = 0.4f)),
                        onClick = { onDeleteReminder(item.id) },
                    ) {
                        Text(
                            text = stringResource(R.string.inspiration_remove),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.terracotta,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
