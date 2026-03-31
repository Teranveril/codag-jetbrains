package com.codag.jetbrains.pipeline

import com.codag.jetbrains.api.CodagApiClient
import com.codag.jetbrains.dto.AnalyzeResponse
import com.codag.jetbrains.settings.CodagSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.google.gson.Gson

/**
 * IDE Action: Analyze selected files with Codag backend.
 * Reads file contents, builds request, calls API, updates webview.
 */
class CodagAnalysisAction : AnAction() {

    private val log = Logger.getInstance(CodagAnalysisAction::class.java)
    private val cache = AnalysisCacheHelper()
    private val gson = Gson()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val supportedFiles = files.filter {
            !it.isDirectory && SourceNavigatorHelper.isSupportedLanguage(it.name)
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
                    sendToWebView(project, cached)
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
                        sendToWebView(project, response)
                        log.info("Analysis complete: ${response.graph.nodes.size} nodes, ${response.graph.edges.size} edges")
                    },
                    onFailure = { error ->
                        log.warn("Analysis failed: ${error.message}")
                    }
                )

                indicator.fraction = 1.0
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = files != null && files.any {
            !it.isDirectory && SourceNavigatorHelper.isSupportedLanguage(it.name)
        }
    }

    private fun readFileContents(files: List<VirtualFile>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (file in files) {
            try {
                val content = String(file.contentsToByteArray(), Charsets.UTF_8)
                result[file.path] = content
            } catch (ex: Exception) {
                log.warn("Failed to read file: ${file.path}", ex)
            }
        }
        return result
    }

    private fun sendToWebView(project: com.intellij.openapi.project.Project, response: AnalyzeResponse) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Codag") ?: return
        val graphJson = gson.toJson(response.graph)

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            toolWindow.show()
            val content = toolWindow.contentManager.contents.firstOrNull() ?: return@invokeLater
            val panel = content.component
            if (panel is com.codag.jetbrains.webview.CodagWebViewPanel) {
                panel.updateGraph(graphJson)
            }
        }
    }
}
