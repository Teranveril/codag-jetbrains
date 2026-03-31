package com.codag.jetbrains.pipeline

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for CodagAnalysisAction registration in plugin.xml.
 */
class CodagAnalysisActionTest {

    @Test
    fun testActionRegisteredInPluginXml() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull("plugin.xml must exist", xml)
        assertTrue(
            "Action must be registered",
            xml!!.contains("com.codag.jetbrains.AnalyzeFiles")
        )
    }

    @Test
    fun testActionClassExists() {
        val clazz = Class.forName("com.codag.jetbrains.pipeline.CodagAnalysisAction")
        assertNotNull(clazz)
        assertTrue(
            "Must extend AnAction",
            com.intellij.openapi.actionSystem.AnAction::class.java.isAssignableFrom(clazz)
        )
    }

    @Test
    fun testActionHasKeyboardShortcut() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull(xml)
        assertTrue(
            "Action must have keyboard shortcut",
            xml!!.contains("keyboard-shortcut")
        )
        assertTrue(
            "Shortcut must be Ctrl+Shift+G",
            xml.contains("ctrl shift G")
        )
    }

    @Test
    fun testActionAddedToContextMenus() {
        val xml = javaClass.classLoader.getResource("META-INF/plugin.xml")?.readText()
        assertNotNull(xml)
        assertTrue(
            "Action must be in ProjectView context menu",
            xml!!.contains("ProjectViewPopupMenu")
        )
        assertTrue(
            "Action must be in Editor context menu",
            xml.contains("EditorPopupMenu")
        )
    }
}
