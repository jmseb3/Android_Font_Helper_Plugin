package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.impl.file.PsiDirectoryFactory
import java.awt.*
import java.io.File
import javax.swing.*


class FontHelperDialog : DialogWrapper(true) {
    private lateinit var titleRow: JPanel
    private lateinit var fontPanel: JPanel

    private val indexToString: Map<Int, String> = mapOf(
            0 to "Thin",
            1 to "Normal",
            2 to "Medium",
            3 to "Bold",
            4 to "Black"
    )

    init {
        init()
        title = "Font Helper"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        titleRow = makeTitleRow("Input your Font Class Name")
        panel.add(titleRow)
        panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        fontPanel = makeFontTable()
        panel.add(fontPanel)
        return panel
    }

    private fun makeTitleRow(title: String): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val label = JLabel(title)
        val textField = JTextField(20)
        panel.add(label)
        panel.add(textField)
        return panel
    }

    private fun makeFontTable(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        for (i in 0..4) {
            val row = makeTypeRow(
                    indexToString[i]!!
            )
            panel.add(row)
            panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        }
        return panel
    }

    private fun makeTypeRow(
            label: String
    ): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        val labelComponent = JLabel(label)
        val normalTextField = makeTextFieldWithBrowseButton("Normal")
        val italicTextField = makeTextFieldWithBrowseButton("Italic")
        panel.add(labelComponent)
        panel.add(normalTextField)
        panel.add(italicTextField)
        return panel
    }

    private fun makeTextFieldWithBrowseButton(
            type: String
    ): TextFieldWithBrowseButton {
        val textField = TextFieldWithBrowseButton()

        textField.addBrowseFolderListener(
                "Select $type font file",
                "Select $type font file",
                null,
                FileChooserDescriptor(true, false, false, false, false, false),
        )
        textField.textField.columns = 20
        textField.textField.addActionListener {
            val path = textField.text.trim()
            println(path)
        }
        val icon = UIManager.getIcon("FileView.fileIcon")
        textField.setButtonIcon(icon)
        return textField
    }

    override fun doOKAction() {
        var fileTitle = ""

        var fontPath = "${getProjectRoot()!!}/app/src/main/res/font"
        var classPath = "${getProjectRoot()!!}/app/src/main/java"

        var normalCheck = mutableListOf<Int>()
        var italicCheck = mutableListOf<Int>()

        titleRow.getComponent(1).let { component ->
            if (component is JTextField) {
                fileTitle = component.text
            }
        }

        if (classPath.isEmpty() || fontPath.isEmpty() || fileTitle.isEmpty()) {
            return
        }

        println("Font paths:")
        for (i in 0..4) {
            val find = fontPanel.getComponent(2 * i)
            val type = indexToString[i]!!
            if (find is JPanel) {
                val normal = find.getComponent(1)
                if (normal is TextFieldWithBrowseButton) {
                    normal.text.let { path ->
                        if (path.isNotEmpty()) {
                            copyFontFile(path, fontPath, "${fileTitle.lowercase()}_${type.lowercase()}.ttf")
                            normalCheck.add(i)
                        }
                    }
                }
                val italic = find.getComponent(2)
                if (italic is TextFieldWithBrowseButton) {
                    italic.text.let { path ->
                        if (path.isNotEmpty()) {
                            copyFontFile(path, fontPath, "${fileTitle.lowercase()}_${type.lowercase()}_italic.ttf")
                            italicCheck.add(i)
                        }
                    }
                }
            }
        }
        makeFontFamily(classPath, fileTitle, normalCheck, italicCheck)

        super.doOKAction()

    }

    private fun copyFontFile(path: String, dest: String, name: String) {
        // Source file
        val sourceFile = File(path)

        // Destination directory
        val destinationDir = File(dest)

        // Create the destination directory if it doesn't exist
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        // Destination file
        val destinationFile = File(destinationDir, name)

        // Copy the file
        FileUtil.copy(sourceFile, destinationFile)
    }


    private fun makeFontFamily(dest: String, name: String, normalCheck: List<Int>, italicCheck: List<Int>) {
        fun makeFontString(index: Int, isItalic: Boolean): String {
            val st = StringBuilder()
            st.append("\tFont(R.font.${name.lowercase()}_${indexToString[index]!!.lowercase()}")
            if (isItalic) {
                st.append("_italic")
            }
            st.append(", FontWeight.${indexToString[index]}, FontStyle.")
            if (isItalic) {
                st.append("Italic")
            } else {
                st.append("Normal")
            }
            st.append(")")
            return st.toString()
        }

        val project = ProjectManager.getInstance().defaultProject
        val directory = VfsUtil.createDirectoryIfMissing(dest)
        val fileName = "${name}.kt"
        directory?.let { directory ->
            val psiDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(directory)
            val psiFile = psiDirectory.createFile(fileName)

            val st = StringBuilder()
            st.append("import androidx.compose.ui.text.font.Font\n")
            st.append("import androidx.compose.ui.text.font.FontFamily\n")
            st.append("import androidx.compose.ui.text.font.FontStyle\n")
            st.append("import androidx.compose.ui.text.font.FontWeight\n")
            st.append("\n")
            st.append("val $name = FontFamily(\n")

            val list = arrayListOf<String>()
            list.addAll(normalCheck.map { makeFontString(it, false) })
            list.addAll(italicCheck.map { makeFontString(it, true) })
            st.append(list.joinToString(",\n"))
            st.append("\n")
            st.append(")")

            // Add content to the Kotlin file
            val fileContent = st.toString().trimIndent()
            try {
                psiFile.virtualFile.setBinaryContent(fileContent.toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Refresh the directory
            directory.refresh(false, true)
        }

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

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}