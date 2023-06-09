package com.wonddak.fonthelper.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.impl.file.PsiDirectoryFactory
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


    fun makeFontFamily(fontCheck: List<FontCheck>) {
        val name = PathUtil.fileName
        val project = ProjectManager.getInstance().defaultProject

        val directory = VfsUtil.createDirectoryIfMissing(PathUtil.getClassPath())
        val fileName =  PathUtil.makeSavingFormatFileName()

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
        fun makeFontString(index: Int, isItalic: Boolean): String {
            val st = StringBuilder()
            val type = getWeightTextByIndex(index)
            st.append("\tFont(R.font.${name.lowercase()}_${type.lowercase()}")
            if (isItalic) {
                st.append("_italic")
            }
            st.append(", FontWeight.${type}, FontStyle.")
            if (isItalic) {
                st.append("Italic")
            } else {
                st.append("Normal")
            }
            st.append(")")
            return st.toString()
        }

        val st = StringBuilder()
        if (PathUtil.packageName.isNotEmpty()) {
            st.append("package ")
            st.append(PathUtil.packageName)
            st.append("\n")
        }
        st.append("\n")
        st.append("import androidx.compose.ui.text.font.Font\n")
        st.append("import androidx.compose.ui.text.font.FontFamily\n")
        st.append("import androidx.compose.ui.text.font.FontStyle\n")
        st.append("import androidx.compose.ui.text.font.FontWeight\n")
        st.append("\n")
        st.append("val ${name.lowercase()} = FontFamily(\n")
        st.append(fontCheck.joinToString(",\n") { makeFontString(it.weightIndex, it.isItalic) })
        st.append("\n")
        st.append(")")

        return st.toString().trimIndent()
    }

}