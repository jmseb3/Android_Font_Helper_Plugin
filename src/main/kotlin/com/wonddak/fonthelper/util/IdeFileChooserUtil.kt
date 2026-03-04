package com.wonddak.fonthelper.util

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import java.util.Locale

object IdeFileChooserUtil {
    fun chooseSingleFile(
        project: Project,
        title: String,
        allowedExtensions: Set<String>,
        onChosen: (path: String) -> Unit
    ) {
        val normalizedExtensions = allowedExtensions.map { it.lowercase(Locale.ROOT) }.toSet()
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            withFileFilter { file ->
                val extension = file.extension?.lowercase(Locale.ROOT).orEmpty()
                extension in normalizedExtensions
            }
            this.title = title
            description = "Allowed: ${normalizedExtensions.joinToString(", ").uppercase(Locale.ROOT)}"
        }

        FileChooser.chooseFile(descriptor, project, null) { virtualFile ->
            val path = virtualFile.path.trim()
            if (path.isNotEmpty()) {
                onChosen(path)
            }
        }
    }
}
