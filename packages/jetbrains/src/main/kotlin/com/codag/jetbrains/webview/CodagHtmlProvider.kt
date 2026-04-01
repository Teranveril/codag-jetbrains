package com.codag.jetbrains.webview

/**
 * Generates JCEF-ready HTML for the Codag WebView panel.
 * Reads the template from resources/web/index.html and injects graph data.
 */
object CodagHtmlProvider {

    private const val EMPTY_GRAPH = """{"nodes":[],"edges":[],"llms_detected":[],"workflows":[]}"""
    private const val GRAPH_DATA_MARKER = """window.__GRAPH_DATA__ = {"nodes":[],"edges":[],"llms_detected":[],"workflows":[]};"""

    /**
     * Generate the full HTML content for JCEF with optional graph data injection.
     *
     * @param graphJson JSON string of the workflow graph data. If null, uses empty graph.
     * @return Complete HTML string ready for JBCefBrowser.loadHTML() or file-based loading.
     */
    fun generateHtml(graphJson: String? = null): String {
        val templateUrl = javaClass.getResource("/web/index.html")
            ?: throw IllegalStateException("WebView template not found at /web/index.html")

        var html = templateUrl.readText()

        // Resolve font URIs — CSS uses {{fontsUri}} placeholder from VS Code build.
        // In JCEF, relative paths work against the base URL so just point at fonts/.
        val fontsBase = getResourceBaseUrl()?.let { "${it}fonts" } ?: "fonts"
        html = html.replace("{{fontsUri}}", fontsBase)

        if (graphJson != null) {
            val escaped = CodagMessageBridge.escapeForScript(graphJson)
            val injection = "window.__GRAPH_DATA__ = $escaped;"
            html = html.replace(GRAPH_DATA_MARKER, injection)
        }

        return html
    }

    /**
     * Get the base URL for resolving relative resource paths in JCEF.
     * Derives from a known file since JAR classloaders don't resolve directories.
     */
    fun getResourceBaseUrl(): String? {
        val indexUrl = javaClass.getResource("/web/index.html") ?: return null
        return indexUrl.toExternalForm().removeSuffix("index.html")
    }
}
