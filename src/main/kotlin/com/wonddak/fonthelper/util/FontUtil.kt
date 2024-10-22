package com.wonddak.fonthelper.util

import ai.grazie.utils.capitalize
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.wonddak.fonthelper.FontHelperDialog.Companion.isCMPProject
import com.wonddak.fonthelper.model.FontCheck
import java.io.File

/**
 * Font Helper Object
 */
object FontUtil {
    const val NORMAL = "Normal"
    const val ITALIC = "Italic"

    private val indexToWeight: Map<Int, String> = mapOf(
        0 to "Thin",
        1 to "ExtraLight",
        2 to "Light",
        3 to "Normal",
        4 to "Medium",
        5 to "SemiBold",
        6 to "Bold",
        7 to "ExtraBold",
        8 to "Black"
    )

    fun getWeightTextByIndex(index: Int): String {
        return indexToWeight[index] ?: ""
    }

    fun getWeightCount(): Int {
        return indexToWeight.size
    }


    private fun makeFileName(name: String, fontCheck: FontCheck): String {
        val st = StringBuilder()
        st.append(name.lowercase())
        st.append("_")
        st.append(getWeightTextByIndex(fontCheck.weightIndex).lowercase())
        if (fontCheck.isItalic) {
            st.append("_")
            st.append(ITALIC.lowercase())
        }
        st.append(".ttf")
        return st.toString()
    }


    private fun copyFontFile(srcPath: String, name: String) {
        // Source file
        val sourceFile = File(srcPath)

        // Destination directory
        val destinationDir = File(PathUtil.getFontPath())

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

    fun copyFontFile(fontCheck: FontCheck) {
        copyFontFile(fontCheck.path, makeFileName(PathUtil.fileName, fontCheck))
    }


    // make File For Android Only Project
    fun makeFontFamilyAndroidProject(fontCheck: List<FontCheck>) {
        val name = PathUtil.fileName
        val project = ProjectManager.getInstance().defaultProject

        val directory = VfsUtil.createDirectoryIfMissing(PathUtil.getClassPath())
        val fileName = PathUtil.makeSavingFormatFileName()

        WriteCommandAction.runWriteCommandAction(project) {
            directory?.let { directory ->
                val psiDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(directory)
                var psiFile = psiDirectory.findFile(fileName)
                psiFile?.delete()
                psiFile = psiDirectory.createFile(fileName)
                psiFile.virtualFile.setBinaryContent(makeContentString(name, fontCheck).toByteArray())
            }
        }
        PathUtil.refresh()
    }

    private fun makeContentString(name: String, fontCheck: List<FontCheck>): String {

        val st = StringBuilder()
        if (PathUtil.packageName.isNotEmpty()) {
            st.append("package ")
            st.append(PathUtil.packageName)
            st.append("\n")
        }
        val lowerName = name.lowercase()
        st.appendLine()
        if (isCMPProject) {
            st.appendLine("import androidx.compose.runtime.Composable")
            st.appendLine("import androidx.compose.ui.text.font.FontFamily")
            st.appendLine("import androidx.compose.ui.text.font.FontStyle")
            st.appendLine("import androidx.compose.ui.text.font.FontWeight")
            st.appendLine("import multiplatform_app.composeapp.generated.resources.Res")
            fontCheck.forEach { font->
                var fontName = lowerName
                fontName += "_"
                fontName += getWeightTextByIndex(font.weightIndex).lowercase()
                if (font.isItalic) {
                    fontName += "_italic"
                }
                st.appendLine("import multiplatform_app.composeapp.generated.resources.${fontName}")
            }
            st.appendLine("import org.jetbrains.compose.resources.Font")
            st.appendLine()
            st.appendLine("@Composable")
            st.appendLine("fun get${lowerName.capitalize()}Font(): FontFamily {")
            st.appendLine("\treturn FontFamily(")
            st.append(fontCheck.joinToString(",\n") { makeFontString(name, it.weightIndex, it.isItalic) })
            st.appendLine()
            st.appendLine("\t)")
            st.appendLine("}")
        } else {
            st.appendLine("import androidx.compose.ui.text.font.Font")
            st.appendLine("import androidx.compose.ui.text.font.FontFamily")
            st.appendLine("import androidx.compose.ui.text.font.FontStyle")
            st.appendLine("import androidx.compose.ui.text.font.FontWeight")
            st.appendLine()
            st.appendLine("val $lowerName = FontFamily(")
            st.append(fontCheck.joinToString(",\n") { makeFontString(name, it.weightIndex, it.isItalic) })
            st.appendLine("")
            st.append(")")
        }
        return st.toString().trimIndent()
    }

    private fun makeFontString(name: String, index: Int, isItalic: Boolean): String {
        val st = StringBuilder()
        val type = getWeightTextByIndex(index)
        val fontFile = "${name.lowercase()}_${type.lowercase()}"

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
        st.append(")")
        return st.toString()
    }


}