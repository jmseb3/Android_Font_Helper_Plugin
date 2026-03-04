package com.wonddak.fonthelper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.wonddak.fonthelper.model.ModuleData
import com.wonddak.fonthelper.util.ModuleFinder
import com.wonddak.fonthelper.widget.FontHelperMain

class FontHelperToolWindow : ToolWindowFactory,
    DumbAware {

    private var moduleList: List<ModuleData> by mutableStateOf(emptyList())
    private var syncListenerRegistered = false

    private fun loadModule(
        project : Project
    ) {
        moduleList = ModuleFinder.findModule(project)
    }

    private fun registerSyncListener(project: Project) {
        if (syncListenerRegistered) return
        syncListenerRegistered = true
        project.messageBus.connect(project).subscribe(
            ProjectDataImportListener.TOPIC,
            object : ProjectDataImportListener {
                override fun onImportFinished(projectPath: String?) {
                    ApplicationManager.getApplication().invokeLater {
                        loadModule(project)
                    }
                }
            }
        )
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        loadModule(project)
        registerSyncListener(project)
        toolWindow.apply {
            addComposePanel {
                Compose17IJSizeBugWorkaround {
                    FontHelperMain(
                        project,
                        moduleList,
                        onRefreshModules = { loadModule(project) }
                    )
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
    setContent {
        content()
    }
}.also { contentManager.addContent(contentManager.factory.createContent(it, displayName, isLockable)) }
