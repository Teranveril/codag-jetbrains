package com.codag.jetbrains.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "com.codag.jetbrains.settings.CodagSettings",
    storages = [Storage("codagSettings.xml")]
)
class CodagSettings : PersistentStateComponent<CodagSettingsState> {

    private var settingsState = CodagSettingsState()

    override fun getState(): CodagSettingsState = settingsState.copy()

    override fun loadState(state: CodagSettingsState) {
        settingsState = state.copy()
    }

    companion object {
        @JvmStatic
        fun getInstance(): CodagSettings =
            ApplicationManager.getApplication().getService(CodagSettings::class.java)
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
