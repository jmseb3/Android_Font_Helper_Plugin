package com.wonddak.fonthelper.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.model.FontType
import java.io.File

/**
 * Font Helper Object
 */
object FontUtil {
    const val NORMAL = "Normal"
    const val ITALIC = "Italic"

    object FontTypeConst {
        const val THIN = "Thin"
        const val EXTRA_LIGHT = "ExtraLight"
        const val LIGHT = "Light"
        const val NORMAL = "Normal"
        const val REGULAR = "Regular"
        const val MEDIUM = "Medium"
        const val SEMI_BOLD = "SemiBold"
        const val BOLD = "Bold"
        const val EXTRA_BOLD = "ExtraBold"
        const val BLACK = "Black"
    }


    private val indexToWeight: Map<Int, String> = mapOf(
        0 to FontTypeConst.THIN,
        1 to FontTypeConst.EXTRA_LIGHT,
        2 to FontTypeConst.LIGHT,
        3 to FontTypeConst.NORMAL,
        4 to FontTypeConst.MEDIUM,
        5 to FontTypeConst.SEMI_BOLD,
        6 to FontTypeConst.BOLD,
        7 to FontTypeConst.EXTRA_BOLD,
        8 to FontTypeConst.BLACK
    )


    fun getWeightTextByIndex(index: Int): String {
        return indexToWeight[index] ?: ""
    }

    private fun copyFontFile(srcPath: String, savePath: String, name: String) {
        // Source file
        val sourceFile = File(srcPath)

        // Destination directory
        val destinationDir = File(savePath)

        // Create the destination directory if it doesn't exist
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        // Destination file
        val destinationFile = File(destinationDir, name)

        // Check File Exist
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        // Copy the file
        FileUtil.copy(sourceFile, destinationFile)
    }


    // make File
    fun makeFontFamilyFile(
        project: Project,
        fontData: FontData
    ) {
        var generatedFilePath: String? = null
        // Build the class content once before mutating VFS state so write steps stay focused on I/O.
        val classContent = buildFontFamilyContent(fontData)
        ApplicationManager.getApplication().runWriteAction {
            if (fontData.selectedModule == null) {
                return@runWriteAction
            }

            val classFileName = fontData.savingClassFileName
            if (classFileName.isBlank()) {
                return@runWriteAction
            }

            val directory = VfsUtil.createDirectoryIfMissing(fontData.getSaveClassPath())
                ?: return@runWriteAction

            WriteAction.run<Throwable> {
                copyFontResources(fontData)
                val psiFile = writeGeneratedClassFile(
                    project = project,
                    directory = directory,
                    classFileName = classFileName,
                    content = classContent,
                )
                generatedFilePath = psiFile.virtualFile.path
                refreshFontDirectory(fontData)
            }

            // Refresh project view/index after generated sources and copied fonts are added.
            reloadProject(project, fontData)
        }

        generatedFilePath?.let { path ->
            ApplicationManager.getApplication().invokeLater {
                val target = LocalFileSystem.getInstance().findFileByPath(path) ?: return@invokeLater
                OpenFileDescriptor(project, target).navigate(true)
            }
        }
    }

    private fun reloadProject(project: Project, fontData: FontData) {
        val projectBase = project.basePath
        if (!projectBase.isNullOrBlank()) {
            LocalFileSystem.getInstance().findFileByPath(projectBase)?.let {
                VfsUtil.markDirtyAndRefresh(true, true, true, it)
            }
        }

        val moduleClassPath = fontData.selectedModule?.path.orEmpty()
        if (moduleClassPath.isNotBlank()) {
            LocalFileSystem.getInstance().findFileByPath(moduleClassPath)?.let {
                VfsUtil.markDirtyAndRefresh(true, true, true, it)
            }
        }

        VirtualFileManager.getInstance().asyncRefresh(null)
    }

