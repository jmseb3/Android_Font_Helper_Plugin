package com.wonddak.fonthelper

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.wonddak.fonthelper.model.FontCheck
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
import kotlin.math.ceil


class FontHelperDialog(
    private val project: Project
) : DialogWrapper(true) {

    companion object {
        /**
         * font path List
         * odd - normal || even - italic
         */
        var fontArray: Array<String> = Array(FontUtil.getWeightCount() * 2) { "" }

        fun printInfoOfFontArray() {
            println("-".repeat(30))
            fontArray.forEachIndexed { index, s ->
                val st = StringBuilder()
                if (index % 2 == 0) {
                    st.append("italic")
                } else {
                    st.append("normal")
                }
                st.append("(")
                st.append(index)
                st.append(") ")
                st.append(s)
                println(st.toString())
            }
        }

        //Spinner List (key : name ,value : real path)
        val spinnerList: MutableMap<String, String> = mutableMapOf()
    }


    init {
        init()
        title = "Font Helper"
        PathUtil.clearAll()
        PathUtil.base = project.basePath!!
        //clear fontArray When Open
        fontArray  = Array(FontUtil.getWeightCount() * 2) { "" }
    }

    /**
     * make Dialog UI
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        panel.add(
            JPanelUI.makeInputRow("Input Font Class Name") { text ->
                PathUtil.fileName = text
                println(PathUtil.getClassPath())
                println(PathUtil.getFontPath())
            }
        )
        panel.add(
            JPanelUI.makeInputRow("Input your PackageName") { text ->
                PathUtil.packageName = text
                println(PathUtil.getClassPath())
                println(PathUtil.getFontPath())
            }
        )

        panel.add(
            JPanelUI.makeModuleSpinner(project) { text ->
                PathUtil.module = text
                println(PathUtil.getClassPath())
                println(PathUtil.getFontPath())
            }
        )

        panel.add(Box.createVerticalStrut(10)) // add some space between the rows
        panel.add(JLabel("input Font File(.ttf) [Left : Normal]  [Right : Italic]"))
        panel.add(Box.createVerticalStrut(10)) // add some space between the rows

        panel.add(JPanelUI.makeFontTable())

        return panel
    }


    override fun doOKAction() {
        if (PathUtil.fileName.length <= 2) {
            return
        }
        if (PathUtil.packageName.isEmpty()) {
            return
        }
        if (PathUtil.module.isEmpty()) {
            return
        }

        val fontCheckList: MutableList<FontCheck> = mutableListOf()

        fontArray.forEachIndexed { index, path ->
            if (path.isNotEmpty()) {
                fontCheckList.add(
                    FontCheck(
                        (index % 2 == 0),
                        ceil(index.toFloat() / 2).toInt(),
                        path
                    )
                )
            }
        }

        fontCheckList.map { fontCheck -> FontUtil.copyFontFile(fontCheck) }

        FontUtil.makeFontFamily(fontCheckList)

        super.doOKAction()

    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}