package com.codag.jetbrains.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * TDD: Tests for CodagSettings — PersistentStateComponent.
 * Written BEFORE implementation.
 */
class CodagSettingsTest : BasePlatformTestCase() {

    fun testDefaultBackendUrl() {
        val settings = CodagSettings()
        assertEquals("http://localhost:52104", settings.state.backendUrl)
    }

    fun testDefaultTimeoutMs() {
        val settings = CodagSettings()
        assertEquals(30_000L, settings.state.timeoutMs)
    }

    fun testDefaultCacheEnabled() {
        val settings = CodagSettings()
        assertTrue(settings.state.cacheEnabled)
    }

    fun testDefaultAutoAnalyze() {
        val settings = CodagSettings()
        assertFalse(settings.state.autoAnalyze)
    }

    fun testModifyBackendUrl() {
        val settings = CodagSettings()
        val newState = settings.state.copy(backendUrl = "http://remote:8080")
        settings.loadState(newState)
        assertEquals("http://remote:8080", settings.state.backendUrl)
    }

    fun testModifyTimeout() {
        val settings = CodagSettings()
        val newState = settings.state.copy(timeoutMs = 60_000L)
        settings.loadState(newState)
        assertEquals(60_000L, settings.state.timeoutMs)
    }

    fun testStateResetToDefaults() {
        val settings = CodagSettings()
        settings.loadState(settings.state.copy(backendUrl = "http://custom:9999", autoAnalyze = true))
        assertEquals("http://custom:9999", settings.state.backendUrl)

        settings.loadState(CodagSettingsState())
        assertEquals("http://localhost:52104", settings.state.backendUrl)
        assertFalse(settings.state.autoAnalyze)
    }
}
