package com.codag.jetbrains.pipeline

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for SourceNavigatorHelper.
 * Pure utility — no IDE dependencies for core logic.
 */
class CodagSourceNavigatorTest {

    @Test
    fun testClampLineNumberToValidRange() {
        // Line 5 in a 10-line file → 5
        assertEquals(5, SourceNavigatorHelper.clampLine(5, totalLines = 10))
    }

    @Test
    fun testClampLineNumberBelowMinimum() {
        // Line 0 or negative → 1 (minimum)
        assertEquals(1, SourceNavigatorHelper.clampLine(0, totalLines = 10))
        assertEquals(1, SourceNavigatorHelper.clampLine(-1, totalLines = 10))
    }

    @Test
    fun testClampLineNumberAboveMaximum() {
        // Line 100 in a 10-line file → 10
        assertEquals(10, SourceNavigatorHelper.clampLine(100, totalLines = 10))
    }

    @Test
    fun testResolveRelativePath() {
        val basePath = "/Users/dev/project"
        val filePath = "src/main.py"
        val resolved = SourceNavigatorHelper.resolveFilePath(basePath, filePath)

        assertEquals("/Users/dev/project/src/main.py", resolved)
    }

    @Test
    fun testResolveAbsolutePathPassthrough() {
        val basePath = "/Users/dev/project"
        val filePath = "/Users/dev/project/src/main.py"
        val resolved = SourceNavigatorHelper.resolveFilePath(basePath, filePath)

        assertEquals("/Users/dev/project/src/main.py", resolved)
    }

    @Test
    fun testResolvePathNormalizesSlashes() {
        val basePath = "/Users/dev/project/"
        val filePath = "src/main.py"
        val resolved = SourceNavigatorHelper.resolveFilePath(basePath, filePath)

        // Should not produce double slashes
        assertFalse(resolved.contains("//"))
    }

    @Test
    fun testExtractFileExtension() {
        assertEquals("py", SourceNavigatorHelper.getExtension("main.py"))
        assertEquals("ts", SourceNavigatorHelper.getExtension("app.component.ts"))
        assertEquals("", SourceNavigatorHelper.getExtension("Makefile"))
    }

    @Test
    fun testIsSupportedLanguage() {
        assertTrue(SourceNavigatorHelper.isSupportedLanguage("main.py"))
        assertTrue(SourceNavigatorHelper.isSupportedLanguage("app.ts"))
        assertTrue(SourceNavigatorHelper.isSupportedLanguage("util.js"))
        assertTrue(SourceNavigatorHelper.isSupportedLanguage("Main.kt"))
        assertTrue(SourceNavigatorHelper.isSupportedLanguage("Main.java"))
        assertFalse(SourceNavigatorHelper.isSupportedLanguage("image.png"))
        assertFalse(SourceNavigatorHelper.isSupportedLanguage("data.bin"))
    }
}
