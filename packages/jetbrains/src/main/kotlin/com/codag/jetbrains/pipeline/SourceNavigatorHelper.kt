package com.codag.jetbrains.pipeline

import com.codag.jetbrains.CodagConstants

/**
 * Pure utility for source file navigation helpers.
 * No IDE dependencies — fully testable.
 */
object SourceNavigatorHelper {

    fun clampLine(line: Int, totalLines: Int): Int {
        return line.coerceIn(1, maxOf(1, totalLines))
    }

    fun resolveFilePath(basePath: String, filePath: String): String {
        if (filePath.startsWith("/")) return filePath
        return "${basePath.trimEnd('/')}/$filePath"
    }

    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot < 0 || lastDot == fileName.length - 1) return ""
        return fileName.substring(lastDot + 1)
    }

    fun isSupportedLanguage(fileName: String): Boolean =
        CodagConstants.isSupportedExtension(fileName)
}
