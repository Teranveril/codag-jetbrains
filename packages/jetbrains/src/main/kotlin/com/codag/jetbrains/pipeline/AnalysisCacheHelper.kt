package com.codag.jetbrains.pipeline

import com.codag.jetbrains.dto.AnalyzeResponse

/**
 * In-memory hash-based cache for analysis results.
 * Uses LinkedHashMap with access-order for LRU eviction.
 */
class AnalysisCacheHelper(private val maxSize: Int = 50) {

    private val cache = object : LinkedHashMap<String, AnalyzeResponse>(
        maxSize, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnalyzeResponse>?): Boolean {
            return size > maxSize
        }
    }

    fun get(hash: String): AnalyzeResponse? = cache[hash]

    fun put(hash: String, response: AnalyzeResponse) {
        cache[hash] = response
    }

    fun contains(hash: String): Boolean = cache.containsKey(hash)

    fun clear() = cache.clear()

    fun size(): Int = cache.size
}
