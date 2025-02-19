package com.wonddak.fonthelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            println("FontHelper Can't find project")
            return
        }
        println("FontHelper Open")
        FontHelperDialog(project).show()
    }
}