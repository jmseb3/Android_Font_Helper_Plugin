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

@Composable
fun FontHelperMain(
    project: Project,
    moduleList: List<ModuleData>,
) {
    WidgetTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            var fontData by rememberSaveable {
                mutableStateOf(
                    FontData(
                        fileName = "",
                        packageName = "",
                        useKotlinPath = false
                    )
                )
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

            }
        }
    }
}
