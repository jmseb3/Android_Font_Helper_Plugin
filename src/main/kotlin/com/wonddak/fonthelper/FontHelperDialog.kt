package com.wonddak.fonthelper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.wonddak.fonthelper.model.FontCheck
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil
import org.jetbrains.jewel.ui.component.Text
import javax.swing.*


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
                st.append(FontUtil.getWeightTextByIndex(index / 2))
                st.append(") ")
                st.append(s)
                println(st.toString())
            }
        }

        //Spinner List (key : name ,value : real path)
        val spinnerList: MutableMap<String, String> = mutableMapOf()

        var isCMPProject:Boolean = false
    }


    init {
        init()
        title = "Font Helper"
    }


    /**
     * make Dialog UI
     */
    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 800, 600)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column {
                            Text("123")
                        }
                    }
                }
            }
        }
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)


        panel.add(
            JPanelUI.makeInputRow("Font Class Name") { text ->
                PathUtil.fileName = text
                JPanelUI.updateInfo()
            }
        )

        panel.add(
            JPanelUI.makeModuleSpinner(project) { text ->
                PathUtil.setModule(text)
                JPanelUI.updateInfo()
            }
        )

        panel.add(
            Box.createVerticalStrut(5)
        )

        panel.add(
            JPanelUI.makeInfoPanel()
        )

        panel.add(
            JPanelUI.makeFontTable()
        )

        return panel
    }


    override fun doOKAction() {
        if (PathUtil.fileName.isEmpty()) {
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
                         index/ 2,
                        path
                    )
                )
            }
        }

        fontCheckList.map { fontCheck -> FontUtil.copyFontFile(fontCheck) }

        FontUtil.makeFontFamilyAndroidProject(fontCheckList)

        super.doOKAction()

    }

    override fun doCancelAction() {
        // Handle Cancel button action
        super.doCancelAction()
    }

}