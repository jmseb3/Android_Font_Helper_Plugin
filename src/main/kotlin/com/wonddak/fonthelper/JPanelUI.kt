package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.wonddak.fonthelper.FontHelperDialog.Companion.fontArray
import com.wonddak.fonthelper.FontHelperDialog.Companion.printInfoOfFontArray
import com.wonddak.fonthelper.FontHelperDialog.Companion.spinnerList
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

object JPanelUI {

    /**
     *  make Dialog Row (label - textField)
     */
    fun makeInputRow(title: String, updateAction: (text: String) -> Unit): JPanel {
        val panel = JPanel()
        panel.layout = FlowLayout(FlowLayout.LEFT)
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

    fun makeModuleSpinner(project: Project, spinnerChangeAction: (selectedValue: String) -> Unit): JPanel {
        val base = project.basePath
        val moduleManager = ModuleManager.getInstance(project)
        for (module in moduleManager.modules) {
            val sourceRoots = ModuleRootManager.getInstance(module).contentRoots
            sourceRoots.forEach {
                if (it.parent.path == base) {
                    spinnerList[it.name] = it.path
                }
            }
        }
        //set First Value is Default
        if (spinnerList.isNotEmpty()) {
            PathUtil.module = spinnerList.entries.iterator().next().key
        }
        val label = JLabel("Select Module For Add class File")
        label.horizontalAlignment = SwingConstants.CENTER
        label.preferredSize = Dimension(250, label.preferredSize.height)

        val spinnerModel = DefaultComboBoxModel(spinnerList.keys.toList().toTypedArray())
        val spinner = ComboBox(spinnerModel)
        spinner.addActionListener {
            spinnerChangeAction(spinner.selectedItem!!.toString())
        }
        spinner.preferredSize = JTextField(20).preferredSize


        val panel = JPanel()
        panel.layout = FlowLayout(FlowLayout.LEFT)

        panel.add(label)
        panel.add(spinner)
        return panel
    }


    /**
     * make Font Table
     */
    fun makeFontTable(): JPanel {

        // make Button with BrowserButton
        fun makeTextFieldWithBrowseButton(
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

            // Add file path drag and drop
            textField.textField.transferHandler = object : TransferHandler() {
                override fun importData(support: TransferSupport): Boolean {
                    if (!canImport(support)) {
                        return false
                    }

                    val transferable = support.transferable
                    return try {
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        if (files.size == 1) {
                            textField.text = files[0].absolutePath
                            true
                        } else {
                            false
                        }
                    } catch (e: UnsupportedClassVersionError) {
                        e.printStackTrace()
                        false
                    } catch (e: IOException) {
                        e.printStackTrace()
                        false
                    }
                }

                override fun canImport(support: TransferSupport): Boolean {
                    return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                }
            }

            val icon = UIManager.getIcon("FileView.fileIcon")
            textField.setButtonIcon(icon)
            return textField
        }

        // make Table Row (label-textField-textField)
        fun makeTypeRow(
            index: Int
        ): JPanel {
            val panel = JPanel()
            panel.layout = FlowLayout(FlowLayout.LEFT)

            val labelComponent = JLabel(FontUtil.getWeightTextByIndex(index))
            labelComponent.horizontalAlignment = SwingConstants.CENTER
            labelComponent.preferredSize = Dimension(100, labelComponent.preferredSize.height)
            panel.add(labelComponent)

            val normalTextField = makeTextFieldWithBrowseButton(FontUtil.NORMAL) { path ->
                fontArray[2 * index + 1] = path
                printInfoOfFontArray()
            }
            panel.add(normalTextField)

            val italicTextField = makeTextFieldWithBrowseButton(FontUtil.ITALIC) { path ->
                fontArray[2 * index] = path
                printInfoOfFontArray()
            }
            panel.add(italicTextField)

            return panel
        }

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val xPanel = JPanel()
        xPanel.layout = FlowLayout(FlowLayout.CENTER)
        xPanel.add(JLabel("[Left : Normal]  [Right : Italic]"))
        panel.add(xPanel)

        for (i in 0 until FontUtil.getWeightCount()) {
            val row = makeTypeRow(i)
            panel.add(row)
            panel.add(Box.createVerticalStrut(5)) // add some space between the rows
        }
        return panel
    }
}