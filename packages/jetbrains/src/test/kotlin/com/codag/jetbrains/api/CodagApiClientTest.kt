package com.codag.jetbrains.api

import com.codag.jetbrains.dto.*
import com.codag.jetbrains.settings.CodagSettingsState
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: Tests for CodagApiClient — HTTP communication with backend.
 * Uses a mock/stub approach: tests verify request construction and response parsing.
 */
class CodagApiClientTest {

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
        val health = CodagApiClient.parseHealthResponse(json)
        assertEquals("ok", health.status)
        assertEquals("valid", health.apiKeyStatus)
    }

    @Test
    fun testParseHealthResponseMissingKey() {
        val json = """{"status":"ok","api_key_status":"missing"}"""
        val health = CodagApiClient.parseHealthResponse(json)
        assertEquals("ok", health.status)
        assertEquals("missing", health.apiKeyStatus)
    }

    @Test
    fun testParseAnalyzeResponseMinimal() {
        val json = """{"graph":{"nodes":[],"edges":[],"llms_detected":[],"workflows":[]},"usage":null,"cost":null}"""
        val response = CodagApiClient.parseAnalyzeResponse(json)
        assertNotNull(response)
        assertTrue(response.graph.nodes.isEmpty())
        assertNull(response.usage)
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
        val response = CodagApiClient.parseAnalyzeResponse(json)
        assertEquals(1, response.graph.nodes.size)
        assertEquals("n1", response.graph.nodes[0].id)
        assertEquals(150, response.usage!!.totalTokens)
        assertEquals(0.003, response.cost!!.totalCost, 0.0001)
    }
}
