package com.codag.jetbrains.webview

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for WebView resources bundling.
 * Verifies all required web assets are present in resources/web/.
 */
class WebViewResourcesTest {

    private val webResourcePath = "/web"

    @Test
    fun testIndexHtmlExistsInResources() {
        val resource = javaClass.getResource("$webResourcePath/index.html")
        assertNotNull("index.html must be bundled in resources/web/", resource)
    }

    @Test
    fun testMainJsBundleExistsInResources() {
        val resource = javaClass.getResource("$webResourcePath/main.js")
        assertNotNull("main.js (webview-client bundle) must be bundled in resources/web/", resource)
    }

    @Test
    fun testD3LibraryExistsInResources() {
        val resource = javaClass.getResource("$webResourcePath/d3.v7.min.js")
        assertNotNull("d3.v7.min.js must be bundled in resources/web/", resource)
    }

    @Test
    fun testStylesCssExistsInResources() {
        val resource = javaClass.getResource("$webResourcePath/styles.css")
        assertNotNull("styles.css must be bundled in resources/web/", resource)
    }

    @Test
    fun testJcefShimExistsInResources() {
        val resource = javaClass.getResource("$webResourcePath/codag-jcef-shim.js")
        assertNotNull("codag-jcef-shim.js must be bundled in resources/web/", resource)
    }

    @Test
    fun testIndexHtmlContainsShimScriptTagBeforeMainJs() {
        val html = javaClass.getResource("$webResourcePath/index.html")!!.readText()
        val shimIndex = html.indexOf("codag-jcef-shim.js")
        val mainIndex = html.indexOf("main.js")
        assertTrue("index.html must reference codag-jcef-shim.js", shimIndex > 0)
        assertTrue("index.html must reference main.js", mainIndex > 0)
        assertTrue("shim must load BEFORE main.js", shimIndex < mainIndex)
    }

    @Test
    fun testIndexHtmlContainsGraphDataPlaceholder() {
        val html = javaClass.getResource("$webResourcePath/index.html")!!.readText()
        assertTrue("index.html must contain __GRAPH_DATA__ injection point", html.contains("__GRAPH_DATA__"))
    }

    @Test
    fun testIndexHtmlContainsRequiredDomElements() {
        val html = javaClass.getResource("$webResourcePath/index.html")!!.readText()
        assertTrue("Must have #graph container", html.contains("id=\"graph\""))
        assertTrue("Must have #sidePanel for node details", html.contains("id=\"sidePanel\""))
        assertTrue("Must have #minimap", html.contains("id=\"minimap\""))
        assertTrue("Must have #controls", html.contains("id=\"controls\""))
    }

    @Test
    fun testShimJsDefinesAcquireVsCodeApiOverride() {
        val shimJs = javaClass.getResource("$webResourcePath/codag-jcef-shim.js")!!.readText()
        assertTrue("Shim must override acquireVsCodeApi", shimJs.contains("acquireVsCodeApi"))
        assertTrue("Shim must define __codagDispatch for Kotlin→JS messages", shimJs.contains("__codagDispatch"))
        assertTrue("Shim must use cefQuery for JS→Kotlin messages", shimJs.contains("cefQuery"))
    }
}