    private fun buildFontFamilyContent(fontData: FontData): String {
        val st = StringBuilder()
        if (fontData.packageName.isNotEmpty()) {
            st.append("package ")
            st.append(fontData.packageName)
            st.append("\n")
        }
        val resourceBaseName = fontData.fileName.lowercase()
        val valueName = fontData.fileName.toLowerCamelCase()
        val functionName = fontData.fileName.toUpperCamelCase()
        st.appendLine()
        fontData.selectedModule?.let { module ->
            val isCMPProject = module.isCMP
            if (isCMPProject) {
                st.appendLine("import androidx.compose.runtime.Composable")
                st.appendLine("import androidx.compose.ui.text.font.FontFamily")
                st.appendLine("import androidx.compose.ui.text.font.FontStyle")
                st.appendLine("import androidx.compose.ui.text.font.FontWeight")

                st.appendLine("import ${module.importModuleName}.generated.resources.Res")
                fontData.totalFontPath.forEach { font ->
                    val fontName = font.makeFontFileName(resourceBaseName, false)
                    st.appendLine("import ${module.importModuleName}.generated.resources.${fontName}")
                }
                st.appendLine("import org.jetbrains.compose.resources.Font")
                st.appendLine()
                st.appendLine("@Composable")
                st.appendLine("fun get${functionName}Font(): FontFamily {")
                st.appendLine("\treturn FontFamily(")
            } else {
                st.appendLine("import androidx.compose.ui.text.font.Font")
                st.appendLine("import androidx.compose.ui.text.font.FontFamily")
                st.appendLine("import androidx.compose.ui.text.font.FontStyle")
                st.appendLine("import androidx.compose.ui.text.font.FontWeight")
                st.appendLine()
                st.appendLine("val $valueName = FontFamily(")
            }

            fontData.totalFontPath.forEach { font ->
                st.appendLine(makeFontString(isCMPProject, resourceBaseName, font))
            }

            if (isCMPProject) {
                st.appendLine("\t)")
                st.appendLine("}")
            } else {
                st.appendLine()
                st.append(")")
            }
        }

        return st.toString().trimIndent()
    }

    private fun copyFontResources(fontData: FontData) {
        val lowerName = fontData.fileName.lowercase()
        fontData.totalFontPath.forEach { font ->
            copyFontFile(font.path, fontData.saveFontPath, font.makeFontFileName(lowerName))
        }
    }

    private fun writeGeneratedClassFile(
        project: Project,
        directory: com.intellij.openapi.vfs.VirtualFile,
        classFileName: String,
        content: String,
    ) = PsiDirectoryFactory.getInstance(project).createDirectory(directory).run {
        // Replace the generated file atomically from the plugin's point of view to keep output deterministic.
        var psiFile = findFile(classFileName)
        psiFile?.delete()
        psiFile = createFile(classFileName)
        psiFile.virtualFile.setBinaryContent(content.toByteArray())
        VfsUtil.markDirtyAndRefresh(true, true, true, psiFile.virtualFile)
        psiFile
    }

    private fun refreshFontDirectory(fontData: FontData) {
        LocalFileSystem.getInstance().findFileByPath(fontData.saveFontPath)?.let {
            VfsUtil.markDirtyAndRefresh(true, true, true, it)
        }
    }

    private fun makeFontString(isCMPProject: Boolean, name: String, fontType: FontType): String {
        val st = StringBuilder()
        val type = getWeightTextByIndex(fontType.weight)
        val fontFile = "${name.lowercase()}_${type.lowercase()}"
        val isItalic = fontType is FontType.Italic

        if (isCMPProject) {
            st.append("\t\tFont(")
            st.append("resource = Res.font.${fontFile}")
            if (isItalic) {
                st.append("_italic")
            }
            st.append(", ")
            st.append("weight = FontWeight.${type}, ")
            st.append("style = FontStyle.")
        } else {
            st.append("\tFont(")
            st.append("R.font.${fontFile}")
            if (isItalic) {
                st.append("_italic")
            }
            st.append(", FontWeight.${type}, FontStyle.")
        }
        if (isItalic) {
            st.append("Italic")
        } else {
            st.append("Normal")
        }
        st.append("),")
        return st.toString()
    }

    private fun String.toUpperCamelCase(): String {
        val trimmed = trim()
        if (trimmed.isEmpty()) return ""

        val normalized = trimmed
            .replace(Regex("[^A-Za-z0-9]+"), " ")
            .trim()
        if (normalized.isEmpty()) return ""

        val parts = normalized.split(Regex("\\s+"))
        return buildString {
            parts.forEach { part ->
                append(part.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }

    private fun String.toLowerCamelCase(): String {
        val upperCamel = toUpperCamelCase()
        return upperCamel.replaceFirstChar { it.lowercase() }
    }

}
