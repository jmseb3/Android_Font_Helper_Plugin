package com.wonddak.fonthelper.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.theme.WidgetTheme
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.GoogleFontsUtil
import com.wonddak.fonthelper.util.IdeFileChooserUtil
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
        Surface(modifier = Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()
            var fontData by rememberSaveable {
                mutableStateOf(
                    FontData(
                        fileName = "",
                        packageName = "",
                        useKotlinPath = false,
                    )
                )
            }
            var showGoogleFontsDialog by remember { mutableStateOf(false) }
            var downloadingGoogleFont by remember { mutableStateOf(false) }
            var googleImportMessage by remember { mutableStateOf<String?>(null) }
            var conflictSelectionState by remember { mutableStateOf<ImportConflictSelectionState?>(null) }
            var googleFileSelectionState by remember { mutableStateOf<GoogleFileSelectionState?>(null) }
            var packageNameTouchedByUser by rememberSaveable { mutableStateOf(false) }
            var autoRenameClassFromGoogle by rememberSaveable { mutableStateOf(true) }
            var selectedTab by rememberSaveable { mutableStateOf(MainContentTab.SETUP) }
            val setupScroll = rememberScrollState()
            val fontsScroll = rememberScrollState()

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

            val clearCacheAction: () -> Unit = {
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
            }

            val importZipAction: () -> Unit = {
                IdeFileChooserUtil.chooseSingleZipFile(project) { pickedPath ->
                    scope.launch {
                        downloadingGoogleFont = true
                        googleImportMessage = "Importing ZIP..."
                        try {
                            val downloaded = GoogleFontsUtil.importFontsFromZip(File(pickedPath))
                            val settings = com.wonddak.fonthelper.setting.FontMatchSettingsService.getInstance().state
                            val analysis = analyzeImportedFonts(downloaded, settings)
                            val autoAssignments = analysis.groups
                                .filterValues { it.size == 1 }
                                .mapValues { it.value.first() }
                            val updated = applyImportedFonts(fontData, autoAssignments)
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
                                    conflicts = conflicts,
                                )
                                googleImportMessage =
                                    "ZIP imported. ${autoAssignments.size} auto-mapped, ${conflicts.size} conflicts need selection."
                            }
                        } catch (e: Exception) {
                            googleImportMessage = "ZIP import failed: ${e.message ?: "Unknown error"}"
                        } finally {
                            downloadingGoogleFont = false
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .padding(bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Font Helper", style = MaterialTheme.typography.h6)
                    Text(
                        text = "Generate Compose FontFamily and copy font resources to your module.",
                        style = MaterialTheme.typography.caption,
                    )
                    InputRow(
                        title = "Font Class Name",
                        text = fontData.fileName,
                        onValueChange = { fontData = fontData.copy(fileName = it) },
                    )

                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        MainContentTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.label) },
                            )
                        }
                    }

                    if (moduleList.isNotEmpty()) {
                        when (selectedTab) {
                            MainContentTab.SETUP -> SetupTabContent(
                                project = project,
                                moduleList = moduleList,
                                fontData = fontData,
                                setupScroll = setupScroll,
                                onPackageTouched = { packageNameTouchedByUser = true },
                                onFontDataChange = { fontData = it },
                                onRefreshModules = onRefreshModules,
                                onAutoDetectPackage = {
                                    scope.launch {
                                        val module = fontData.selectedModule ?: return@launch
                                        val resolvedPackage = PackageNameResolver.resolve(module) ?: return@launch
                                        fontData = fontData.copy(packageName = resolvedPackage)
                                        packageNameTouchedByUser = false
                                    }
                                },
                            )

                            MainContentTab.FONTS -> FontsTabContent(
                                project = project,
                                fontData = fontData,
                                fontsScroll = fontsScroll,
                                downloadingGoogleFont = downloadingGoogleFont,
                                googleImportMessage = googleImportMessage,
                                onFontDataChange = { fontData = it },
                                onClearCache = clearCacheAction,
                                onImportZip = importZipAction,
                                onOpenGoogleFonts = { showGoogleFontsDialog = true },
                            )
                        }
                    } else {
                        NoModuleContent(onRefreshModules = onRefreshModules)
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
                                    val settings = com.wonddak.fonthelper.setting.FontMatchSettingsService.getInstance().state
                                    val refs = GoogleFontsUtil.fetchFamilyFontFileRefs(family)
                                    val slotCandidates = refs
                                        .mapNotNull { ref ->
                                            val shortName = ref.filename.substringAfterLast('/').substringAfterLast('\\')
                                            val match = settings.checkType(shortName.lowercase()) ?: return@mapNotNull null
                                            FontSlotKey(isItalic = match.first, weight = match.second) to ref
                                        }
                                        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
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
                                                refs = autoSelectedBySlot.values.distinctBy { it.url },
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
                                                conflictCandidatesBySlot = conflictCandidatesBySlot,
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
                        },
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
                                        refs = selectedRefs,
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
                        },
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
                        },
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    elevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { fontData = fontData.clearAllFont() },
                            enabled = fontData.totalFontPath.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(" Clear All")
                        }
                        Button(
                            onClick = {
                                FontUtil.makeFontFamilyFile(
                                    project = project,
                                    fontData = fontData,
                                )
                            },
                            enabled = moduleList.isNotEmpty() && fontData.enabledOk(),
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
                                onClick = {},
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
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

internal enum class MainContentTab(val label: String) {
    SETUP("Setup"),
    FONTS("Fonts"),
}

internal fun String.toSafeClassName(): String {
    val parts = trim()
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val merged = parts.joinToString("") { part ->
        part.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
    return if (merged.firstOrNull()?.isDigit() == true) "Font$merged" else merged
}
