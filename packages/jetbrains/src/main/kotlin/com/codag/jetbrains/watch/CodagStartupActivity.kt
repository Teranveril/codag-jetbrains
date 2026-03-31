package com.codag.jetbrains.watch

import com.codag.jetbrains.api.CodagApiClient
import com.codag.jetbrains.settings.CodagSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.WindowManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Runs after project opens: checks backend health and starts periodic monitoring.
 */
class CodagStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(CodagStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("Codag plugin initialized for project: ${project.name}")

        // Initial health check on background thread
        val settings = CodagSettings.getInstance().state
        val client = CodagApiClient(settings)

        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val widget = statusBar?.getWidget(CodagStatusBarWidgetFactory.WIDGET_ID)

        val monitor = if (widget is CodagStatusBarWidget) widget.monitor else null

        val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "codag-health-monitor").apply { isDaemon = true }
        }

        scheduler.scheduleWithFixedDelay({
            try {
                val result = client.checkHealth()
                result.fold(
                    onSuccess = { health ->
                        val apiValid = health.apiKeyStatus == "valid"
                        monitor?.reportHealthy(apiKeyValid = apiValid)
                        log.debug("Codag health check: status=${health.status}, apiKey=${health.apiKeyStatus}")
                    },
                    onFailure = {
                        monitor?.reportUnreachable()
                        log.debug("Codag health check failed: ${it.message}")
                    }
                )
            } catch (e: Exception) {
                monitor?.reportUnreachable()
                log.debug("Codag health check error: ${e.message}")
            }
        }, 0, 30, TimeUnit.SECONDS)
    }
}
