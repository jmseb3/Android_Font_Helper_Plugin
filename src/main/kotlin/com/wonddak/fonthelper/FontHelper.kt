package com.wonddak.fonthelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil
import org.jetbrains.android.util.AndroidUtils

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            PathUtil.clearAll()
            PathUtil.base = project.basePath!!
            //clear fontArray When Open
            FontHelperDialog.fontArray = Array(FontUtil.getWeightCount() * 2) { "" }

            //start only Android Project
            if (AndroidUtils.getInstance().isAndroidProject(project)) {
                val dialog = FontHelperDialog(project)
                dialog.show()
            }
        }
    }
}