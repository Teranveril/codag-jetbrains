package com.codag.jetbrains

/**
 * Centralised constants for the Codag plugin.
 * Eliminates magic strings scattered across modules.
 */
object CodagConstants {

    // ── Tool Window ────────────────────────────────────────────────────
    const val TOOL_WINDOW_ID = "Codag"
    const val TOOL_WINDOW_TAB = "Graph"

    // ── Bridge Commands (Kotlin → JS) ──────────────────────────────────
    const val CMD_UPDATE_GRAPH = "updateGraph"
    const val CMD_SHOW_PROGRESS = "showProgress"
    const val CMD_HIDE_PROGRESS = "hideProgress"
    const val CMD_SHOW_ERROR = "showError"

    // ── Bridge Commands (JS → Kotlin) ──────────────────────────────────
    const val CMD_WEBVIEW_READY = "webviewReady"
    const val CMD_OPEN_FILE = "openFile"
    const val CMD_NODE_SELECTED = "nodeSelected"
    const val CMD_NODE_DESELECTED = "nodeDeselected"
    const val CMD_RETRY_ANALYSIS = "retryAnalysis"
    const val CMD_OPEN_ANALYZE_PANEL = "openAnalyzePanel"

    // ── Notification ───────────────────────────────────────────────────
    const val NOTIFICATION_GROUP_ID = "Codag Notifications"

    // ── Supported Source File Extensions ────────────────────────────────
    val SUPPORTED_EXTENSIONS: Set<String> = setOf(
        "py", "ts", "tsx", "js", "jsx", "kt", "kts",
        "java", "go", "rs", "rb", "php", "swift", "scala",
        "c", "cpp", "h", "hpp", "cs", "vue", "svelte"
    )

    fun isSupportedExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_EXTENSIONS
    }

    // ── File Concatenation ─────────────────────────────────────────────
    const val FILE_SEPARATOR = "\n\n// --- FILE: %s ---\n\n"
}
