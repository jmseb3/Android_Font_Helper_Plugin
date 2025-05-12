package com.wonddak.fonthelper.setting

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class FontMatchSettingsConfigurable : Configurable {

    private val service = FontMatchSettingsService.getInstance()
    private val settings: FontMatchSettingsState
        get() = service.state

    // 리스트 모델 저장
    private lateinit var thinListModel: DefaultListModel<String>
    private lateinit var extraLightListModel: DefaultListModel<String>


    override fun getDisplayName(): String = "Font Helper Settings"

    override fun createComponent(): JComponent {
        return panel {
            collapsibleGroup("Thin Keywords") {
                row {
                    cell(
                        createKeywordList(settings.thinKeywords) {
                            thinListModel = it
                        }
                    )
                }
            }
            collapsibleGroup("Extra Light Keywords") {
                row {
                    cell(
                        createKeywordList(settings.extraLightKeywords) {
                            extraLightListModel = it
                        }
                    )
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return !settings.thinKeywords.contentEquals(getListFrom(thinListModel)) ||
                !settings.extraLightKeywords.contentEquals(getListFrom(extraLightListModel))
    }

    override fun apply() {
        settings.thinKeywords = getListFrom(thinListModel)
        settings.extraLightKeywords = getListFrom(extraLightListModel)
    }

    private fun getListFrom(model: DefaultListModel<String>): List<String> {
        return (0 until model.size()).map { model.getElementAt(it) }
    }

    private fun List<String>.contentEquals(other: List<String>): Boolean {
        return this.size == other.size && this.zip(other).all { it.first == it.second }
    }

    private fun createKeywordList(
        initialKeywords: List<String>,
        onModelCreated: (DefaultListModel<String>) -> Unit
    ): JPanel {
        val model = DefaultListModel<String>().apply {
            initialKeywords.forEach { addElement(it) }
        }
        onModelCreated(model)

        val list = JBList(model).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        return ToolbarDecorator.createDecorator(list)
            .setAddAction {
                val newKeyword = JOptionPane.showInputDialog("Add Keyword:")
                if (!newKeyword.isNullOrBlank()) {
                    model.addElement(newKeyword.trim())
                }
            }
            .setRemoveAction {
                val selected = list.selectedIndex
                if (selected >= 0) {
                    model.remove(selected)
                }
            }
            .createPanel()
    }
}