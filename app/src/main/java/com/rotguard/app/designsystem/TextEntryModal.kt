package com.rotguard.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private const val FIELD_CORNER_DP = 6
private const val FIELD_MAX_LINES = 3

/**
 * Edits a short piece of text in a bottom sheet that rides up on top of the keyboard, so the field can never be
 * hidden behind it. Opens focused with the keyboard up. Done, the keyboard's done key, swiping down, or tapping the
 * dimmed area closes it. Every keystroke goes to [onValueChange], so nothing is lost on dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntryModal(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    doneLabel: String,
    onDismiss: () -> Unit,
    supportingText: String? = null,
) {
    val palette = VerdantTheme.colors
    val focusRequester = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(value.length))) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.card,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        Column(
            modifier = Modifier
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    onValueChange(it.text)
                },
                maxLines = FIELD_MAX_LINES,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onDismiss() }),
                shape = RoundedCornerShape(size = FIELD_CORNER_DP.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.mossAccent,
                    unfocusedBorderColor = palette.cardBorder,
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                    cursorColor = palette.mossAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            if (supportingText != null) {
                Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = palette.inkMuted)
            }
            SquarePrimaryButton(text = doneLabel, onClick = onDismiss, useMossAccent = true)
        }
    }
}
