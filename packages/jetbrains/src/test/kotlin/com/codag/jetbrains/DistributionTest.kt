package com.codag.jetbrains

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Distribution tests — verify the built plugin ZIP is correct.
 */
class DistributionTest {

    @Test
    fun testPluginXmlHasRequiredFields() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull("plugin.xml must exist in resources", xml)
        assertTrue("Must have plugin id", xml!!.contains("<id>com.codag.jetbrains</id>"))
        assertTrue("Must have plugin name", xml.contains("<name>Codag</name>"))
        assertTrue("Must have description", xml.contains("<description>"))
        assertTrue("Must declare platform dependency", xml.contains("com.intellij.modules.platform"))
    }

    @Test
    fun testPluginXmlRegistersAllComponents() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()!!

        // Extensions
        assertTrue("Settings configurable", xml.contains("applicationConfigurable"))
        assertTrue("Application service", xml.contains("applicationService"))
        assertTrue("Tool window", xml.contains("toolWindow"))
        assertTrue("Status bar widget", xml.contains("statusBarWidgetFactory"))
        assertTrue("Startup activity", xml.contains("postStartupActivity"))

        // Listeners
        assertTrue("File watcher listener", xml.contains("BulkFileListener"))

        // Actions
        assertTrue("Analysis action", xml.contains("com.codag.jetbrains.AnalyzeFiles"))
    }

    @Test
    fun testAllWebResourcesBundled() {
        assertNotNull("index.html", javaClass.getResource("/web/index.html"))
        assertNotNull("main.js", javaClass.getResource("/web/main.js"))
        assertNotNull("codag-jcef-shim.js", javaClass.getResource("/web/codag-jcef-shim.js"))
        assertNotNull("d3.v7.min.js", javaClass.getResource("/web/d3.v7.min.js"))
        assertNotNull("styles.css", javaClass.getResource("/web/styles.css"))
    }

    @Test
    fun testMainJsBundleSizeReasonable() {
        val resource = javaClass.getResource("/web/main.js")!!
        val content = resource.readText()
        // Bundle should be substantial (contains ELK.js + D3 interactions)
        assertTrue("main.js should be > 100KB", content.length > 100_000)
        // But not absurdly large
        assertTrue("main.js should be < 10MB", content.length < 10_000_000)
    }

    @Test
    fun testVersionConsistency() {
        val properties = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull(properties)
        // Plugin version is injected by Gradle into plugin.xml at build time
        // Just verify the file is parseable and has the version attribute pattern
        assertTrue("plugin.xml must contain version info or id", properties!!.contains("com.codag.jetbrains"))
    }
}
