package com.wonddak.fonthelper.widget

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
    WidgetTheme(darkTheme = true) {
        Surface() {
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
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
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
                    LaunchedEffect(true) {
                        fontData = fontData.copy(selectedModule = moduleList.first())
                    }
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
                        Text(fontData.previewClassPath().replace(project.basePath!!, "."))
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
                    Button(
                        modifier = Modifier.fillMaxWidth(0.3f)
                            .align(Alignment.End),
                        onClick = {
                            fontData = fontData.clearAllFont()
                        },
                        enabled = fontData.totalFontPath.isNotEmpty()
                    ) {
                        Text("Clear All Font")
                    }
                    Divider()
                    Button(
                        modifier = Modifier.fillMaxWidth(0.7f)
                            .align(Alignment.Start),
                        onClick = {
                            FontUtil.makeFontFamilyFile(
                                project = project,
                                fontData = fontData
                            )
                        },
                        enabled = fontData.enabledOk()
                    ) {
                        Text("Add")
                    }
                } else {
                    Text("Can't find module in this project\n please wait finish Sync And re-open FontHelper")
                }

            }
        }
    }
}