package com.wonddak.fonthelper.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.intellij.openapi.project.Project
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.setting.FontMatchSettingsService
import com.wonddak.fonthelper.setting.FontMatchSettingsState
import com.wonddak.fonthelper.theme.WidgetTheme
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.GoogleFontFileRef
import com.wonddak.fonthelper.util.GoogleFontsUtil
import com.wonddak.fonthelper.util.PackageNameResolver
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FontHelperMain(
    project: Project,
    moduleList: List<ModuleData>,
    onRefreshModules: () -> Unit,
) {
    WidgetTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            val scope = rememberCoroutineScope()
            var fontData by rememberSaveable {
                mutableStateOf(
                    FontData(
                        fileName = "",
                        packageName = "",
                        useKotlinPath = false
                    )
                )
            }
            var showGoogleFontsDialog by remember { mutableStateOf(false) }
            var showDownloadedZipPicker by remember { mutableStateOf(false) }
            var downloadingGoogleFont by remember { mutableStateOf(false) }
            var googleImportMessage by remember { mutableStateOf<String?>(null) }
            var conflictSelectionState by remember { mutableStateOf<ImportConflictSelectionState?>(null) }
            var googleFileSelectionState by remember { mutableStateOf<GoogleFileSelectionState?>(null) }
            var packageNameTouchedByUser by rememberSaveable { mutableStateOf(false) }
            var autoRenameClassFromGoogle by rememberSaveable { mutableStateOf(true) }
            var selectedTab by rememberSaveable { mutableStateOf(MainContentTab.SETUP) }
            val setupScroll = rememberScrollState()
            val fontsScroll = rememberScrollState()

            FilePicker(
                show = showDownloadedZipPicker,
                fileExtensions = listOf("zip")
            ) { platformFile ->
                showDownloadedZipPicker = false
                val pickedPath = platformFile?.path?.normalizeDroppedPath().orEmpty()
                if (pickedPath.isBlank()) return@FilePicker

                scope.launch {
                    downloadingGoogleFont = true
                    googleImportMessage = "Importing ZIP..."
                    try {
                        val downloaded = GoogleFontsUtil.importFontsFromZip(File(pickedPath))
                        val settings = FontMatchSettingsService.getInstance().state
                        val analysis = analyzeImportedFonts(downloaded, settings)
                        val autoAssignments = analysis.groups
                            .filterValues { it.size == 1 }
                            .mapValues { it.value.first() }
                        var updated = applyImportedFonts(fontData, autoAssignments)
                        fontData = updated

                        val conflicts = analysis.groups
                            .filterValues { it.size > 1 }
                            .entries
                            .sortedBy { it.key.displayText() }
                            .map { (slot, files) -> ImportConflict(slot, files.sortedBy { it.name.lowercase() }) }

                        if (analysis.matchedCount == 0) {
                            googleImportMessage = "ZIP imported, but no variants matched current keywords."
                        } else if (conflicts.isEmpty()) {
                            googleImportMessage = "ZIP imported. ${analysis.matchedCount} variants mapped."
                        } else {
                            conflictSelectionState = ImportConflictSelectionState(
                                sourceLabel = "ZIP",
                                currentFontData = updated,
                                conflicts = conflicts
                            )
                            googleImportMessage = "ZIP imported. ${autoAssignments.size} auto-mapped, ${conflicts.size} conflicts need selection."
                        }
                    } catch (e: Exception) {
                        googleImportMessage = "ZIP import failed: ${e.message ?: "Unknown error"}"
                    } finally {
                        downloadingGoogleFont = false
                    }
                }
            }

            if (moduleList.isNotEmpty()) {
                LaunchedEffect(moduleList) {
                    val selected = fontData.selectedModule
                    val selectedStillExists = selected != null && moduleList.any {
                        it.path == selected.path && it.isCMP == selected.isCMP
                    }
                    if (!selectedStillExists) {
                        fontData = fontData.copy(selectedModule = moduleList.first())
                    }
                }
                LaunchedEffect(fontData.selectedModule) {
                    val module = fontData.selectedModule ?: return@LaunchedEffect
                    if (packageNameTouchedByUser && fontData.packageName.isNotBlank()) return@LaunchedEffect
                    val resolvedPackage = PackageNameResolver.resolve(module) ?: return@LaunchedEffect
                    fontData = fontData.copy(packageName = resolvedPackage)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .padding(bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Font Helper",
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = "Generate Compose FontFamily and copy font resources to your module.",
                        style = MaterialTheme.typography.caption
                    )
                    InputRow(
                        title = "Font Class Name",
                        text = fontData.fileName,
                        onValueChange = {
                            fontData = fontData.copy(fileName = it)
                        }
                    )

                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        MainContentTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.label) }
                            )
                        }
                    }

                    if (moduleList.isNotEmpty()) {
                        when (selectedTab) {
                            MainContentTab.SETUP -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(setupScroll),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    InputRow(
                                        title = "Package Name",
                                        text = fontData.packageName,
                                        onValueChange = {
                                            packageNameTouchedByUser = true
                                            fontData = fontData.copy(packageName = it)
                                        }
                                    )
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        elevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Project Settings",
                                                style = MaterialTheme.typography.subtitle1
                                            )
                                            ModuleSpinner(
                                                moduleList = moduleList,
                                                selectedModule = fontData.selectedModule,
                                                updateModule = { module ->
                                                    fontData = fontData.copy(selectedModule = module)
                                                }
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                            ) {
                                                TextButton(
                                                    onClick = onRefreshModules
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                                    Text(" Refresh Modules")
                                                }
                                                TextButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val module = fontData.selectedModule ?: return@launch
                                                            val resolvedPackage = PackageNameResolver.resolve(module) ?: return@launch
                                                            fontData = fontData.copy(packageName = resolvedPackage)
                                                            packageNameTouchedByUser = false
                                                        }
                                                    }
                                                ) {
                                                    Text("Auto Detect Package")
                                                }
                                            }

                                            if (fontData.selectedModule?.isCMP == false) {
                                                LabelContent(
                                                    "Use Kotlin Path"
                                                ) {
                                                    Checkbox(
                                                        checked = fontData.useKotlinPath,
                                                        onCheckedChange = {
                                                            fontData = fontData.copy(useKotlinPath = it)
                                                        }
                                                    )
                                                }
                                            }
                                            LabelContent("Class Path Preview") {
                                                val basePath = project.basePath
                                                val previewPath = if (basePath.isNullOrBlank()) {
                                                    fontData.previewClassPath()
                                                } else {
                                                    fontData.previewClassPath().replace(basePath, ".")
                                                }
                                                Text(
                                                    text = previewPath,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                                                            shape = MaterialTheme.shapes.small
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    style = MaterialTheme.typography.caption
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            MainContentTab.FONTS -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(fontsScroll),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        elevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Font Files",
                                                style = MaterialTheme.typography.subtitle1
                                            )
                                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                val narrow = maxWidth < 820.dp
                                                if (narrow) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    downloadingGoogleFont = true
                                                                    googleImportMessage = "Clearing downloaded font cache..."
                                                                    try {
                                                                        val deleted = GoogleFontsUtil.clearDownloadedCache()
                                                                        fontData = removeManagedDownloadedPaths(fontData)
                                                                        googleImportMessage = "Download cache cleared. $deleted items deleted."
                                                                    } catch (e: Exception) {
                                                                        googleImportMessage = "Failed to clear cache: ${e.message ?: "Unknown error"}"
                                                                    } finally {
                                                                        downloadingGoogleFont = false
                                                                    }
                                                                }
                                                            },
                                                            enabled = !downloadingGoogleFont,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = null)
                                                            Text(" Clear Cache")
                                                        }
                                                        Button(
                                                            onClick = { showDownloadedZipPicker = true },
                                                            enabled = !downloadingGoogleFont,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(Icons.Default.Build, contentDescription = null)
                                                            Text(" Import ZIP")
                                                        }
                                                        Button(
                                                            onClick = { showGoogleFontsDialog = true },
                                                            enabled = !downloadingGoogleFont,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(Icons.Default.Build, contentDescription = null)
                                                            Text(" Google Fonts (Beta)")
                                                        }
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    downloadingGoogleFont = true
                                                                    googleImportMessage = "Clearing downloaded font cache..."
                                                                    try {
                                                                        val deleted = GoogleFontsUtil.clearDownloadedCache()
                                                                        fontData = removeManagedDownloadedPaths(fontData)
                                                                        googleImportMessage = "Download cache cleared. $deleted items deleted."
                                                                    } catch (e: Exception) {
                                                                        googleImportMessage = "Failed to clear cache: ${e.message ?: "Unknown error"}"
                                                                    } finally {
                                                                        downloadingGoogleFont = false
                                                                    }
                                                                }
                                                            },
                                                            enabled = !downloadingGoogleFont
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = null)
                                                            Text(" Clear Cache")
                                                        }
                                                        Button(
                                                            onClick = { showDownloadedZipPicker = true },
                                                            enabled = !downloadingGoogleFont
                                                        ) {
                                                            Icon(Icons.Default.Build, contentDescription = null)
                                                            Text(" Import ZIP")
                                                        }
                                                        Button(
                                                            onClick = { showGoogleFontsDialog = true },
                                                            enabled = !downloadingGoogleFont
                                                        ) {
                                                            Icon(Icons.Default.Build, contentDescription = null)
                                                            Text(" Google Fonts (Beta)")
                                                        }
                                                    }
                                                }
                                            }
                                            if (googleImportMessage != null) {
                                                Text(
                                                    text = googleImportMessage.orEmpty(),
                                                    style = MaterialTheme.typography.caption
                                                )
                                            }
                                            DragDropFiles(
                                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                                updateNormalFontList = { index, path ->
                                                    fontData = fontData.updateNormalFont(index, path)
                                                },
                                                updateItalicFontList = { index, path ->
                                                    fontData = fontData.updateItalicFont(index, path)
                                                }
                                            )
                                            FontTable(
                                                normalFontList = fontData.normalFontPath,
                                                italicFontList = fontData.italicFontPath,
                                                updateNormalFontList = { index, path ->
                                                    fontData = fontData.updateNormalFont(index, path)
                                                },
                                                updateItalicFontList = { index, path ->
                                                    fontData = fontData.updateItalicFont(index, path)
                                                }
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = "Google Fonts import is experimental.",
                                                    style = MaterialTheme.typography.caption
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No module found. If build/sync just finished, click refresh.",
                                    style = MaterialTheme.typography.body2
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = onRefreshModules) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Text(" Refresh")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showGoogleFontsDialog) {
                    GoogleFontsImportDialog(
                        autoRenameClassName = autoRenameClassFromGoogle,
                        onAutoRenameClassNameChange = { autoRenameClassFromGoogle = it },
                        onDismiss = { showGoogleFontsDialog = false },
                        onImport = { family ->
                            showGoogleFontsDialog = false
                            scope.launch {
                                downloadingGoogleFont = true
                                googleImportMessage = "Loading \"$family\" file list..."
                                try {
                                    val settings = FontMatchSettingsService.getInstance().state
                                    val refs = GoogleFontsUtil.fetchFamilyFontFileRefs(family)
                                    val slotCandidates = refs
                                        .mapNotNull { ref ->
                                            val shortName = ref.filename.substringAfterLast('/').substringAfterLast('\\')
                                            val match = settings.checkType(shortName.lowercase()) ?: return@mapNotNull null
                                            FontSlotKey(isItalic = match.first, weight = match.second) to ref
                                        }
                                        .groupBy(
                                            keySelector = { it.first },
                                            valueTransform = { it.second }
                                        )
                                        .toSortedMap(compareBy<FontSlotKey> { it.isItalic }.thenBy { it.weight })

                                    if (slotCandidates.isEmpty()) {
                                        googleImportMessage =
                                            "\"$family\" list loaded, but no variants matched current keywords."
                                    } else {
                                        val normalizedBySlot = slotCandidates.mapValues { (_, list) ->
                                            list.distinctBy { it.url }.sortedBy { it.filename.lowercase() }
                                        }
                                        val autoSelectedBySlot = normalizedBySlot
                                            .filterValues { it.size == 1 }
                                            .mapValues { (_, refsBySlot) -> refsBySlot.first() }
                                        val conflictCandidatesBySlot = normalizedBySlot
                                            .filterValues { it.size > 1 }

                                        if (conflictCandidatesBySlot.isEmpty()) {
                                            val downloadedByUrl = GoogleFontsUtil.downloadFamilyFontRefs(
                                                family = family,
                                                refs = autoSelectedBySlot.values.distinctBy { it.url }
                                            )
                                            val assignments = autoSelectedBySlot.mapNotNull { (slot, ref) ->
                                                downloadedByUrl[ref.url]?.let { slot to it }
                                            }.toMap()
                                            val updated = applyImportedFonts(fontData, assignments)
                                            fontData = if (autoRenameClassFromGoogle && assignments.isNotEmpty()) {
                                                updated.copy(fileName = family.toSafeClassName())
                                            } else {
                                                updated
                                            }
                                            googleImportMessage =
                                                "\"$family\" imported. ${assignments.size} variants mapped."
                                        } else {
                                            googleFileSelectionState = GoogleFileSelectionState(
                                                family = family,
                                                autoSelectedBySlot = autoSelectedBySlot,
                                                conflictCandidatesBySlot = conflictCandidatesBySlot
                                            )
                                            googleImportMessage =
                                                "\"$family\" list loaded. ${autoSelectedBySlot.size} auto-selected, choose ${conflictCandidatesBySlot.size} conflict slots."
                                        }
                                    }
                                } catch (e: Exception) {
                                    googleImportMessage = "Google Fonts import failed: ${e.message ?: "Unknown error"}"
                                } finally {
                                    downloadingGoogleFont = false
                                }
                            }
                        }
                    )
                }

                googleFileSelectionState?.let { state ->
                    GoogleFileSelectionDialog(
                        state = state,
                        onDismiss = { googleFileSelectionState = null },
                        onApply = { selectedBySlot ->
                            googleFileSelectionState = null
                            scope.launch {
                                downloadingGoogleFont = true
                                googleImportMessage = "Downloading selected files from \"${state.family}\"..."
                                try {
                                    val allSelections = state.autoSelectedBySlot + selectedBySlot
                                    val selectedRefs = allSelections.values.distinctBy { it.url }
                                    val downloadedByUrl = GoogleFontsUtil.downloadFamilyFontRefs(
                                        family = state.family,
                                        refs = selectedRefs
                                    )
                                    val assignments = allSelections.mapNotNull { (slot, ref) ->
                                        downloadedByUrl[ref.url]?.let { slot to it }
                                    }.toMap()

                                    val updated = applyImportedFonts(fontData, assignments)
                                    fontData = if (autoRenameClassFromGoogle && assignments.isNotEmpty()) {
                                        updated.copy(fileName = state.family.toSafeClassName())
                                    } else {
                                        updated
                                    }
                                    googleImportMessage =
                                        "\"${state.family}\" imported. ${assignments.size} variants mapped."
                                } catch (e: Exception) {
                                    googleImportMessage =
                                        "Google Fonts download failed: ${e.message ?: "Unknown error"}"
                                } finally {
                                    downloadingGoogleFont = false
                                }
                            }
                        }
                    )
                }

                conflictSelectionState?.let { state ->
                    ImportConflictDialog(
                        state = state,
                        onDismiss = { conflictSelectionState = null },
                        onApply = { selections ->
                            fontData = applyImportedFonts(state.currentFontData, selections)
                            googleImportMessage =
                                "${state.sourceLabel} conflict selections applied. ${selections.size} slots updated."
                            conflictSelectionState = null
                        }
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                fontData = fontData.clearAllFont()
                            },
                            enabled = fontData.totalFontPath.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(" Clear All")
                        }
                        Button(
                            onClick = {
                                FontUtil.makeFontFamilyFile(
                                    project = project,
                                    fontData = fontData
                                )
                            },
                            enabled = moduleList.isNotEmpty() && fontData.enabledOk()
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Text(" Generate")
                        }
                    }
                }

                if (downloadingGoogleFont) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.surface.copy(alpha = 0.72f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Processing fonts...", style = MaterialTheme.typography.body2)
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun GoogleFileSelectionDialog(
    state: GoogleFileSelectionState,
    onDismiss: () -> Unit,
    onApply: (Map<FontSlotKey, GoogleFontFileRef>) -> Unit
) {
    val conflictSlots = remember(state) { state.conflictCandidatesBySlot.keys.toList() }
    val initial = remember(state) { state.conflictCandidatesBySlot.mapValues { (_, refs) -> refs.first() } }
    var selections by remember(state) { mutableStateOf(initial) }
    var currentIndex by remember(state) { mutableStateOf(0) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Select Google Fonts Files",
        state = rememberDialogState(size = DpSize(760.dp, 520.dp))
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
                    text = "\"${state.family}\" has duplicate matches. Select one file only for conflict slots.",
                    style = MaterialTheme.typography.body2
                )
                Text(
                    text = "${state.autoSelectedBySlot.size} slots were auto-selected.",
                    style = MaterialTheme.typography.caption
                )
                if (conflictSlots.isNotEmpty()) {
                    val slot = conflictSlots[currentIndex]
                    val refs = state.conflictCandidatesBySlot[slot].orEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (currentIndex > 0) currentIndex -= 1 },
                            enabled = currentIndex > 0
                        ) { Text("Prev") }
                        Text(
                            text = "${currentIndex + 1} / ${conflictSlots.size}",
                            style = MaterialTheme.typography.caption
                        )
                        TextButton(
                            onClick = { if (currentIndex < conflictSlots.lastIndex) currentIndex += 1 },
                            enabled = currentIndex < conflictSlots.lastIndex
                        ) { Text("Next") }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = slot.displayText(),
                                fontWeight = FontWeight.SemiBold
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                refs.forEach { ref ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selections[slot] == ref,
                                            onClick = {
                                                selections = selections + (slot to ref)
                                                if (currentIndex < conflictSlots.lastIndex) {
                                                    currentIndex += 1
                                                }
                                            }
                                        )
                                        Text(
                                            text = ref.filename.substringAfterLast('/').substringAfterLast('\\'),
                                            modifier = Modifier.weight(1f)
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
                        onClick = { onApply(selections) },
                        enabled = selections.size == state.conflictCandidatesBySlot.size
                    ) {
                        Text("Download Selected")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportConflictDialog(
    state: ImportConflictSelectionState,
    onDismiss: () -> Unit,
    onApply: (Map<FontSlotKey, File>) -> Unit
) {
    val conflictSlots = remember(state) { state.conflicts.map { it.slot } }
    val initialSelection = remember(state) { state.conflicts.associate { it.slot to it.candidates.first() } }
    var selections by remember(state) { mutableStateOf(initialSelection) }
    var currentIndex by remember(state) { mutableStateOf(0) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Resolve Font Type Conflicts",
        state = rememberDialogState(size = DpSize(720.dp, 520.dp))
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
                    text = "Multiple files matched the same type. Select one file per slot.",
                    style = MaterialTheme.typography.body2
                )
                if (conflictSlots.isNotEmpty()) {
                    val slot = conflictSlots[currentIndex]
                    val conflict = state.conflicts.first { it.slot == slot }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (currentIndex > 0) currentIndex -= 1 },
                            enabled = currentIndex > 0
                        ) { Text("Prev") }
                        Text(
                            text = "${currentIndex + 1} / ${conflictSlots.size}",
                            style = MaterialTheme.typography.caption
                        )
                        TextButton(
                            onClick = { if (currentIndex < conflictSlots.lastIndex) currentIndex += 1 },
                            enabled = currentIndex < conflictSlots.lastIndex
                        ) { Text("Next") }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = conflict.slot.displayText(),
                                fontWeight = FontWeight.SemiBold
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                conflict.candidates.forEach { candidate ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selections[conflict.slot] == candidate,
                                            onClick = {
                                                selections = selections + (conflict.slot to candidate)
                                                if (currentIndex < conflictSlots.lastIndex) {
                                                    currentIndex += 1
                                                }
                                            }
                                        )
                                        Text(
                                            text = candidate.name,
                                            modifier = Modifier.weight(1f)
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
                        onClick = { onApply(selections) },
                        enabled = selections.size == state.conflicts.size
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

private fun analyzeImportedFonts(
    files: List<File>,
    settings: FontMatchSettingsState
): ImportAnalysis {
    val groups = linkedMapOf<FontSlotKey, MutableList<File>>()
    var matchedCount = 0
    files.forEach { file ->
        val fileName = file.name.lowercase()
        val match = settings.checkType(fileName) ?: return@forEach
        val key = FontSlotKey(isItalic = match.first, weight = match.second)
        groups.getOrPut(key) { mutableListOf() }.add(file)
        matchedCount += 1
    }
    return ImportAnalysis(
        groups = groups.mapValues { it.value.toList() },
        matchedCount = matchedCount
    )
}

private fun applyImportedFonts(
    current: FontData,
    assignments: Map<FontSlotKey, File>
): FontData {
    var updated = current
    assignments.forEach { (slot, file) ->
        updated = if (slot.isItalic) {
            updated.updateItalicFont(slot.weight, file.absolutePath)
        } else {
            updated.updateNormalFont(slot.weight, file.absolutePath)
        }
    }
    return updated
}

private fun removeManagedDownloadedPaths(current: FontData): FontData {
    val cleanedNormal = current.normalFontPath.map { font ->
        if (font != null && GoogleFontsUtil.isManagedDownloadedFile(font.path)) null else font
    }
    val cleanedItalic = current.italicFontPath.map { font ->
        if (font != null && GoogleFontsUtil.isManagedDownloadedFile(font.path)) null else font
    }
    return current.copy(
        normalFontPath = cleanedNormal,
        italicFontPath = cleanedItalic
    )
}

private data class GoogleFileSelectionState(
    val family: String,
    val autoSelectedBySlot: Map<FontSlotKey, GoogleFontFileRef>,
    val conflictCandidatesBySlot: Map<FontSlotKey, List<GoogleFontFileRef>>
)

private data class ImportAnalysis(
    val groups: Map<FontSlotKey, List<File>>,
    val matchedCount: Int
)

private data class ImportConflictSelectionState(
    val sourceLabel: String,
    val currentFontData: FontData,
    val conflicts: List<ImportConflict>
)

private data class ImportConflict(
    val slot: FontSlotKey,
    val candidates: List<File>
)

private data class FontSlotKey(
    val isItalic: Boolean,
    val weight: Int
) {
    fun displayText(): String {
        val style = if (isItalic) "Italic" else "Normal"
        return "${FontUtil.getWeightTextByIndex(weight)} $style"
    }
}

private enum class MainContentTab(val label: String) {
    SETUP("Setup"),
    FONTS("Fonts")
}

private fun String.toSafeClassName(): String {
    val parts = trim()
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val merged = parts.joinToString("") { part ->
        part.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
    return if (merged.firstOrNull()?.isDigit() == true) "Font$merged" else merged
}
