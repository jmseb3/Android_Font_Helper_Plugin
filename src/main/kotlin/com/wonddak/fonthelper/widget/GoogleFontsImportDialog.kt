package com.wonddak.fonthelper.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.wonddak.fonthelper.util.GoogleFontFamily
import com.wonddak.fonthelper.util.GoogleFontsUtil
import java.util.Locale

@Composable
fun GoogleFontsImportDialog(
    onDismiss: () -> Unit,
    onImport: (family: String) -> Unit,
) {
    var families by remember { mutableStateOf<List<GoogleFontFamily>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedFamily by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        try {
            families = GoogleFontsUtil.fetchFamilies()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load Google Fonts."
        } finally {
            loading = false
        }
    }

    val filtered = if (query.isBlank()) {
        families
    } else {
        val normalizedQuery = query.normalizeForSearch()
        families.filter { family ->
            family.family.normalizeForSearch().contains(normalizedQuery) ||
                family.category.orEmpty().normalizeForSearch().contains(normalizedQuery)
        }
    }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Import from Google Fonts",
        state = rememberDialogState(size = DpSize(640.dp, 560.dp))
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Search and select a family from Google Fonts.",
                    style = MaterialTheme.typography.body2
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search family") }
                )
                if (!loading && errorMessage == null) {
                    Text(
                        text = "${filtered.size} results",
                        style = MaterialTheme.typography.caption
                    )
                }

                when {
                    loading -> {
                        Text("Loading Google Fonts list...")
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colors.error
                        )
                    }
                    else -> {
                        if (filtered.isEmpty()) {
                            Text("No fonts found for \"$query\"")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 240.dp, max = 360.dp)
                            ) {
                                items(filtered) { family ->
                                    val selected = selectedFamily == family.family
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFamily = family.family }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = family.family,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        Text(
                                            text = family.category ?: "",
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            selectedFamily?.let(onImport)
                        },
                        enabled = !loading && selectedFamily != null
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

private fun String.normalizeForSearch(): String {
    return lowercase(Locale.getDefault())
        .replace(" ", "")
        .replace("-", "")
        .trim()
}
