package com.codag.jetbrains.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class CodagSettingsConfigurable : BoundConfigurable("Codag") {

    private val settings = CodagSettings()

    override fun createPanel(): DialogPanel = panel {
        group("Backend Connection") {
            row("API URL:") {
                textField()
                    .bindText(settings.state::backendUrl)
                    .comment("Codag backend URL (default: ${CodagSettingsState.DEFAULT_BACKEND_URL})")
            }
            row("Timeout (ms):") {
                textField()
                    .bindText(
                        getter = { settings.state.timeoutMs.toString() },
                        setter = { settings.state.timeoutMs = it.toLongOrNull() ?: CodagSettingsState.DEFAULT_TIMEOUT_MS }
                    )
            }
        }
        group("Behavior") {
            row {
                checkBox("Enable response cache")
                    .bindSelected(settings.state::cacheEnabled)
            }
            row {
                checkBox("Auto-analyze on file open")
                    .bindSelected(settings.state::autoAnalyze)
            }
        }
    }
}
