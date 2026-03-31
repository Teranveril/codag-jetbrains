package com.codag.jetbrains.webview

import com.codag.jetbrains.CodagConstants
import com.codag.jetbrains.watch.ThemeDetector
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JComponent

/**
 * JCEF WebView panel hosting the Codag graph visualization.
 *
 * Loads the bundled webview-client (index.html + main.js + d3.js) inside
 * a JBCefBrowser and establishes a bidirectional message bridge:
 *   - **Kotlin → JS**: [sendToWebView] serialises a JSON command and calls
 *     `window.__codagDispatch(json)` via [CefBrowser.executeJavaScript].
 *   - **JS → Kotlin**: The JCEF shim calls `cefQuery` which is routed to
 *     [JBCefJSQuery] and handled by [handleIncomingMessage].
 *
 * Retrieve the panel for a given project via [forProject].
 */
class CodagWebViewPanel(
    private val project: Project
) : Disposable {

    private val log = Logger.getInstance(CodagWebViewPanel::class.java)
    private val browser: JBCefBrowser = JBCefBrowser()
    private val jsQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    init {
        registry[project] = WeakReference(this)
        setupMessageBridge()
        loadWebView()
        Disposer.register(project, this)
    }

    /** Swing component to embed in a ToolWindow content tab. */
    fun getComponent(): JComponent = browser.component

    /**
     * Send a JSON command from Kotlin to the webview JavaScript layer.
     *
     * @param command  One of [CodagConstants] CMD_* constants.
     * @param fields   Optional key-value payload merged into the JSON message.
     */
    fun sendToWebView(command: String, fields: Map<String, Any>? = null) {
        val json = CodagMessageBridge.createOutgoingMessage(command, fields)
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
        browser.cefBrowser.executeJavaScript(
            "window.__codagDispatch && window.__codagDispatch('$escaped')",
            browser.cefBrowser.url,
            0
        )
    }

    /** Push a new workflow graph to the webview for rendering. */
    fun updateGraph(graphJson: String) {
        sendToWebView(CodagConstants.CMD_UPDATE_GRAPH, mapOf("graph" to graphJson))
    }

    // ── Message bridge setup ───────────────────────────────────────────

    private fun setupMessageBridge() {
        jsQuery.addHandler { request ->
            try {
                val message = CodagMessageBridge.parseIncomingMessage(request)
                handleIncomingMessage(message)
                JBCefJSQuery.Response("ok")
            } catch (e: Exception) {
                log.warn("Failed to handle webview message: ${e.message}", e)
                JBCefJSQuery.Response(null, 0, "Error: ${e.message}")
            }
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    injectCefQueryBridge()
                }
            }
        }, browser.cefBrowser)
    }

    /**
     * Replace the shim's placeholder `cefQuery` with the real [JBCefJSQuery]
     * injection code, then re-signal readiness and apply the IDE theme.
     */
    private fun injectCefQueryBridge() {
        val injection = jsQuery.inject("request")
        val script = """
            (function() {
                window.cefQuery = function(params) {
                    $injection
                };
                if (window.codagBridge) {
                    window.codagBridge.postMessage({ command: '${CodagConstants.CMD_WEBVIEW_READY}' });
                }
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        applyIdeTheme()
    }

    private fun applyIdeTheme() {
        val schemeName = EditorColorsManager.getInstance().globalScheme.name
        val themeClass = ThemeDetector.resolveThemeClass(schemeName)
        val themeScript = ThemeDetector.generateThemeScript(themeClass)
        browser.cefBrowser.executeJavaScript(themeScript, browser.cefBrowser.url, 0)
    }

    // ── WebView loading ────────────────────────────────────────────────

    private fun loadWebView() {
        val baseUrl = CodagHtmlProvider.getResourceBaseUrl()
        if (baseUrl != null) {
            val html = CodagHtmlProvider.generateHtml()
            browser.loadHTML(html, baseUrl)
        } else {
            log.error("WebView resources not found — cannot load Codag panel")
            browser.loadHTML("<html><body><h2>Codag: WebView resources not found</h2></body></html>")
        }
    }

    // ── Incoming message dispatch ──────────────────────────────────────

    private fun handleIncomingMessage(message: BridgeMessage) {
        when (message.command) {
            CodagConstants.CMD_WEBVIEW_READY -> {
                log.info("Codag WebView ready")
            }
            CodagConstants.CMD_OPEN_FILE -> {
                val file = message.getString("file") ?: return
                val line = message.getInt("line") ?: 1
                navigateToSource(file, line)
            }
            CodagConstants.CMD_NODE_SELECTED -> {
                log.debug("Node selected: ${message.getString("nodeId")}")
            }
            CodagConstants.CMD_NODE_DESELECTED -> {
                log.debug("Node deselected")
            }
            CodagConstants.CMD_RETRY_ANALYSIS -> {
                log.info("Retry analysis requested from webview")
            }
            CodagConstants.CMD_OPEN_ANALYZE_PANEL -> {
                log.info("Open analyze panel requested from webview")
            }
            else -> {
                log.debug("Unhandled webview message: ${message.command}")
            }
        }
    }

    // ── Source navigation ──────────────────────────────────────────────

    private fun navigateToSource(filePath: String, line: Int) {
        val baseDir = project.basePath ?: return
        val fullPath = if (filePath.startsWith("/")) filePath else "$baseDir/$filePath"
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath) ?: run {
            log.warn("File not found for navigation: $fullPath")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            OpenFileDescriptor(project, virtualFile, maxOf(0, line - 1), 0).navigate(true)
        }
    }

    override fun dispose() {
        registry.remove(project)
    }

    companion object {
        /**
         * Weak registry of active panels keyed by project.
         * Allows [CodagAnalysisAction] and other callers to retrieve the panel
         * without relying on ToolWindow content introspection.
         */
        private val registry = ConcurrentHashMap<Project, WeakReference<CodagWebViewPanel>>()

        /** Get the active panel for [project], or `null` if the tool window was never opened. */
        fun forProject(project: Project): CodagWebViewPanel? = registry[project]?.get()
    }
}
