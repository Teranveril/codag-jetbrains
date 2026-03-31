package com.codag.jetbrains.pipeline

/**
 * Pure utility for source file navigation helpers.
 * No IDE dependencies — fully testable.
 */
object SourceNavigatorHelper {

    private val SUPPORTED_LANGUAGE_EXTENSIONS = setOf(
        "py", "ts", "tsx", "js", "jsx", "kt", "kts",
        "java", "go", "rs", "rb", "php", "swift", "scala",
        "c", "cpp", "h", "hpp", "cs", "vue", "svelte"
    )

    fun clampLine(line: Int, totalLines: Int): Int {
        return line.coerceIn(1, maxOf(1, totalLines))
    }

    fun resolveFilePath(basePath: String, filePath: String): String {
        if (filePath.startsWith("/")) {
            return filePath
        }
        val base = basePath.trimEnd('/')
        return "$base/$filePath"
    }

    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot < 0 || lastDot == fileName.length - 1) return ""
        return fileName.substring(lastDot + 1)
    }

    fun isSupportedLanguage(fileName: String): Boolean {
        val ext = getExtension(fileName).lowercase()
        return ext in SUPPORTED_LANGUAGE_EXTENSIONS
    }
}
