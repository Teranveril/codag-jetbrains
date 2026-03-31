package com.codag.jetbrains.watch

/**
 * Tracks backend connectivity state with callbacks.
 * Pure utility — testable without IDE.
 */
class BackendHealthMonitor {

    enum class State { UNKNOWN, CONNECTED, DISCONNECTED }

    var currentState: State = State.UNKNOWN
        private set

    var isApiKeyValid: Boolean = false
        private set

    private val listeners = mutableListOf<(State) -> Unit>()

    val statusText: String
        get() = when {
            currentState == State.UNKNOWN -> "Unknown"
            currentState == State.DISCONNECTED -> "Disconnected"
            currentState == State.CONNECTED && !isApiKeyValid -> "API Key Invalid"
            else -> "Connected"
        }

    fun onStateChange(listener: (State) -> Unit) {
        listeners.add(listener)
    }

    fun reportHealthy(apiKeyValid: Boolean) {
        isApiKeyValid = apiKeyValid
        val newState = State.CONNECTED
        if (currentState != newState) {
            currentState = newState
            listeners.forEach { it(newState) }
        }
    }

    fun reportUnreachable() {
        val newState = State.DISCONNECTED
        if (currentState != newState) {
            currentState = newState
            listeners.forEach { it(newState) }
        }
    }
}
