package com.codag.jetbrains.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: Tests for API request/response DTOs.
 * Written BEFORE implementation.
 */
class ApiDtoTest {

    @Test
    fun testAnalyzeRequestMinimal() {
        val request = AnalyzeRequest(
            code = "print('hello')",
            filePaths = listOf("main.py")
        )
        assertEquals("print('hello')", request.code)
        assertEquals(listOf("main.py"), request.filePaths)
        assertNull(request.frameworkHint)
        assertTrue(request.metadata.isEmpty())
        assertNull(request.httpConnections)
    }

    @Test
    fun testAnalyzeRequestFull() {
        val location = LocationMetadata(
            line = 10,
            type = "llm_call",
            description = "Calls Gemini",
            function = "analyze",
            variable = "response"
        )
        val fileMeta = FileMetadata(
            file = "main.py",
            locations = listOf(location),
            relatedFiles = listOf("utils.py")
        )
        val request = AnalyzeRequest(
            code = "code here",
            filePaths = listOf("main.py"),
            frameworkHint = "langchain",
            metadata = listOf(fileMeta),
            httpConnections = "service_a -> service_b"
        )
        assertEquals("langchain", request.frameworkHint)
        assertEquals(1, request.metadata.size)
        assertEquals("response", request.metadata[0].locations[0].variable)
    }

    @Test
    fun testMetadataRequest() {
        val fn = FunctionContext(
            name = "analyze",
            line = 5,
            type = "llm",
            calls = listOf("parse", "validate"),
            code = "def analyze(): pass"
        )
        val fileCtx = FileStructureContext(
            filePath = "main.py",
            functions = listOf(fn),
            imports = listOf("os", "json")
        )
        val request = MetadataRequest(
            files = listOf(fileCtx),
            code = null
        )
        assertEquals(1, request.files.size)
        assertEquals(2, request.files[0].functions[0].calls.size)
    }

    @Test
    fun testFileMetadataResult() {
        val fnMeta = FunctionMetadataResult(
            name = "analyze",
            label = "Analyze Code",
            description = "Sends code to LLM"
        )
        val result = FileMetadataResult(
            filePath = "main.py",
            functions = listOf(fnMeta),
            edgeLabels = mapOf("analyze→parse" to "invokes")
        )
        assertEquals("main.py", result.filePath)
        assertEquals("invokes", result.edgeLabels["analyze→parse"])
    }
}
