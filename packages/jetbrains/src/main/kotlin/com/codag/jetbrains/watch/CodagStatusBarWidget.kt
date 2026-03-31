package com.codag.jetbrains.watch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import javax.swing.Icon

/**
 * Status bar widget showing Codag backend connectivity status.
 */
class CodagStatusBarWidgetFactory : StatusBarWidgetFactory {

    companion object {
        const val WIDGET_ID = "CodagStatusBar"
    }

    override fun getId(): String = WIDGET_ID
    override fun getDisplayName(): String = "Codag Backend Status"
    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return CodagStatusBarWidget(project)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class CodagStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null
    val monitor = BackendHealthMonitor()

    init {
        monitor.onStateChange { _ ->
            statusBar?.updateWidget(ID())
        }
    }

    override fun ID(): String = CodagStatusBarWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getText(): String = "Codag: ${monitor.statusText}"

    override fun getTooltipText(): String = when (monitor.currentState) {
        BackendHealthMonitor.State.CONNECTED -> "Codag backend is connected"
        BackendHealthMonitor.State.DISCONNECTED -> "Codag backend is not reachable"
        BackendHealthMonitor.State.UNKNOWN -> "Codag backend status unknown"
    }

    override fun getAlignment(): Float = 0f

    override fun dispose() {
        statusBar = null
    }
}
