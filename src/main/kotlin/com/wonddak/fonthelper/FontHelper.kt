package com.wonddak.fonthelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FontHelper : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = FontHelperDialog()
        dialog.show()
    }
}