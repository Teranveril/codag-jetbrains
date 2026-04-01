package com.codag.jetbrains.pipeline

import com.codag.jetbrains.dto.AnalyzeResponse
import com.codag.jetbrains.dto.GraphNode
import com.codag.jetbrains.dto.GraphEdge
import com.codag.jetbrains.dto.WorkflowGraph
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for AnalysisCacheHelper.
 * In-memory hash-based cache for analysis results.
 */
class CodagCacheServiceTest {

    private lateinit var cache: AnalysisCacheHelper

    private val sampleGraph = WorkflowGraph(
        nodes = listOf(GraphNode(id = "n1", label = "Node 1", type = "function")),
        edges = listOf(GraphEdge(source = "n1", target = "n1")),
        llmsDetected = emptyList(),
        workflows = emptyList()
    )

    private val sampleResponse = AnalyzeResponse(graph = sampleGraph)

    @Before
    fun setUp() {
        cache = AnalysisCacheHelper(maxSize = 10)
    }

    @Test
    fun testCacheMissReturnsNull() {
        val result = cache.get("nonexistent-hash")
        assertNull(result)
    }

    @Test
    fun testCacheHitReturnsStoredResponse() {
        val hash = "abc123"
        cache.put(hash, sampleResponse)

        val result = cache.get(hash)
        assertNotNull(result)
        assertEquals(1, result!!.graph.nodes.size)
        assertEquals("n1", result.graph.nodes[0].id)
    }

    @Test
    fun testCacheInvalidationOnHashChange() {
        cache.put("hash_v1", sampleResponse)
        assertNotNull(cache.get("hash_v1"))

        // Different hash = different content = cache miss
        assertNull(cache.get("hash_v2"))
    }

    @Test
    fun testCacheClearRemovesAll() {
        cache.put("hash1", sampleResponse)
        cache.put("hash2", sampleResponse)

        cache.clear()

        assertNull(cache.get("hash1"))
        assertNull(cache.get("hash2"))
        assertEquals(0, cache.size())
    }

    @Test
    fun testCacheSizeTracking() {
        assertEquals(0, cache.size())

        cache.put("hash1", sampleResponse)
        assertEquals(1, cache.size())

        cache.put("hash2", sampleResponse)
        assertEquals(2, cache.size())
    }

    @Test
    fun testCacheEvictsOldestWhenFull() {
        val smallCache = AnalysisCacheHelper(maxSize = 2)

        smallCache.put("hash1", sampleResponse)
        smallCache.put("hash2", sampleResponse)
        smallCache.put("hash3", sampleResponse) // Should evict hash1

        assertNull(smallCache.get("hash1"))
        assertNotNull(smallCache.get("hash2"))
        assertNotNull(smallCache.get("hash3"))
        assertEquals(2, smallCache.size())
    }

    @Test
    fun testCacheContainsKey() {
        cache.put("existing", sampleResponse)

        assertTrue(cache.contains("existing"))
        assertFalse(cache.contains("missing"))
    }
}
