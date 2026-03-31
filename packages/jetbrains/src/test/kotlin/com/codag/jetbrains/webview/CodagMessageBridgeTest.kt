package com.codag.jetbrains.webview

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for CodagMessageBridge — message protocol between Kotlin and JS.
 * Tests message parsing/serialization without requiring JCEF runtime.
 */
class CodagMessageBridgeTest {

    // --- Webview → Kotlin message parsing ---

    @Test
    fun testParseWebviewReadyMessage() {
        val json = """{"command":"webviewReady"}"""
        val parsed = CodagMessageBridge.parseIncomingMessage(json)
        assertEquals("webviewReady", parsed.command)
        assertNull(parsed.payload)
    }

    @Test
    fun testParseOpenFileMessageWithFileAndLine() {
        val json = """{"command":"openFile","file":"src/main.py","line":42}"""
        val parsed = CodagMessageBridge.parseIncomingMessage(json)
        assertEquals("openFile", parsed.command)
        assertEquals("src/main.py", parsed.getString("file"))
        assertEquals(42, parsed.getInt("line"))
    }

    @Test
    fun testParseNodeSelectedMessage() {
        val json = """{"command":"nodeSelected","nodeId":"node-1","nodeLabel":"LLM Call","nodeType":"llm_call"}"""
        val parsed = CodagMessageBridge.parseIncomingMessage(json)
        assertEquals("nodeSelected", parsed.command)
        assertEquals("node-1", parsed.getString("nodeId"))
        assertEquals("LLM Call", parsed.getString("nodeLabel"))
        assertEquals("llm_call", parsed.getString("nodeType"))
    }

    @Test
    fun testParseRetryAnalysisMessage() {
        val json = """{"command":"retryAnalysis"}"""
        val parsed = CodagMessageBridge.parseIncomingMessage(json)
        assertEquals("retryAnalysis", parsed.command)
    }

    @Test
    fun testParseMalformedJsonReturnsErrorMessage() {
        val json = "not valid json{"
        val parsed = CodagMessageBridge.parseIncomingMessage(json)
        assertEquals("error", parsed.command)
    }

    // --- Kotlin → Webview message serialization ---

    @Test
    fun testSerializeAnalysisStartedMessage() {
        val json = CodagMessageBridge.createOutgoingMessage("analysisStarted")
        val obj = JsonParser.parseString(json).asJsonObject
        assertEquals("analysisStarted", obj.get("command").asString)
    }

    @Test
    fun testSerializeUpdateGraphMessageWithGraphData() {
        val graphJson = """{"nodes":[{"id":"n1","label":"Test"}],"edges":[],"llms_detected":[],"workflows":[]}"""
        val json = CodagMessageBridge.createOutgoingMessage("updateGraph", mapOf("graph" to graphJson))
        val obj = JsonParser.parseString(json).asJsonObject
        assertEquals("updateGraph", obj.get("command").asString)
        assertTrue(obj.has("graph"))
    }

    @Test
    fun testSerializeShowLoadingMessageWithText() {
        val json = CodagMessageBridge.createOutgoingMessage("showLoading", mapOf("text" to "Analyzing..."))
        val obj = JsonParser.parseString(json).asJsonObject
        assertEquals("showLoading", obj.get("command").asString)
        assertEquals("Analyzing...", obj.get("text").asString)
    }

    @Test
    fun testSerializeBackendErrorMessage() {
        val json = CodagMessageBridge.createOutgoingMessage("backendError")
        val obj = JsonParser.parseString(json).asJsonObject
        assertEquals("backendError", obj.get("command").asString)
    }

    @Test
    fun testSerializeFocusNodeMessage() {
        val json = CodagMessageBridge.createOutgoingMessage("focusNode", mapOf("nodeId" to "node-5"))
        val obj = JsonParser.parseString(json).asJsonObject
        assertEquals("focusNode", obj.get("command").asString)
        assertEquals("node-5", obj.get("nodeId").asString)
    }
}
