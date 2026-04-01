package com.codag.jetbrains.api

import com.codag.jetbrains.settings.CodagSettingsState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for CodagApiClient — URL construction, config, and JSON parsing.
 */
class CodagApiClientTest {

    private val gson = Gson()

    @Test
    fun testClientCreationWithDefaults() {
        val config = CodagSettingsState()
        val client = CodagApiClient(config)
        assertEquals("http://localhost:52104", client.baseUrl)
        assertEquals(30_000L, client.timeoutMs)
    }

    @Test
    fun testClientCreationWithCustomConfig() {
        val config = CodagSettingsState(
            backendUrl = "http://remote:8080",
            timeoutMs = 60_000L
        )
        val client = CodagApiClient(config)
        assertEquals("http://remote:8080", client.baseUrl)
        assertEquals(60_000L, client.timeoutMs)
    }

    @Test
    fun testBuildAnalyzeUrl() {
        val config = CodagSettingsState()
        val client = CodagApiClient(config)
        assertEquals("http://localhost:52104/analyze", client.buildUrl("/analyze"))
    }

    @Test
    fun testBuildHealthUrl() {
        val config = CodagSettingsState(backendUrl = "http://custom:9999")
        val client = CodagApiClient(config)
        assertEquals("http://custom:9999/health", client.buildUrl("/health"))
    }

    @Test
    fun testTrailingSlashNormalization() {
        val config = CodagSettingsState(backendUrl = "http://localhost:52104/")
        val client = CodagApiClient(config)
        assertEquals("http://localhost:52104/health", client.buildUrl("/health"))
    }

    @Test
    fun testParseHealthResponseOk() {
        val json = """{"status":"ok","api_key_status":"valid"}"""
        val health = gson.fromJson(json, HealthResponse::class.java)
        assertEquals("ok", health.status)
        assertEquals("valid", health.apiKeyStatus)
    }

    @Test
    fun testParseHealthResponseMissingKey() {
        val json = """{"status":"ok","api_key_status":"missing"}"""
        val health = gson.fromJson(json, HealthResponse::class.java)
        assertEquals("ok", health.status)
        assertEquals("missing", health.apiKeyStatus)
    }

    @Test
    fun testParseAnalyzeResponseMinimal() {
        val json = """{"graph":{"nodes":[],"edges":[],"llms_detected":[],"workflows":[]},"usage":null,"cost":null}"""
        val wire = gson.fromJson(json, AnalyzeResponseTestWire::class.java)
        assertNotNull(wire.graph)
        assertTrue(wire.graph.nodes.isEmpty())
        assertNull(wire.usage)
    }

    @Test
    fun testParseAnalyzeResponseWithGraph() {
        val json = """{
            "graph":{
                "nodes":[{"id":"n1","label":"Start","type":"step"}],
                "edges":[{"source":"n1","target":"n2"}],
                "llms_detected":["gemini"],
                "workflows":[]
            },
            "usage":{"input_tokens":100,"output_tokens":50,"total_tokens":150,"cached_tokens":0},
            "cost":{"input_cost":0.001,"output_cost":0.002,"total_cost":0.003}
        }"""
        val wire = gson.fromJson(json, AnalyzeResponseTestWire::class.java)
        assertEquals(1, wire.graph.nodes.size)
        assertEquals("n1", wire.graph.nodes[0].id)
        assertEquals(150, wire.usage!!.total_tokens)
        assertEquals(0.003, wire.cost!!.total_cost, 0.0001)
    }
}

/** Minimal wire DTOs for test-only JSON parsing (mirrors backend JSON shape). */
private data class AnalyzeResponseTestWire(
    val graph: GraphTestWire,
    val usage: UsageTestWire?,
    val cost: CostTestWire?
)

private data class GraphTestWire(
    val nodes: List<NodeTestWire> = emptyList(),
    val edges: List<EdgeTestWire> = emptyList(),
    val llms_detected: List<String> = emptyList(),
    val workflows: List<Any> = emptyList()
)

private data class NodeTestWire(val id: String, val label: String, val type: String)
private data class EdgeTestWire(val source: String, val target: String)
private data class UsageTestWire(val input_tokens: Int, val output_tokens: Int, val total_tokens: Int, val cached_tokens: Int = 0)
private data class CostTestWire(val input_cost: Double, val output_cost: Double, val total_cost: Double)
