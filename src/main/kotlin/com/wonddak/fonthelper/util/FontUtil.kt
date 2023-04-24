package com.wonddak.fonthelper.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.impl.file.PsiDirectoryFactory
import java.io.File

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

    private fun getProjectRoot(): String? {
        val projectManager = ProjectManager.getInstance()
        val openProjects = projectManager.openProjects
        return if (openProjects.isNotEmpty()) {
            openProjects[0].basePath
        } else {
            null
        }
    }

    private val fontPath = "${getProjectRoot()!!}/app/src/main/res/font"
    private val classPath = "${getProjectRoot()!!}/app/src/main/java"

    fun makeFileName(name: String, index: Int, isItalic: Boolean): String {
        val st = StringBuilder()
        st.append(name.lowercase())
        st.append("_")
        st.append(getWeightTextByIndex(index).lowercase())
        if (isItalic) {
            st.append("_")
            st.append(ITALIC.lowercase())
        }
        st.append(".ttf")
        return st.toString()
    }


    fun copyFontFile(srcPath: String, name: String) {
        // Source file
        val sourceFile = File(srcPath)

        // Destination directory
        val destinationDir = File(fontPath)

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


    fun makeFontFamily(packageName: String, name: String, normalCheck: List<Int>, italicCheck: List<Int>) {

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

        val project = ProjectManager.getInstance().defaultProject
        val directory = VfsUtil.createDirectoryIfMissing("$classPath/${packageName.replace(".", "//")}")
        val fileName = "${name[0].uppercase()}${name.slice(IntRange(1, name.length - 1))}.kt"

        WriteCommandAction.runWriteCommandAction(project) {
            directory?.let { directory ->
                val psiDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(directory)
                var psiFile = psiDirectory.findFile(fileName)
                psiFile?.delete()
                psiFile = psiDirectory.createFile(fileName)

                val st = StringBuilder()
                st.append("package ")
                st.append(packageName)
                st.append("\n")
                st.append("\n")
                st.append("import androidx.compose.ui.text.font.Font\n")
                st.append("import androidx.compose.ui.text.font.FontFamily\n")
                st.append("import androidx.compose.ui.text.font.FontStyle\n")
                st.append("import androidx.compose.ui.text.font.FontWeight\n")
                st.append("\n")
                st.append("val ${name.lowercase()} = FontFamily(\n")

                val list = arrayListOf<String>()
                list.addAll(normalCheck.map { makeFontString(it, false) })
                list.addAll(italicCheck.map { makeFontString(it, true) })
                st.append(list.joinToString(",\n"))
                st.append("\n")
                st.append(")")

                // Add content to the Kotlin file
                val fileContent = st.toString().trimIndent()

                psiFile.virtualFile.setBinaryContent(fileContent.toByteArray())


                // Refresh the directory
                directory.refresh(false, true)
                VfsUtil.createDirectoryIfMissing(fontPath)?.refresh(false, true)
            }
        }
    }

}