package com.wonddak.fonthelper.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import java.util.Locale

object IdeFileChooserUtil {

    fun chooseSingleZipFile(
        project: Project,
        onChosen: (path: String) -> Unit
    ) {
        chooseSingleFile(
            project = project,
            title = "Select Downloaded Google Fonts ZIP",
            allowedExtensions = setOf("zip"),
            onChosen = onChosen
        )
    }

    fun chooseSingleFontFile(
        project: Project,
        onChosen: (path: String) -> Unit
    ) {
        chooseSingleFile(
            project = project,
            title = "Select Font File",
            allowedExtensions = setOf("ttf", "otf"),
            onChosen = onChosen
        )
    }

    private fun chooseSingleFile(
        project: Project,
        title: String,
        allowedExtensions: Set<String>,
        onChosen: (path: String) -> Unit
    ) {
        val normalizedExtensions = allowedExtensions.map { it.lowercase(Locale.ROOT) }.toSet()
        val descriptor = FileChooserDescriptor(
            true,
            false,
            false,
            false,
            false,
            false
        ).apply {
            this.title = title
            withFileFilter { virtualFile ->
                if (virtualFile.isDirectory) return@withFileFilter true
                val ext = virtualFile.extension?.lowercase(Locale.ROOT).orEmpty()
                ext in normalizedExtensions
            }
        }

        FileChooser.chooseFiles(descriptor, project, null) { selectedFiles ->
            val selected = selectedFiles.firstOrNull() ?: return@chooseFiles
            val selectedPath = selected.path.trim()
            val extensionMatched = normalizedExtensions.any { extension ->
                selectedPath.lowercase(Locale.ROOT).endsWith(".$extension")
            }

            if (selectedPath.isNotEmpty() && extensionMatched) {
                onChosen(selectedPath)
            }
        }
    }
}
