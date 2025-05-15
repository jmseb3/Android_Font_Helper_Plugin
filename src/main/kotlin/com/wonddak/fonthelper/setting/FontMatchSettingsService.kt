package com.wonddak.fonthelper.setting

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "FontMatchSettings",
    storages = [Storage("FontMatchSettings.xml")]
)
class FontMatchSettingsService : PersistentStateComponent<FontMatchSettingsState> {

    private var state = FontMatchSettingsState()

    override fun getState(): FontMatchSettingsState = state

    override fun loadState(state: FontMatchSettingsState) {
        this.state = state
    }

    companion object {

        fun getInstance(): FontMatchSettingsService =
            ApplicationManager.getApplication().getService(FontMatchSettingsService::class.java)
    }
}