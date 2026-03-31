package com.codag.jetbrains.webview

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

/**
 * Bidirectional message bridge between Kotlin plugin and JCEF WebView.
 *
 * Protocol:
 *   **JS → Kotlin**: `cefQuery` (CefMessageRouter) → [parseIncomingMessage]
 *   **Kotlin → JS**: [createOutgoingMessage] → `browser.executeJavaScript("window.__codagDispatch(…)")`
 *
 * All commands should use constants from [com.codag.jetbrains.CodagConstants].
 */
object CodagMessageBridge {

    private val gson = Gson()

    /** Parse an incoming JSON message from the webview into a [BridgeMessage]. */
    fun parseIncomingMessage(json: String): BridgeMessage {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val command = obj.get("command")?.asString ?: "unknown"
            val payload = obj.entrySet()
                .filter { it.key != "command" }
                .takeIf { it.isNotEmpty() }
                ?.associate { it.key to it.value }
            BridgeMessage(command, payload)
        } catch (e: Exception) {
            BridgeMessage("error", mapOf("raw" to JsonPrimitive(json)))
        }
    }

    /** Build a JSON string to dispatch to the webview via `window.__codagDispatch`. */
    fun createOutgoingMessage(command: String, fields: Map<String, Any>? = null): String {
        val obj = JsonObject()
        obj.addProperty("command", command)
        fields?.forEach { (key, value) ->
            when (value) {
                is String -> obj.addProperty(key, value)
                is Number -> obj.addProperty(key, value)
                is Boolean -> obj.addProperty(key, value)
                else -> obj.addProperty(key, value.toString())
            }
        }
        return gson.toJson(obj)
    }

    /** Escape JSON for safe injection into a `<script>` context. */
    fun escapeForScript(json: String): String =
        json.replace("</script>", "<\\/script>")
}

/**
 * Parsed message from the webview bridge.
 * Access typed fields via [getString] / [getInt].
 */
data class BridgeMessage(
    val command: String,
    val payload: Map<String, Any>?
) {
    fun getString(key: String): String? {
        val value = payload?.get(key) ?: return null
        return when (value) {
            is JsonPrimitive -> value.asString
            is String -> value
            else -> value.toString()
        }
    }

    fun getInt(key: String): Int? {
        val value = payload?.get(key) ?: return null
        return when (value) {
            is JsonPrimitive -> value.asInt
            is Number -> value.toInt()
            else -> value.toString().toIntOrNull()
        }
    }
}
