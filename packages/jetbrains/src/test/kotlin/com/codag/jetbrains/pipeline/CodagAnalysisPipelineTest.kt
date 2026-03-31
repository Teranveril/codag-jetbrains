package com.codag.jetbrains.pipeline

import com.codag.jetbrains.dto.AnalyzeRequest
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for AnalysisPipelineHelper.
 * Pure utility — no IDE dependencies.
 */
class CodagAnalysisPipelineTest {

    @Test
    fun testBuildRequestFromSingleFile() {
        val files = mapOf("src/main.py" to "print('hello')")
        val request = AnalysisPipelineHelper.buildAnalyzeRequest(files)

        assertEquals(listOf("src/main.py"), request.filePaths)
        assertTrue(request.code.contains("print('hello')"))
    }

    @Test
    fun testBuildRequestFromMultipleFiles() {
        val files = mapOf(
            "src/a.py" to "def a(): pass",
            "src/b.py" to "def b(): pass"
        )
        val request = AnalysisPipelineHelper.buildAnalyzeRequest(files)

        assertEquals(2, request.filePaths.size)
        assertTrue(request.code.contains("def a(): pass"))
        assertTrue(request.code.contains("def b(): pass"))
    }

    @Test
    fun testBuildRequestConcatenatesWithSeparator() {
        val files = mapOf(
            "a.py" to "code_a",
            "b.py" to "code_b"
        )
        val request = AnalysisPipelineHelper.buildAnalyzeRequest(files)
        // Each file section should have file path marker
        assertTrue(request.code.contains("a.py"))
        assertTrue(request.code.contains("b.py"))
        // Code blocks separated, not merged
        assertNotEquals("code_acode_b", request.code)
    }

    @Test
    fun testBuildRequestEmptyFilesReturnsEmptyRequest() {
        val files = emptyMap<String, String>()
        val request = AnalysisPipelineHelper.buildAnalyzeRequest(files)

        assertTrue(request.filePaths.isEmpty())
        assertTrue(request.code.isEmpty())
    }

    @Test
    fun testComputeFileHashDeterministic() {
        val content = "def hello(): print('world')"
        val hash1 = AnalysisPipelineHelper.computeContentHash(content)
        val hash2 = AnalysisPipelineHelper.computeContentHash(content)

        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotEmpty())
    }

    @Test
    fun testComputeFileHashDifferentContent() {
        val hash1 = AnalysisPipelineHelper.computeContentHash("version_1")
        val hash2 = AnalysisPipelineHelper.computeContentHash("version_2")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testComputeFilesHashCombines() {
        val files = mapOf(
            "a.py" to "content_a",
            "b.py" to "content_b"
        )
        val hash = AnalysisPipelineHelper.computeFilesHash(files)

        assertTrue(hash.isNotEmpty())
        // Same input → same hash
        assertEquals(hash, AnalysisPipelineHelper.computeFilesHash(files))
    }

    @Test
    fun testBuildRequestWithFrameworkHint() {
        val files = mapOf("app.py" to "from fastapi import FastAPI")
        val request = AnalysisPipelineHelper.buildAnalyzeRequest(
            files,
            frameworkHint = "FastAPI"
        )

        assertEquals("FastAPI", request.frameworkHint)
    }

    @Test
    fun testFilterSupportedExtensions() {
        val paths = listOf(
            "src/main.py",
            "src/app.ts",
            "src/util.js",
            "README.md",
            "image.png",
            "data.json"
        )
        val filtered = AnalysisPipelineHelper.filterSupportedFiles(paths)

        assertTrue(filtered.contains("src/main.py"))
        assertTrue(filtered.contains("src/app.ts"))
        assertTrue(filtered.contains("src/util.js"))
        assertFalse(filtered.contains("image.png"))
    }
}
