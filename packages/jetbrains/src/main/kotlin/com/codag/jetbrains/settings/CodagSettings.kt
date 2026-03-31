package com.codag.jetbrains.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(
    name = "com.codag.jetbrains.settings.CodagSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class CodagSettings : PersistentStateComponent<CodagSettingsState> {

    private var settingsState = CodagSettingsState()

    override fun getState(): CodagSettingsState = settingsState.copy()

    override fun loadState(state: CodagSettingsState) {
        settingsState = state.copy()
    }
}

data class CodagSettingsState(
    var backendUrl: String = DEFAULT_BACKEND_URL,
    var timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    var cacheEnabled: Boolean = DEFAULT_CACHE_ENABLED,
    var autoAnalyze: Boolean = DEFAULT_AUTO_ANALYZE
) {
    companion object {
        const val DEFAULT_BACKEND_URL = "http://localhost:52104"
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val DEFAULT_CACHE_ENABLED = true
        const val DEFAULT_AUTO_ANALYZE = false
    }
}
