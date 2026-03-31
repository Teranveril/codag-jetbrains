package com.codag.jetbrains.watch

import com.codag.jetbrains.pipeline.SourceNavigatorHelper

/**
 * Filters file change events to decide whether re-analysis is needed.
 * Pure utility — testable without IDE.
 */
class FileChangeFilter {

    private val lastEventTimestamps = mutableMapOf<String, Long>()

    /**
     * Check if a file event should be debounced (too soon after last event for same path).
     */
    fun isDebouncedAt(path: String, timestampMs: Long): Boolean {
        val lastTs = lastEventTimestamps[path]
        lastEventTimestamps[path] = timestampMs
        return lastTs != null && (timestampMs - lastTs) < DEBOUNCE_MS
    }

    companion object {
        const val DEBOUNCE_MS = 1500L

        private val EXCLUDED_DIRS = setOf(
            "node_modules", ".git", "__pycache__", "build", "dist",
            ".venv", "venv", ".gradle", ".idea", "out", "target",
            ".mypy_cache", ".pytest_cache", ".ruff_cache"
        )

        fun shouldReanalyze(path: String): Boolean {
            if (!SourceNavigatorHelper.isSupportedLanguage(path.substringAfterLast('/'))) {
                return false
            }
            val segments = path.replace("\\", "/").split("/")
            return segments.none { it in EXCLUDED_DIRS }
        }

        fun filterBatchForReanalysis(paths: List<String>): List<String> {
            return paths.distinct().filter { shouldReanalyze(it) }
        }
    }
}
