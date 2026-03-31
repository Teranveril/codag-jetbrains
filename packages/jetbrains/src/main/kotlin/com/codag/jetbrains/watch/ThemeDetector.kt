package com.codag.jetbrains.watch

/**
 * Maps IDE theme names to VS Code-compatible CSS classes for the webview.
 * Codag's webview-client uses vscode-dark / vscode-light / vscode-high-contrast.
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
            else -> "vscode-dark" // safe default
        }
    }

    fun generateThemeScript(themeClass: String): String {
        val removeAll = THEME_CLASSES.joinToString("; ") { "document.body.classList.remove('$it')" }
        return """
            (function() {
                $removeAll;
                document.body.classList.add('$themeClass');
            })();
        """.trimIndent()
    }
}
