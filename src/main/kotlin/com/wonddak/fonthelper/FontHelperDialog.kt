package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.wonddak.fonthelper.util.FontUtil
import java.awt.*
import javax.swing.*


class FontHelperDialog : DialogWrapper(true) {
    private lateinit var titleRow: JPanel
    private lateinit var fontPanel: JPanel

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

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)


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

        for (i in 0 until FontUtil.getWeightCount()) {
            val row = makeTypeRow(FontUtil.getWeightTextByIndex(i))
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
        labelComponent.horizontalAlignment = SwingConstants.CENTER
        labelComponent.preferredSize = Dimension(100, labelComponent.preferredSize.height)
        panel.add(labelComponent)

        val normalTextField = makeTextFieldWithBrowseButton(FontUtil.NORMAL)
        panel.add(normalTextField)

        val italicTextField = makeTextFieldWithBrowseButton(FontUtil.ITALIC)
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
        var fileName = ""

        var normalCheck = mutableListOf<Int>()
        var italicCheck = mutableListOf<Int>()

        titleRow.getComponent(1).let { component ->
            if (component is JTextField) {
                fileName = component.text
            }
        }

        if (fileName.length <= 2) {
            return
        }

        for (i in 0 until FontUtil.getWeightCount()) {
            val find = fontPanel.getComponent(2 * i)
            if (find is JPanel) {
                val normal = find.getComponent(1)
                if (normal is TextFieldWithBrowseButton) {
                    normal.text.let { path ->
                        if (path.isNotEmpty()) {
                            FontUtil.copyFontFile(path, FontUtil.makeFileName(fileName, i, false))
                            normalCheck.add(i)
                        }
                    }
                }
                val italic = find.getComponent(2)
                if (italic is TextFieldWithBrowseButton) {
                    italic.text.let { path ->
                        if (path.isNotEmpty()) {
                            FontUtil.copyFontFile(path, FontUtil.makeFileName(fileName, i, true))
                            italicCheck.add(i)
                        }
                    }
                }
            }
        }
        FontUtil.makeFontFamily("com.example.myapplication", fileName, normalCheck, italicCheck)

        super.doOKAction()

    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}