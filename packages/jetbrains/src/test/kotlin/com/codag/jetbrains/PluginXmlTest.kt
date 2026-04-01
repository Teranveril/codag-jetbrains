package com.codag.jetbrains

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * TDD: Verify plugin.xml is valid and plugin loads correctly.
 * Written BEFORE implementation code — scaffold must pass these.
 */
class PluginXmlTest : BasePlatformTestCase() {

    fun testPluginXmlExists() {
        val pluginXml = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertNotNull("plugin.xml must exist in META-INF/", pluginXml)
    }

    fun testPluginXmlContainsId() {
        val content = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull("plugin.xml must be readable", content)
        assertTrue(
            "plugin.xml must contain plugin id",
            content!!.contains("<id>com.codag.jetbrains</id>")
        )
    }

    fun testPluginXmlDependsOnPlatform() {
        val content = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull(content)
        assertTrue(
            "plugin.xml must depend on com.intellij.modules.platform",
            content!!.contains("<depends>com.intellij.modules.platform</depends>")
        )
    }
}
