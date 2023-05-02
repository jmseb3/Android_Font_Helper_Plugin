package com.wonddak.fonthelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.android.util.AndroidUtils

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            //start only Android Project
            if (AndroidUtils.getInstance().isAndroidProject(project)) {
                val dialog = FontHelperDialog(project)
                dialog.show()
            }
        }
    }
}