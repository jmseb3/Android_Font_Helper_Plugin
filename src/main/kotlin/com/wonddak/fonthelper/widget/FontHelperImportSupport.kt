package com.wonddak.fonthelper.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.wonddak.fonthelper.util.GoogleFontFileRef
import com.wonddak.fonthelper.util.GoogleFileSelectionState
import com.wonddak.fonthelper.util.ImportConflictSelectionState
import com.wonddak.fonthelper.util.FontSlotKey
import java.io.File

@Composable
internal fun GoogleFileSelectionDialog(
    state: GoogleFileSelectionState,
    onDismiss: () -> Unit,
    onApply: (Map<FontSlotKey, GoogleFontFileRef>) -> Unit,
) {
    val conflictSlots = remember(state) { state.conflictCandidatesBySlot.keys.toList() }
    val initial = remember(state) { state.conflictCandidatesBySlot.mapValues { (_, refs) -> refs.first() } }
    var selections by remember(state) { mutableStateOf(initial) }
    var currentIndex by remember(state) { mutableStateOf(0) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Select Google Fonts Files",
        state = rememberDialogState(size = DpSize(760.dp, 520.dp)),
    ) {
        Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "\"${state.family}\" has duplicate matches. Select one file only for conflict slots.",
                    style = MaterialTheme.typography.body2,
                )
                Text(
                    text = "${state.autoSelectedBySlot.size} slots were auto-selected.",
                    style = MaterialTheme.typography.caption,
                )
                if (conflictSlots.isNotEmpty()) {
                    val slot = conflictSlots[currentIndex]
                    val refs = state.conflictCandidatesBySlot[slot].orEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { if (currentIndex > 0) currentIndex -= 1 },
                            enabled = currentIndex > 0,
                        ) { Text("Prev") }
                        Text(
                            text = "${currentIndex + 1} / ${conflictSlots.size}",
                            style = MaterialTheme.typography.caption,
                        )
                        TextButton(
                            onClick = { if (currentIndex < conflictSlots.lastIndex) currentIndex += 1 },
                            enabled = currentIndex < conflictSlots.lastIndex,
                        ) { Text("Next") }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(text = slot.displayText(), fontWeight = FontWeight.SemiBold)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                refs.forEach { ref ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = selections[slot] == ref,
                                            onClick = {
                                                selections = selections + (slot to ref)
                                                if (currentIndex < conflictSlots.lastIndex) currentIndex += 1
                                            },
                                        )
                                        Text(
                                            text = ref.filename.substringAfterLast('/').substringAfterLast('\\'),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onApply(selections) },
                        enabled = selections.size == state.conflictCandidatesBySlot.size,
                    ) {
                        Text("Download Selected")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImportConflictDialog(
    state: ImportConflictSelectionState,
    onDismiss: () -> Unit,
    onApply: (Map<FontSlotKey, File>) -> Unit,
) {
    val conflictSlots = remember(state) { state.conflicts.map { it.slot } }
    val initialSelection = remember(state) { state.conflicts.associate { it.slot to it.candidates.first() } }
    var selections by remember(state) { mutableStateOf(initialSelection) }
    var currentIndex by remember(state) { mutableStateOf(0) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Resolve Font Type Conflicts",
        state = rememberDialogState(size = DpSize(720.dp, 520.dp)),
    ) {
        Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Multiple files matched the same type. Select one file per slot.",
                    style = MaterialTheme.typography.body2,
                )
                if (conflictSlots.isNotEmpty()) {
                    val slot = conflictSlots[currentIndex]
                    val conflict = state.conflicts.first { it.slot == slot }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { if (currentIndex > 0) currentIndex -= 1 },
                            enabled = currentIndex > 0,
                        ) { Text("Prev") }
                        Text(
                            text = "${currentIndex + 1} / ${conflictSlots.size}",
                            style = MaterialTheme.typography.caption,
                        )
                        TextButton(
                            onClick = { if (currentIndex < conflictSlots.lastIndex) currentIndex += 1 },
                            enabled = currentIndex < conflictSlots.lastIndex,
                        ) { Text("Next") }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(text = conflict.slot.displayText(), fontWeight = FontWeight.SemiBold)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                conflict.candidates.forEach { candidate ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = selections[conflict.slot] == candidate,
                                            onClick = {
                                                selections = selections + (conflict.slot to candidate)
                                                if (currentIndex < conflictSlots.lastIndex) currentIndex += 1
                                            },
                                        )
                                        Text(text = candidate.name, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onApply(selections) },
                        enabled = selections.size == state.conflicts.size,
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
