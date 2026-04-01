package com.codag.jetbrains.webview

import com.codag.jetbrains.CodagConstants
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Codag ToolWindow.
 * Registered in plugin.xml — creates the JCEF WebView panel on first activation.
 */
class CodagToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = CodagWebViewPanel(project)
        val content = ContentFactory.getInstance().createContent(
            panel.getComponent(),
            CodagConstants.TOOL_WINDOW_TAB,
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}
