package com.wonddak.fonthelper

import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.theme.WidgetTheme
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.ModuleFinder
import com.wonddak.fonthelper.widget.FontTable
import com.wonddak.fonthelper.widget.InputRow
import com.wonddak.fonthelper.widget.LabelContent
import com.wonddak.fonthelper.widget.ModuleSpinner
import javax.swing.*


class FontHelperDialog(
    private val project: Project
) : DialogWrapper(project) {

    private var moduleList: List<ModuleData> = emptyList()

    private var fontDataResult : FontData? = null

    init {
        title = "Font Helper"
        moduleList = ModuleFinder.findModule(project)
        moduleList.forEach {
            println("[KK] $it")
        }
        init()
    }


    /**
     * make Dialog UI
     */
    override fun createCenterPanel(): JComponent {
        val width = 800
        val height = 1100
        return ComposePanel().apply {
            setBounds(0, 0, width, height)
            setContent {
                WidgetTheme(darkTheme = true) {
                    Surface() {
                        var fontData by rememberSaveable {
                            mutableStateOf(FontData(
                                fileName = "Roboto",
                                packageName = "com.example.myapplication",
                                useKotlinPath = false
                            ))
                        }
                        LaunchedEffect(fontData) {
                            fontDataResult = fontData
                        }
                        Column(
                            modifier = Modifier
                                .width(width.dp)
                                .height(height.dp)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            Text(fontData.toString())
                            InputRow(
                                "Font Class Name",
                                fontData.fileName,
                                onValueChange = {
                                    fontData = fontData.copy(fileName = it)
                                }
                            )
                            InputRow(
                                "Package Name",
                                fontData.packageName,
                                onValueChange = {
                                    fontData = fontData.copy(packageName = it)
                                }
                            )
                            if (moduleList.isNotEmpty()) {
                                LaunchedEffect(true) {
                                    fontData = fontData.copy(selectedModule = moduleList.first())
                                }
                                ModuleSpinner(
                                    moduleList,
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
                                    Text(fontData.previewClassPath().replace(project.basePath!!,""))
                                }
                                FontTable(
                                    normalFontList = fontData.normalFontPath,
                                    italicFontList = fontData.italicFontPath,
                                    updateNormalFontList = { index, path ->
                                        fontData = fontData.updateNormalFont(index, path)
                                    },
                                    updateItalicFontList = {index, path ->
                                        fontData = fontData.updateItalicFont(index, path)
                                    }
                                )
                            } else {
                                Text("Can't find module in this project\n please wait finish Sync..")
                            }
                        }
                    }
                }
            }
        }
    }


    override fun doOKAction() {
        fontDataResult?.let { fontData ->
            if (fontData.fileName.isEmpty()) {
                return
            }

            if (fontData.selectedModule == null) {
                return
            }

            if (fontData.normalFontPath.filterNotNull().isEmpty() && fontData.italicFontPath.filterNotNull().isEmpty()) {
                return
            }

            FontUtil.makeFontFamilyFile(project,fontData)
            super.doOKAction()
        }
    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}