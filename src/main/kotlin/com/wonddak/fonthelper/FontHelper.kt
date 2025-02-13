package com.wonddak.fonthelper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wonddak.fonthelper.util.FontUtil

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            println("FontHelper Can't find project")
            return
        }
        FontHelperDialog(project).show()
    }
}