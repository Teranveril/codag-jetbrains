package com.codag.jetbrains.webview

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for CodagHtmlProvider — generates JCEF-ready HTML with graph data injection.
 */
class CodagHtmlProviderTest {

    @Test
    fun testGeneratesHtmlWithEmptyGraphByDefault() {
        val html = CodagHtmlProvider.generateHtml()
        assertTrue(html.contains("__GRAPH_DATA__"))
        assertTrue(html.contains("\"nodes\":[]"))
    }

    @Test
    fun testInjectsGraphDataIntoHtml() {
        val graphJson = """{"nodes":[{"id":"n1","label":"Test Node"}],"edges":[],"llms_detected":["gemini"],"workflows":["main"]}"""
        val html = CodagHtmlProvider.generateHtml(graphJson)
        assertTrue("Graph data must be injected into HTML", html.contains("Test Node"))
        assertTrue(html.contains("gemini"))
    }

    @Test
    fun testHtmlLoadsShimBeforeMainJs() {
        val html = CodagHtmlProvider.generateHtml()
        val shimIdx = html.indexOf("codag-jcef-shim.js")
        val mainIdx = html.indexOf("main.js")
        assertTrue(shimIdx > 0 && mainIdx > 0)
        assertTrue("Shim must load before main.js", shimIdx < mainIdx)
    }

    @Test
    fun testHtmlContainsRequiredStructuralElements() {
        val html = CodagHtmlProvider.generateHtml()
        assertTrue(html.contains("id=\"graph\""))
        assertTrue(html.contains("id=\"sidePanel\""))
        assertTrue(html.contains("id=\"minimap\""))
        assertTrue(html.contains("d3.v7.min.js"))
    }

    @Test
    fun testHtmlUsesFileProtocolUrlsForJcef() {
        val html = CodagHtmlProvider.generateHtml()
        assertFalse(html.contains("vscode-webview://"))
        assertFalse(html.contains("{{cspSource}}"))
        assertFalse(html.contains("{{nonce}}"))
    }

    @Test
    fun testEscapesSpecialCharactersInGraphJson() {
        val graphJson = """{"nodes":[{"id":"n1","label":"Test</script>XSS"}],"edges":[],"llms_detected":[],"workflows":[]}"""
        val html = CodagHtmlProvider.generateHtml(graphJson)
        assertFalse(html.contains("</script>XSS"))
    }
}
