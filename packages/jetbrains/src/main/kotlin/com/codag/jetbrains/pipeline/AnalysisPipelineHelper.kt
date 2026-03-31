package com.codag.jetbrains.pipeline

import com.codag.jetbrains.dto.AnalyzeRequest
import java.security.MessageDigest

/**
 * Pure utility for building analysis requests from file maps.
 * No IDE dependencies — fully testable.
 */
object AnalysisPipelineHelper {

    private val SUPPORTED_EXTENSIONS = setOf(
        "py", "ts", "tsx", "js", "jsx", "kt", "kts",
        "java", "go", "rs", "rb", "php", "swift", "scala",
        "c", "cpp", "h", "hpp", "cs", "vue", "svelte"
    )

    private const val FILE_SEPARATOR = "\n\n// --- FILE: %s ---\n\n"

    fun buildAnalyzeRequest(
        files: Map<String, String>,
        frameworkHint: String? = null
    ): AnalyzeRequest {
        if (files.isEmpty()) {
            return AnalyzeRequest(
                code = "",
                filePaths = emptyList(),
                frameworkHint = frameworkHint
            )
        }

        val sortedFiles = files.toSortedMap()
        val code = sortedFiles.entries.joinToString("") { (path, content) ->
            String.format(FILE_SEPARATOR, path) + content
        }
        val filePaths = sortedFiles.keys.toList()

        return AnalyzeRequest(
            code = code,
            filePaths = filePaths,
            frameworkHint = frameworkHint
        )
    }

    fun computeContentHash(content: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun computeFilesHash(files: Map<String, String>): String {
        val combined = files.toSortedMap().entries.joinToString("\n") { (path, content) ->
            "$path:${computeContentHash(content)}"
        }
        return computeContentHash(combined)
    }

    fun filterSupportedFiles(paths: List<String>): List<String> {
        return paths.filter { path ->
            val ext = path.substringAfterLast('.', "").lowercase()
            ext in SUPPORTED_EXTENSIONS
        }
    }
}
