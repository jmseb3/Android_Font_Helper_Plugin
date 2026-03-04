package com.wonddak.fonthelper.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.intellij.openapi.project.Project
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.setting.FontMatchSettingsService
import com.wonddak.fonthelper.setting.FontMatchSettingsState
import com.wonddak.fonthelper.theme.WidgetTheme
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.GoogleFontsUtil
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FontHelperMain(
    project: Project,
    moduleList: List<ModuleData>,
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
                        val (updated, matched) = applyImportedFonts(fontData, downloaded, settings)
                        fontData = updated
                        googleImportMessage = if (matched == 0) {
                            "ZIP imported, but no variants matched current keywords."
                        } else {
                            "ZIP imported. $matched variants mapped."
                        }
                    } catch (e: Exception) {
                        googleImportMessage = "ZIP import failed: ${e.message ?: "Unknown error"}"
                    } finally {
                        downloadingGoogleFont = false
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
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
                InputRow(
                    title = "Package Name",
                    text = fontData.packageName,
                    onValueChange = {
                        fontData = fontData.copy(packageName = it)
                    }
                )
                if (moduleList.isNotEmpty()) {
                    LaunchedEffect(moduleList) {
                        fontData = fontData.copy(selectedModule = moduleList.first())
                    }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                Button(
                                    onClick = { showDownloadedZipPicker = true },
                                    enabled = !downloadingGoogleFont
                                ) {
                                    Text("Import Downloaded ZIP")
                                }
                                Button(
                                    onClick = { showGoogleFontsDialog = true },
                                    enabled = !downloadingGoogleFont
                                ) {
                                    Text("Import from Google Fonts")
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
                                Button(
                                    onClick = {
                                        fontData = fontData.clearAllFont()
                                    },
                                    enabled = fontData.totalFontPath.isNotEmpty()
                                ) {
                                    Text("Clear All Fonts")
                                }
                            }
                        }
                    }

                    Divider()
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            FontUtil.makeFontFamilyFile(
                                project = project,
                                fontData = fontData
                            )
                        },
                        enabled = fontData.enabledOk()
                    ) {
                        Text("Generate FontFamily")
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = 1.dp
                    ) {
                        Text(
                            text = "No module found. Wait for project sync to finish and reopen Font Helper.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }

                if (showGoogleFontsDialog) {
                    GoogleFontsImportDialog(
                        onDismiss = { showGoogleFontsDialog = false },
                        onImport = { family ->
                            showGoogleFontsDialog = false
                            scope.launch {
                                downloadingGoogleFont = true
                                googleImportMessage = "Downloading \"$family\"..."
                                try {
                                    val downloaded = GoogleFontsUtil.downloadFamilyFonts(family)
                                    val settings = FontMatchSettingsService.getInstance().state
                                    val (updated, matched) = applyImportedFonts(fontData, downloaded, settings)
                                    fontData = updated
                                    googleImportMessage = if (matched == 0) {
                                        "\"$family\" downloaded, but no variants matched current keywords."
                                    } else {
                                        "\"$family\" imported. $matched variants mapped."
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

            }
        }
    }
}

private fun applyImportedFonts(
    current: FontData,
    files: List<File>,
    settings: FontMatchSettingsState
): Pair<FontData, Int> {
    var updated = current
    var matched = 0

    files.forEach { file ->
        val fileName = file.name.lowercase()
        settings.checkType(fileName)?.let { (isItalic, weight) ->
            updated = if (isItalic) {
                updated.updateItalicFont(weight, file.absolutePath)
            } else {
                updated.updateNormalFont(weight, file.absolutePath)
            }
            matched += 1
        }
    }
    return updated to matched
}
