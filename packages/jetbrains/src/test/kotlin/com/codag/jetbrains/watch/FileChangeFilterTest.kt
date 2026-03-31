package com.codag.jetbrains.watch

import com.codag.jetbrains.pipeline.SourceNavigatorHelper
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for FileChangeFilter — decides whether file changes warrant re-analysis.
 * Pure utility, no IDE dependencies.
 */
class FileChangeFilterTest {

    @Test
    fun testSupportedFileTriggersReanalysis() {
        assertTrue(FileChangeFilter.shouldReanalyze("src/main.py"))
        assertTrue(FileChangeFilter.shouldReanalyze("app/service.ts"))
        assertTrue(FileChangeFilter.shouldReanalyze("lib/util.kt"))
    }

    @Test
    fun testUnsupportedFileIgnored() {
        assertFalse(FileChangeFilter.shouldReanalyze("README.md"))
        assertFalse(FileChangeFilter.shouldReanalyze("image.png"))
        assertFalse(FileChangeFilter.shouldReanalyze(".gitignore"))
        assertFalse(FileChangeFilter.shouldReanalyze("package-lock.json"))
    }

    @Test
    fun testExcludedDirectoriesIgnored() {
        assertFalse(FileChangeFilter.shouldReanalyze("node_modules/lodash/index.js"))
        assertFalse(FileChangeFilter.shouldReanalyze(".git/hooks/pre-commit"))
        assertFalse(FileChangeFilter.shouldReanalyze("__pycache__/module.cpython-312.pyc"))
        assertFalse(FileChangeFilter.shouldReanalyze("build/classes/Main.class"))
        assertFalse(FileChangeFilter.shouldReanalyze(".venv/lib/site-packages/fastapi.py"))
    }

    @Test
    fun testDebouncingThresholdDefaultValue() {
        assertTrue(FileChangeFilter.DEBOUNCE_MS > 0)
        assertEquals(1500L, FileChangeFilter.DEBOUNCE_MS)
    }

    @Test
    fun testShouldDebounceWithinThreshold() {
        val filter = FileChangeFilter()
        val now = System.currentTimeMillis()

        // First event — should NOT debounce
        assertFalse(filter.isDebouncedAt("src/main.py", now))

        // Second event within threshold — should debounce
        assertTrue(filter.isDebouncedAt("src/main.py", now + 500))

        // Third event after threshold — should NOT debounce
        assertFalse(filter.isDebouncedAt("src/main.py", now + 2000))
    }

    @Test
    fun testDifferentFilesNotDebounced() {
        val filter = FileChangeFilter()
        val now = System.currentTimeMillis()

        assertFalse(filter.isDebouncedAt("src/a.py", now))
        assertFalse(filter.isDebouncedAt("src/b.py", now + 100))
    }

    @Test
    fun testBatchChangesCollectsUniquePaths() {
        val paths = listOf(
            "src/main.py",
            "src/main.py", // duplicate
            "src/util.py",
            "README.md",   // unsupported
            "src/app.ts"
        )
        val filtered = FileChangeFilter.filterBatchForReanalysis(paths)

        assertEquals(3, filtered.size)
        assertTrue(filtered.contains("src/main.py"))
        assertTrue(filtered.contains("src/util.py"))
        assertTrue(filtered.contains("src/app.ts"))
    }
}
