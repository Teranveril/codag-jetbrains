package com.codag.jetbrains.watch

/**
 * Maps IDE theme names to VS Code-compatible CSS classes and injects
 * CSS custom properties that the webview-client expects (--vscode-*).
 *
 * The original Codag web UI was built for VS Code which provides these
 * variables automatically.  In JCEF we must define them ourselves.
 */
object ThemeDetector {

    private val DARK_KEYWORDS = listOf("dark", "darcula", "monokai", "dracula", "one dark", "nord", "gruvbox dark", "solarized dark")
    private val LIGHT_KEYWORDS = listOf("light", "default", "intellij", "solarized light", "gruvbox light")
    private val HIGH_CONTRAST_KEYWORDS = listOf("high contrast", "high-contrast")

    private val THEME_CLASSES = listOf("vscode-dark", "vscode-light", "vscode-high-contrast")

    fun resolveThemeClass(themeName: String): String {
        val lower = themeName.lowercase()
        return when {
            HIGH_CONTRAST_KEYWORDS.any { lower.contains(it) } -> "vscode-high-contrast"
            LIGHT_KEYWORDS.any { lower.contains(it) } -> "vscode-light"
            DARK_KEYWORDS.any { lower.contains(it) } -> "vscode-dark"
            else -> "vscode-dark"
        }
    }

    /**
     * Generate JS that sets the body class AND injects CSS custom properties.
     */
    fun generateThemeScript(themeClass: String): String {
        val removeAll = THEME_CLASSES.joinToString("; ") { "document.body.classList.remove('$it')" }
        val cssVars = getCssVariables(themeClass)
        val cssBlock = cssVars.entries.joinToString("\\n") { (k, v) -> "  $k: $v;" }
        return """
            (function() {
                $removeAll;
                document.body.classList.add('$themeClass');

                var id = '__codag_theme_vars';
                var existing = document.getElementById(id);
                if (existing) existing.remove();
                var style = document.createElement('style');
                style.id = id;
                style.textContent = ':root {\n$cssBlock\n}';
                document.head.appendChild(style);
            })();
        """.trimIndent()
    }

    private fun getCssVariables(themeClass: String): Map<String, String> = when (themeClass) {
        "vscode-light" -> LIGHT_PALETTE
        "vscode-high-contrast" -> HIGH_CONTRAST_PALETTE
        else -> DARK_PALETTE
    }

    // ── Darcula / Dark themes ──────────────────────────────────────────
    private val DARK_PALETTE = mapOf(
        "--vscode-editor-background" to "#1e1e1e",
        "--vscode-editor-foreground" to "#d4d4d4",
        "--vscode-foreground" to "#cccccc",
        "--vscode-descriptionForeground" to "#9e9e9e",
        "--vscode-icon-foreground" to "#c5c5c5",
        "--vscode-panel-border" to "#2d2d2d",
        "--vscode-contrastBorder" to "#444444",
        "--vscode-focusBorder" to "#007fd4",
        "--vscode-textLink-foreground" to "#3794ff",
        "--vscode-button-background" to "#0e639c",
        "--vscode-button-foreground" to "#ffffff",
        "--vscode-button-hoverBackground" to "#1177bb",
        "--vscode-button-secondaryBackground" to "#3a3d41",
        "--vscode-input-background" to "#3c3c3c",
        "--vscode-input-foreground" to "#cccccc",
        "--vscode-input-placeholderForeground" to "#a0a0a0",
        "--vscode-toolbar-hoverBackground" to "#2a2d2e",
        "--vscode-list-hoverBackground" to "#2a2d2e",
        "--vscode-editor-inactiveSelectionBackground" to "#3a3d41",
        "--vscode-editorWidget-border" to "#454545",
        "--vscode-editorLineNumber-foreground" to "#858585",
        "--vscode-progressBar-background" to "#0e70c0",
        "--vscode-progressBar-foreground" to "#0e70c0",
        "--vscode-charts-green" to "#89d185",
        "--vscode-font-family" to "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
        "--vscode-editor-font-family" to "'JetBrains Mono', Menlo, Monaco, 'Courier New', monospace",
    )

    // ── IntelliJ Light themes ──────────────────────────────────────────
    private val LIGHT_PALETTE = mapOf(
        "--vscode-editor-background" to "#ffffff",
        "--vscode-editor-foreground" to "#1e1e1e",
        "--vscode-foreground" to "#333333",
        "--vscode-descriptionForeground" to "#717171",
        "--vscode-icon-foreground" to "#424242",
        "--vscode-panel-border" to "#d4d4d4",
        "--vscode-contrastBorder" to "#c8c8c8",
        "--vscode-focusBorder" to "#0078d4",
        "--vscode-textLink-foreground" to "#006ab1",
        "--vscode-button-background" to "#0078d4",
        "--vscode-button-foreground" to "#ffffff",
        "--vscode-button-hoverBackground" to "#106ebe",
        "--vscode-button-secondaryBackground" to "#e1e1e1",
        "--vscode-input-background" to "#ffffff",
        "--vscode-input-foreground" to "#1e1e1e",
        "--vscode-input-placeholderForeground" to "#a0a0a0",
        "--vscode-toolbar-hoverBackground" to "#e8e8e8",
        "--vscode-list-hoverBackground" to "#e8e8e8",
        "--vscode-editor-inactiveSelectionBackground" to "#e4e6f1",
        "--vscode-editorWidget-border" to "#c8c8c8",
        "--vscode-editorLineNumber-foreground" to "#999999",
        "--vscode-progressBar-background" to "#0078d4",
        "--vscode-progressBar-foreground" to "#0078d4",
        "--vscode-charts-green" to "#388a34",
        "--vscode-font-family" to "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
        "--vscode-editor-font-family" to "'JetBrains Mono', Menlo, Monaco, 'Courier New', monospace",
    )

    // ── High-contrast ──────────────────────────────────────────────────
    private val HIGH_CONTRAST_PALETTE = mapOf(
        "--vscode-editor-background" to "#000000",
        "--vscode-editor-foreground" to "#ffffff",
        "--vscode-foreground" to "#ffffff",
        "--vscode-descriptionForeground" to "#d4d4d4",
        "--vscode-icon-foreground" to "#ffffff",
        "--vscode-panel-border" to "#6fc3df",
        "--vscode-contrastBorder" to "#6fc3df",
        "--vscode-focusBorder" to "#f38518",
        "--vscode-textLink-foreground" to "#3794ff",
        "--vscode-button-background" to "#0078d4",
        "--vscode-button-foreground" to "#ffffff",
        "--vscode-button-hoverBackground" to "#1177bb",
        "--vscode-button-secondaryBackground" to "#333333",
        "--vscode-input-background" to "#000000",
        "--vscode-input-foreground" to "#ffffff",
        "--vscode-input-placeholderForeground" to "#a0a0a0",
        "--vscode-toolbar-hoverBackground" to "#333333",
        "--vscode-list-hoverBackground" to "#333333",
        "--vscode-editor-inactiveSelectionBackground" to "#333333",
        "--vscode-editorWidget-border" to "#6fc3df",
        "--vscode-editorLineNumber-foreground" to "#d4d4d4",
        "--vscode-progressBar-background" to "#0078d4",
        "--vscode-progressBar-foreground" to "#0078d4",
        "--vscode-charts-green" to "#89d185",
        "--vscode-font-family" to "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
        "--vscode-editor-font-family" to "'JetBrains Mono', Menlo, Monaco, 'Courier New', monospace",
    )
}
