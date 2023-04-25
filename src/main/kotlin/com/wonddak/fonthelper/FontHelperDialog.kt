package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.wonddak.fonthelper.util.FontUtil
import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class FontHelperDialog : DialogWrapper(true) {
    private var fileName: String = ""
    private var packageName: String = ""
    private val normalArray: Array<String>
    private val italicArray: Array<String>

    init {
        init()
        title = "Font Helper"
        normalArray = Array(FontUtil.getWeightCount()) { "" }
        italicArray = Array(FontUtil.getWeightCount()) { "" }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        panel.add(makeTitleRow("Input Font Class Name"))
        panel.add(makePackageNameRow("Input your PackageName"))

        panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        panel.add(JLabel("input Font File(.ttf) [Left : Normal]  [Right : Italic]"))

        panel.add(makeFontTable())

        return panel
    }

    private fun makeInputRow(title: String, updateAction: (text: String) -> Unit): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val label = JLabel(title)
        label.horizontalAlignment = SwingConstants.CENTER
        label.preferredSize = Dimension(250, label.preferredSize.height)

        val textField = JTextField(20)
        val dl: DocumentListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            override fun removeUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            override fun changedUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            fun updateFieldState() {
                updateAction(textField.text)
            }
        }
        textField.document.addDocumentListener(dl)
        panel.add(label)
        panel.add(textField)
        return panel
    }

    private fun makeTitleRow(title: String): JPanel {
        return makeInputRow(title) { text ->
            fileName = text
        }
    }

    private fun makePackageNameRow(title: String): JPanel {
        return makeInputRow(title) { text ->
            packageName = text
        }
    }


    private fun makeFontTable(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        for (i in 0 until FontUtil.getWeightCount()) {
            val row = makeTypeRow(i)
            panel.add(row)
            panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        }
        return panel
    }

    private fun makeTypeRow(
            index: Int
    ): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)

        val labelComponent = JLabel(FontUtil.getWeightTextByIndex(index))
        labelComponent.horizontalAlignment = SwingConstants.CENTER
        labelComponent.preferredSize = Dimension(100, labelComponent.preferredSize.height)
        panel.add(labelComponent)

        val normalTextField = makeTextFieldWithBrowseButton(FontUtil.NORMAL) { path ->
            normalArray[index] = path
        }
        panel.add(normalTextField)

        val italicTextField = makeTextFieldWithBrowseButton(FontUtil.ITALIC) { path ->
            italicArray[index] = path
        }
        panel.add(italicTextField)

        return panel
    }

    private fun makeTextFieldWithBrowseButton(
            type: String,
            updatePath: (path: String) -> Unit
    ): TextFieldWithBrowseButton {
        val textField = TextFieldWithBrowseButton()

        textField.addBrowseFolderListener(
                "Select $type font file",
                "Select $type font file",
                null,
                FileChooserDescriptor(true, false, false, false, false, false),
        )
        textField.textField.columns = 20
        textField.isEditable = false

        val dl: DocumentListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            override fun removeUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            override fun changedUpdate(e: DocumentEvent) {
                updateFieldState()
            }

            fun updateFieldState() {
                updatePath(textField.text)
            }
        }
        textField.textField.document.addDocumentListener(dl)
        val icon = UIManager.getIcon("FileView.fileIcon")
        textField.setButtonIcon(icon)
        return textField
    }

    override fun doOKAction() {
        var fontCheckList = mutableListOf<FontUtil.FontCheck>()
        if (fileName.length <= 2) {
            return
        }
        if (packageName.isEmpty()) {
            return
        }
        normalArray.forEachIndexed { index, path ->
            println("$index - $path")
            if (path.isNotEmpty()) {
                val fontCheck = FontUtil.FontCheck(false, index)
                FontUtil.copyFontFile(path, fileName, fontCheck)
                fontCheckList.add(fontCheck)
            }
        }

        italicArray.forEachIndexed { index, path ->
            println("$index - $path")
            if (path.isNotEmpty()) {
                val fontCheck = FontUtil.FontCheck(true, index)
                FontUtil.copyFontFile(path, fileName, fontCheck)
                fontCheckList.add(fontCheck)
            }
        }

        FontUtil.makeFontFamily(packageName, fileName, fontCheckList)

        super.doOKAction()

    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}