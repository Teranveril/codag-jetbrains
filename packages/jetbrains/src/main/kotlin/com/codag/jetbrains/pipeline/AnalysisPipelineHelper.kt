package com.codag.jetbrains.pipeline

import com.codag.jetbrains.CodagConstants
import com.codag.jetbrains.dto.AnalyzeRequest
import java.security.MessageDigest

/**
 * Pure utility for building analysis requests from file maps.
 * No IDE dependencies — fully testable.
 */
object AnalysisPipelineHelper {

    fun buildAnalyzeRequest(
        files: Map<String, String>,
        frameworkHint: String? = null
    ): AnalyzeRequest {
        if (files.isEmpty()) {
            return AnalyzeRequest(code = "", filePaths = emptyList(), frameworkHint = frameworkHint)
        }

        val sortedFiles = files.toSortedMap()
        val code = sortedFiles.entries.joinToString("") { (path, content) ->
            String.format(CodagConstants.FILE_SEPARATOR, path) + content
        }

        return AnalyzeRequest(
            code = code,
            filePaths = sortedFiles.keys.toList(),
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
        return paths.filter { CodagConstants.isSupportedExtension(it.substringAfterLast('/')) }
    }
}
