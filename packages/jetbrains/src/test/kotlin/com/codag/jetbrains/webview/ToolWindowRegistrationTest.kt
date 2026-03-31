package com.codag.jetbrains.webview

import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

/**
 * TDD tests for ToolWindow registration in plugin.xml.
 */
class ToolWindowRegistrationTest {

    @Test
    fun testPluginXmlRegistersToolWindowFactory() {
        val pluginXml = javaClass.getResource("/META-INF/plugin.xml")!!.readText()
        assertTrue(
            "plugin.xml must register CodagToolWindowFactory",
            pluginXml.contains("com.codag.jetbrains.webview.CodagToolWindowFactory")
        )
    }

    @Test
    fun testToolWindowHasCorrectAttributes() {
        val inputStream = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val toolWindows = doc.getElementsByTagName("toolWindow")

        var found = false
        for (i in 0 until toolWindows.length) {
            val tw = toolWindows.item(i)
            val factoryClass = tw.attributes.getNamedItem("factoryClass")?.nodeValue
            if (factoryClass == "com.codag.jetbrains.webview.CodagToolWindowFactory") {
                found = true
                val id = tw.attributes.getNamedItem("id")?.nodeValue
                assertEquals("ToolWindow id must be 'Codag'", "Codag", id)

                val anchor = tw.attributes.getNamedItem("anchor")?.nodeValue
                assertEquals("ToolWindow should anchor to right panel", "right", anchor)
            }
        }
        assertTrue("CodagToolWindowFactory must be registered in plugin.xml", found)
    }
}
