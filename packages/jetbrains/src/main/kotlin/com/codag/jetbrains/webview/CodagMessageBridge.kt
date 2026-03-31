package com.codag.jetbrains.webview

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Message bridge between Kotlin plugin and JCEF WebView.
 *
 * Protocol:
 *   JS → Kotlin: CefMessageRouter (cefQuery) → parseIncomingMessage()
 *   Kotlin → JS: createOutgoingMessage() → browser.executeJavaScript("window.__codagDispatch(...)")
 */
object CodagMessageBridge {

    private val gson = Gson()

    /**
     * Parse an incoming JSON message from the webview.
     * Returns a BridgeMessage with command and optional payload fields.
     */
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
            BridgeMessage("error", mapOf("raw" to com.google.gson.JsonPrimitive(json)))
        }
    }

    /**
     * Create an outgoing JSON message to send to the webview.
     */
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

    /**
     * Escape a JSON string for safe injection into a JavaScript context.
     * Prevents script injection via </script> tags in data.
     */
    fun escapeForScript(json: String): String {
        return json.replace("</script>", "<\\/script>")
    }
}

/**
 * Parsed message from the webview bridge.
 */
data class BridgeMessage(
    val command: String,
    val payload: Map<String, Any>?
) {
    fun getString(key: String): String? {
        val value = payload?.get(key) ?: return null
        return when (value) {
            is com.google.gson.JsonPrimitive -> value.asString
            is String -> value
            else -> value.toString()
        }
    }

    fun getInt(key: String): Int? {
        val value = payload?.get(key) ?: return null
        return when (value) {
            is com.google.gson.JsonPrimitive -> value.asInt
            is Number -> value.toInt()
            else -> value.toString().toIntOrNull()
        }
    }
}
