package com.wonddak.fonthelper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.theme.WidgetTheme
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.ModuleFinder
import com.wonddak.fonthelper.widget.FontTable
import com.wonddak.fonthelper.widget.InputRow
import com.wonddak.fonthelper.widget.LabelContent
import com.wonddak.fonthelper.widget.ModuleSpinner

class FontHelperToolWindow : ToolWindowFactory,
    DumbAware {

    private var moduleList: List<ModuleData> = emptyList()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        System.setProperty("compose.swing.render.on.graphics", "true")
        moduleList = ModuleFinder.findModule(project)
        moduleList.forEach {
            println("[KK] $it")
        }
        toolWindow.apply {
            addComposePanel {
                Compose17IJSizeBugWorkaround {
                    WidgetTheme(darkTheme = true) {
                        Surface() {
                            var fontData by rememberSaveable {
                                mutableStateOf(
                                    FontData(
                                        fileName = "Roboto",
                                        packageName = "dev.wonddak.capturableExample",
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
                                        Text(fontData.previewClassPath().replace(project.basePath!!, "."))
                                    }
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
                                } else {
                                    Text("Can't find module in this project\n please wait finish Sync..")
                                }
                                Button(
                                    onClick = {
                                        FontUtil.makeFontFamilyFile(project, fontData)
                                    },
                                    enabled = fontData.enabledOk()
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Workaround until the issue with Compose 1.7 + fillMax__ + IntelliJ Panels is fixed:
     * https://youtrack.jetbrains.com/issue/CMP-5856
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Compose17IJSizeBugWorkaround(content: @Composable () -> Unit) {
        with(LocalDensity.current) {
            Box(modifier = Modifier.requiredSize(LocalWindowInfo.current.containerSize.toSize().toDpSize())) {
                content()
            }
        }
    }

    companion object {
        const val ID = "FontHelper"
    }
}

private fun ToolWindow.addComposePanel(
    displayName: String = "",
    isLockable: Boolean = true,
    content: @Composable ComposePanel.() -> Unit,
) = ComposePanel().apply {
    setBounds(x = 0, y = 0, width = 1000, height = 800)
    setContent {
        content()
    }
}.also { contentManager.addContent(contentManager.factory.createContent(it, displayName, isLockable)) }