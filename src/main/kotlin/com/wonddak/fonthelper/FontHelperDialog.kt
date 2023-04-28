package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class FontHelperDialog(
    private val project: Project
) : DialogWrapper(true) {

    companion object {
        // Class Name and Variable Name
        private var fileName: String = ""

        val normalArray: Array<String> = Array(FontUtil.getWeightCount()) { "" }
        val italicArray: Array<String> = Array(FontUtil.getWeightCount()) { "" }

        //Spinner List (key : name ,value : real path)
        val spinnerList: MutableMap<String, String> = mutableMapOf()
    }


    init {
        init()
        title = "Font Helper"
        PathUtil.base = project.basePath!!
    }

    /**
     * make Dialog UI
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        panel.add(
            JPanelUI.makeInputRow("Input Font Class Name") { text ->
                fileName = text
            }
        )
        panel.add(
            JPanelUI.makeInputRow("Input your PackageName") { text ->
                PathUtil.packageName = text
            }
        )

        panel.add(
            JPanelUI.makeModuleSpinner(project) { text ->
                PathUtil.module = text
            }
        )

        panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        panel.add(JLabel("input Font File(.ttf) [Left : Normal]  [Right : Italic]"))

        panel.add(JPanelUI.makeFontTable())

        return panel
    }


    override fun doOKAction() {
        val fontCheckList = mutableListOf<FontUtil.FontCheck>()

        println(PathUtil.getFontPath())
        println(PathUtil.getClassPath())
        if (fileName.length <= 2) {
            return
        }
        if (PathUtil.packageName.isEmpty()) {
            return
        }
        if (PathUtil.module.isEmpty()) {
            return
        }

        normalArray.forEachIndexed { index, path ->
            if (path.isNotEmpty()) {
                val fontCheck = FontUtil.FontCheck(false, index)
                FontUtil.copyFontFile(path, fileName, fontCheck)
                fontCheckList.add(fontCheck)
            }
        }

        italicArray.forEachIndexed { index, path ->
            if (path.isNotEmpty()) {
                val fontCheck = FontUtil.FontCheck(true, index)
                FontUtil.copyFontFile(path, fileName, fontCheck)
                fontCheckList.add(fontCheck)
            }
        }

        FontUtil.makeFontFamily(fileName, fontCheckList)

        super.doOKAction()

    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}