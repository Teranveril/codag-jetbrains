package com.codag.jetbrains.webview

import com.codag.jetbrains.watch.ThemeDetector
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

/**
 * JCEF WebView panel hosting the Codag graph visualization.
 * Loads the bundled webview-client with a message bridge shim.
 */
class CodagWebViewPanel(
    private val project: Project
) : Disposable {

    private val log = Logger.getInstance(CodagWebViewPanel::class.java)
    private val browser: JBCefBrowser = JBCefBrowser()
    private val jsQuery: JBCefJSQuery = JBCefJSQuery.create(browser as com.intellij.ui.jcef.JBCefBrowserBase)

    init {
        setupMessageBridge()
        loadWebView()
        Disposer.register(project, this)
    }

    fun getComponent(): JComponent = browser.component

    /**
     * Send a message from Kotlin to the webview JS.
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

    /**
     * Update the displayed graph with new data.
     */
    fun updateGraph(graphJson: String) {
        sendToWebView("updateGraph", mapOf("graph" to graphJson))
    }

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

        // Inject cefQuery bridge after page loads
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    injectCefQueryBridge()
                }
            }
        }, browser.cefBrowser)
    }

    private fun injectCefQueryBridge() {
        // Replace the shim's generic cefQuery with the actual JBCefJSQuery injection code
        val injection = jsQuery.inject("request")
        val script = """
            (function() {
                window.cefQuery = function(params) {
                    $injection
                };
                // Re-signal ready in case webview already sent webviewReady before bridge was set up
                if (window.codagBridge) {
                    window.codagBridge.postMessage({ command: 'webviewReady' });
                }
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)

        // Inject IDE theme
        applyIdeTheme()
    }

    private fun applyIdeTheme() {
        val schemeName = EditorColorsManager.getInstance().globalScheme.name
        val themeClass = ThemeDetector.resolveThemeClass(schemeName)
        val themeScript = ThemeDetector.generateThemeScript(themeClass)
        browser.cefBrowser.executeJavaScript(themeScript, browser.cefBrowser.url, 0)
    }

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

    private fun handleIncomingMessage(message: BridgeMessage) {
        when (message.command) {
            "webviewReady" -> {
                log.info("Codag WebView ready")
            }
            "openFile" -> {
                val file = message.getString("file") ?: return
                val line = message.getInt("line") ?: 1
                navigateToSource(file, line)
            }
            "nodeSelected" -> {
                log.debug("Node selected: ${message.getString("nodeId")}")
            }
            "nodeDeselected" -> {
                log.debug("Node deselected")
            }
            "retryAnalysis" -> {
                log.info("Retry analysis requested")
            }
            "openAnalyzePanel" -> {
                log.info("Open analyze panel requested")
            }
            else -> {
                log.debug("Unhandled webview message: ${message.command}")
            }
        }
    }

    private fun navigateToSource(filePath: String, line: Int) {
        val baseDir = project.basePath ?: return
        val fullPath = if (filePath.startsWith("/")) filePath else "$baseDir/$filePath"
        val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            .findFileByPath(fullPath) ?: run {
            log.warn("File not found for navigation: $fullPath")
            return
        }
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            com.intellij.openapi.fileEditor.OpenFileDescriptor(
                project, virtualFile, maxOf(0, line - 1), 0
            ).navigate(true)
        }
    }

    override fun dispose() {
        // JBCefBrowser is auto-disposed via Disposer chain
    }
}
