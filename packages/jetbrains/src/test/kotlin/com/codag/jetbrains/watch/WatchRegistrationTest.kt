package com.codag.jetbrains.watch

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for plugin.xml registration of status bar widget and file watcher.
 */
class WatchRegistrationTest {

    @Test
    fun testStatusBarWidgetRegisteredInPluginXml() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull("plugin.xml must exist", xml)
        assertTrue(
            "Status bar widget factory must be registered",
            xml!!.contains("statusBarWidgetFactory")
        )
    }

    @Test
    fun testFileWatcherListenerRegisteredInPluginXml() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull("plugin.xml must exist", xml)
        assertTrue(
            "VFS listener must be registered",
            xml!!.contains("vfs.asyncListener") || xml.contains("BulkFileListener")
        )
    }

    @Test
    fun testPostStartupActivityRegistered() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull(xml)
        assertTrue(
            "PostStartupActivity should be registered for health check",
            xml!!.contains("postStartupActivity") || xml.contains("backgroundPostStartupActivity")
        )
    }
}
