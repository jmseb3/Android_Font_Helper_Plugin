package com.wonddak.fonthelper.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ScrollState
import com.intellij.openapi.project.Project
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.theme.icon.CloudDownload
import com.wonddak.fonthelper.theme.icon.FolderZip
import com.wonddak.fonthelper.theme.icon.FontHelperIcons

@Composable
internal fun SetupTabContent(
    project: Project,
    moduleList: List<ModuleData>,
    fontData: FontData,
    setupScroll: ScrollState,
    onPackageTouched: () -> Unit,
    onFontDataChange: (FontData) -> Unit,
    onRefreshModules: () -> Unit,
    onAutoDetectPackage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(setupScroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InputRow(
            title = "Package Name",
            text = fontData.packageName,
            onValueChange = {
                onPackageTouched()
                onFontDataChange(fontData.copy(packageName = it))
            },
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Project Settings", style = MaterialTheme.typography.subtitle1)
                ModuleSpinner(
                    moduleList = moduleList,
                    selectedModule = fontData.selectedModule,
                    updateModule = { module -> onFontDataChange(fontData.copy(selectedModule = module)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onRefreshModules) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(" Refresh Modules")
                    }
                    TextButton(
                        onClick = onAutoDetectPackage,
                    ) {
                        Text("Auto Detect Package")
                    }
                }

                if (fontData.selectedModule?.isCMP == false) {
                    LabelContent("Use Kotlin Path") {
                        Checkbox(
                            checked = fontData.useKotlinPath,
                            onCheckedChange = { onFontDataChange(fontData.copy(useKotlinPath = it)) },
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
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FontsTabContent(
    project: Project,
    fontData: FontData,
    fontsScroll: ScrollState,
    downloadingGoogleFont: Boolean,
    googleImportMessage: String?,
    onFontDataChange: (FontData) -> Unit,
    onClearCache: () -> Unit,
    onImportZip: () -> Unit,
    onOpenGoogleFonts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(fontsScroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Font Files", style = MaterialTheme.typography.subtitle1)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    FontImportActions(
                        narrow = maxWidth < 820.dp,
                        enabled = !downloadingGoogleFont,
                        onClearCache = onClearCache,
                        onImportZip = onImportZip,
                        onOpenGoogleFonts = onOpenGoogleFonts,
                    )
                }
                if (googleImportMessage != null) {
                    Text(text = googleImportMessage, style = MaterialTheme.typography.caption)
                }
                DragDropFiles(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    updateNormalFontList = { index, path ->
                        onFontDataChange(
                            fontData.updateNormalFont(
                                index,
                                path
                            )
                        )
                    },
                    updateItalicFontList = { index, path ->
                        onFontDataChange(
                            fontData.updateItalicFont(
                                index,
                                path
                            )
                        )
                    },
                )
                FontTable(
                    project = project,
                    normalFontList = fontData.normalFontPath,
                    italicFontList = fontData.italicFontPath,
                    updateNormalFontList = { index, path ->
                        onFontDataChange(
                            fontData.updateNormalFont(
                                index,
                                path
                            )
                        )
                    },
                    updateItalicFontList = { index, path ->
                        onFontDataChange(
                            fontData.updateItalicFont(
                                index,
                                path
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FontImportActions(
    narrow: Boolean,
    enabled: Boolean,
    onClearCache: () -> Unit,
    onImportZip: () -> Unit,
    onOpenGoogleFonts: () -> Unit,
) {
    if (narrow) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ActionButton(
                " Clear Cache",
                Icons.Default.Delete,
                enabled,
                Modifier.fillMaxWidth(),
                onClearCache
            )
            ActionButton(
                " Import ZIP",
                FontHelperIcons.FolderZip,
                enabled,
                Modifier.fillMaxWidth(),
                onImportZip
            )
            ActionButton(
                " Google Fonts",
                FontHelperIcons.CloudDownload,
                enabled,
                Modifier.fillMaxWidth(),
                onOpenGoogleFonts
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        ActionButton(" Clear Cache", Icons.Default.Delete, enabled, Modifier, onClearCache)
        ActionButton(" Import ZIP", Icons.Default.AddCircle, enabled, Modifier, onImportZip)
        ActionButton(
            " Google Fonts (Beta)",
            Icons.Default.Search,
            enabled,
            Modifier,
            onOpenGoogleFonts
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(icon, contentDescription = null)
        Text(text)
    }
}

@Composable
internal fun NoModuleContent(
    onRefreshModules: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No module found. If build/sync just finished, click refresh.",
                style = MaterialTheme.typography.body2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onRefreshModules) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(" Refresh")
                }
            }
        }
    }
}
