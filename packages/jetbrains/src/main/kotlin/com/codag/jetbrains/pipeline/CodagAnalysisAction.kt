package com.codag.jetbrains.pipeline

import com.codag.jetbrains.CodagConstants
import com.codag.jetbrains.api.CodagApiClient
import com.codag.jetbrains.dto.AnalyzeResponse
import com.codag.jetbrains.settings.CodagSettings
import com.codag.jetbrains.webview.CodagWebViewPanel
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.google.gson.Gson

/**
 * IDE Action: Analyse selected files with the Codag backend.
 *
 * Flow: read files → check cache → call API → push graph to webview.
 * Available via Ctrl+Shift+G, Project View context menu, and Editor context menu.
 */
class CodagAnalysisAction : AnAction() {

    private val log = Logger.getInstance(CodagAnalysisAction::class.java)
    private val cache = AnalysisCacheHelper()
    private val gson = Gson()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val supportedFiles = files.filter {
            !it.isDirectory && CodagConstants.isSupportedExtension(it.name)
        }

        if (supportedFiles.isEmpty()) {
            log.info("No supported files selected for Codag analysis")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Codag: Analyzing workflow...", true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.1
                indicator.text = "Reading files..."

                val fileMap = readFileContents(supportedFiles)
                if (fileMap.isEmpty()) return

                val filesHash = AnalysisPipelineHelper.computeFilesHash(fileMap)
                val cached = cache.get(filesHash)
                if (cached != null) {
                    log.info("Cache hit for analysis (hash=$filesHash)")
                    indicator.fraction = 1.0
                    pushGraphToWebView(project, cached)
                    return
                }

                indicator.fraction = 0.3
                indicator.text = "Building analysis request..."

                val settings = CodagSettings.getInstance().state
                val request = AnalysisPipelineHelper.buildAnalyzeRequest(fileMap)

                indicator.fraction = 0.5
                indicator.text = "Calling Codag backend..."

                val client = CodagApiClient(settings)
                val result = client.analyzeWorkflow(request)

                result.fold(
                    onSuccess = { response ->
                        indicator.fraction = 0.9
                        indicator.text = "Rendering graph..."

                        cache.put(filesHash, response)
                        pushGraphToWebView(project, response)

                        val nodeCount = response.graph.nodes.size
                        val edgeCount = response.graph.edges.size
                        log.info("Analysis complete: $nodeCount nodes, $edgeCount edges")
                        notify(project, "Analysis complete — $nodeCount nodes, $edgeCount edges", NotificationType.INFORMATION)
                    },
                    onFailure = { error ->
                        log.warn("Analysis failed: ${error.message}")
                        notify(project, "Analysis failed: ${error.message}", NotificationType.ERROR)
                    }
                )

                indicator.fraction = 1.0
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = files != null && files.any {
            !it.isDirectory && CodagConstants.isSupportedExtension(it.name)
        }
    }

    // ── Internals ──────────────────────────────────────────────────────

    private fun readFileContents(files: List<VirtualFile>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (file in files) {
            try {
                result[file.path] = String(file.contentsToByteArray(), Charsets.UTF_8)
            } catch (ex: Exception) {
                log.warn("Failed to read file: ${file.path}", ex)
            }
        }
        return result
    }

    /**
     * Retrieve the [CodagWebViewPanel] via the companion registry and push graph data.
     * Opens the tool window if it was not already visible.
     */
    private fun pushGraphToWebView(project: Project, response: AnalyzeResponse) {
        val graphJson = gson.toJson(response.graph)

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            // Ensure the tool window is visible (creates the panel if needed)
            val toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(CodagConstants.TOOL_WINDOW_ID) ?: return@invokeLater
            toolWindow.show()

            // Look up the panel through the companion registry (not content.component)
            val panel = CodagWebViewPanel.forProject(project)
            if (panel != null) {
                panel.updateGraph(graphJson)
            } else {
                log.warn("CodagWebViewPanel not found for project — graph not rendered")
            }
        }
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(CodagConstants.NOTIFICATION_GROUP_ID)
                .createNotification(content, type)
                .notify(project)
        } catch (e: Exception) {
            // Notification group may not be registered yet — fall back to log
            log.info("Notification: $content")
        }
    }
}
