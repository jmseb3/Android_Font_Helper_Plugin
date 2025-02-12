package com.wonddak.fonthelper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.PathUtil

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            println("FontHelper Can't find project")
//            Notifications.Bus.notify(
//                Notification(
//                    /* groupId = */ "FontHelper_error",
//                    "FontHelper",
//                    "No project found. Wait indexing...",
//                    NotificationType.ERROR
//                )
//            )
            return
        }
        PathUtil.clearAll()
        PathUtil.base = project.basePath!!
        //clear fontArray When Open
        FontHelperDialog.fontArray = Array(FontUtil.getWeightCount() * 2) { "" }
        FontHelperDialog(project).show()
    }
}