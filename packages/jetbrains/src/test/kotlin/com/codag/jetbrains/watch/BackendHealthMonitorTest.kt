package com.codag.jetbrains.watch

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for BackendHealthMonitor — tracks backend connectivity state.
 * Pure utility, no IDE dependencies.
 */
class BackendHealthMonitorTest {

    @Test
    fun testInitialStateUnknown() {
        val monitor = BackendHealthMonitor()
        assertEquals(BackendHealthMonitor.State.UNKNOWN, monitor.currentState)
    }

    @Test
    fun testTransitionToConnected() {
        val monitor = BackendHealthMonitor()
        monitor.reportHealthy(apiKeyValid = true)

        assertEquals(BackendHealthMonitor.State.CONNECTED, monitor.currentState)
        assertTrue(monitor.isApiKeyValid)
    }

    @Test
    fun testTransitionToDisconnected() {
        val monitor = BackendHealthMonitor()
        monitor.reportHealthy(apiKeyValid = true)
        monitor.reportUnreachable()

        assertEquals(BackendHealthMonitor.State.DISCONNECTED, monitor.currentState)
    }

    @Test
    fun testApiKeyInvalid() {
        val monitor = BackendHealthMonitor()
        monitor.reportHealthy(apiKeyValid = false)

        assertEquals(BackendHealthMonitor.State.CONNECTED, monitor.currentState)
        assertFalse(monitor.isApiKeyValid)
    }

    @Test
    fun testStateChangeCallback() {
        val monitor = BackendHealthMonitor()
        val changes = mutableListOf<BackendHealthMonitor.State>()
        monitor.onStateChange { changes.add(it) }

        monitor.reportHealthy(apiKeyValid = true)
        monitor.reportUnreachable()

        assertEquals(2, changes.size)
        assertEquals(BackendHealthMonitor.State.CONNECTED, changes[0])
        assertEquals(BackendHealthMonitor.State.DISCONNECTED, changes[1])
    }

    @Test
    fun testNoCallbackOnSameState() {
        val monitor = BackendHealthMonitor()
        val changes = mutableListOf<BackendHealthMonitor.State>()
        monitor.onStateChange { changes.add(it) }

        monitor.reportHealthy(apiKeyValid = true)
        monitor.reportHealthy(apiKeyValid = true) // same state

        assertEquals(1, changes.size) // only one callback
    }

    @Test
    fun testStatusText() {
        val monitor = BackendHealthMonitor()
        assertEquals("Unknown", monitor.statusText)

        monitor.reportHealthy(apiKeyValid = true)
        assertEquals("Connected", monitor.statusText)

        monitor.reportUnreachable()
        assertEquals("Disconnected", monitor.statusText)

        monitor.reportHealthy(apiKeyValid = false)
        assertEquals("API Key Invalid", monitor.statusText)
    }
}
