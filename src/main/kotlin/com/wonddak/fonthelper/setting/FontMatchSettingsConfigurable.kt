package com.wonddak.fonthelper.setting

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import java.awt.Font
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class FontMatchSettingsConfigurable : Configurable {

    private val service = FontMatchSettingsService.getInstance()
    private val settings: FontMatchSettingsState
        get() = service.state

    // 리스트 모델 저장
    private val listModels = mutableMapOf<String, DefaultListModel<String>>()

    override fun getDisplayName(): String = "Font Helper Settings"

    override fun createComponent(): JComponent {
        return panel {

            // 헤더: Normal / Italic
            row {
                cell(label("Normal").component).align(Align.CENTER).resizableColumn()
                cell(label("Italic").component).align(Align.CENTER).resizableColumn()
            }

            // 그룹화: thin / thinItalic 같은 이름끼리 묶기
            val grouped = FontMatchSettingsState::class.members
                .filterIsInstance<KProperty1<FontMatchSettingsState, List<String>>>()
                .sortedBy { it.name }
                .groupBy { field ->
                    field.name.removeSuffix("ItalicKeywords").removeSuffix("Keywords")
                }

            for ((weight, props) in grouped) {
                val normalProp = props.find { !it.name.contains("Italic") }
                val italicProp = props.find { it.name.contains("Italic") }

                row(weight.replaceFirstChar { it.uppercase() }) {
                    // Normal Cell
                    if (normalProp != null) {
                        cell(
                            createKeywordList(normalProp.get(settings)) {
                                listModels[normalProp.name] = it
                            }
                        ).align(Align.FILL).resizableColumn()
                    } else {
                        cell(JPanel()).align(Align.FILL).resizableColumn()
                    }

                    // Italic Cell
                    if (italicProp != null) {
                        cell(
                            createKeywordList(italicProp.get(settings)) {
                                listModels[italicProp.name] = it
                            }
                        ).align(Align.FILL).resizableColumn()
                    } else {
                        cell(JPanel()).align(Align.FILL).resizableColumn()
                    }
                }
            }
        }
    }


    override fun isModified(): Boolean {
        return listModels.any { (fieldName, model) ->
            val originalList = getFieldValue(fieldName)
            !originalList.contentEquals(getListFrom(model))
        }
    }

    override fun apply() {
        listModels.forEach { (fieldName, model) ->
            setFieldValue(fieldName, getListFrom(model))
        }
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

    private fun getFieldValue(fieldName: String): List<String> {
        val property = FontMatchSettingsState::class.members
            .firstOrNull { it.name == fieldName } as? KProperty1<FontMatchSettingsState, List<String>>
        return property?.get(settings) ?: emptyList()
    }

    private fun setFieldValue(fieldName: String, value: List<String>) {
        val property = FontMatchSettingsState::class.members
            .firstOrNull { it.name == fieldName } as? KMutableProperty1<FontMatchSettingsState, List<String>>
        property?.set(settings, value)
    }
}