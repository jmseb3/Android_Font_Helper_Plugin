package com.wonddak.fonthelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            PathUtil.clearAll()
            PathUtil.base = project.basePath!!
            //clear fontArray When Open
            FontHelperDialog.fontArray = Array(FontUtil.getWeightCount() * 2) { "" }
            FontHelperDialog(project).show()
        }
    }
}