package com.codag.jetbrains.api

import com.codag.jetbrains.dto.*
import com.codag.jetbrains.settings.CodagSettingsState
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** Health-check response from the Codag backend. */
data class HealthResponse(
    val status: String,
    @SerializedName("api_key_status") val apiKeyStatus: String
)

/**
 * HTTP client for the Codag analysis backend.
 *
 * Uses plain [HttpURLConnection] to avoid extra dependencies.
 * All network calls are **blocking** — callers are expected to run
 * them on a background thread (e.g. inside [com.intellij.openapi.progress.Task.Backgroundable]).
 */
class CodagApiClient(config: CodagSettingsState) {

    val baseUrl: String = config.backendUrl.trimEnd('/')
    val timeoutMs: Long = config.timeoutMs
    private val gson = Gson()

    fun buildUrl(path: String): String = "$baseUrl$path"

    /** Ping /health and return structured status. */
    fun checkHealth(): Result<HealthResponse> = runCatching {
        val json = httpGet(buildUrl("/health"))
        gson.fromJson(json, HealthResponse::class.java)
    }

    /** Send files for workflow analysis and return the graph. */
    fun analyzeWorkflow(request: AnalyzeRequest): Result<AnalyzeResponse> = runCatching {
        val body = gson.toJson(AnalyzeRequestWire.from(request))
        val json = httpPost(buildUrl("/analyze"), body)
        val wire = gson.fromJson(json, AnalyzeResponseWire::class.java)
        wire.toDomain()
    }

    // ── HTTP primitives ────────────────────────────────────────────────

    private fun httpGet(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs.toInt()
        connection.readTimeout = timeoutMs.toInt()
        return try {
            if (connection.responseCode != 200) {
                throw IOException("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun httpPost(url: String, body: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = timeoutMs.toInt()
        connection.readTimeout = timeoutMs.toInt()
        connection.setRequestProperty("Content-Type", "application/json")
        return try {
            connection.outputStream.bufferedWriter().use { it.write(body) }
            if (connection.responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw IOException("HTTP ${connection.responseCode}: $errorBody")
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}

// ── Wire DTOs (JSON ↔ domain mapping) ──────────────────────────────────

/** Outgoing request shape matching the backend's expected JSON. */
private data class AnalyzeRequestWire(
    val code: String,
    @SerializedName("file_paths") val filePaths: List<String>,
    @SerializedName("framework_hint") val frameworkHint: String?,
    val metadata: List<FileMetadataWire>,
    @SerializedName("http_connections") val httpConnections: String?
) {
    companion object {
        fun from(r: AnalyzeRequest) = AnalyzeRequestWire(
            code = r.code,
            filePaths = r.filePaths,
            frameworkHint = r.frameworkHint,
            metadata = r.metadata.map { FileMetadataWire.from(it) },
            httpConnections = r.httpConnections
        )
    }
}

private data class FileMetadataWire(
    val file: String,
    val locations: List<LocationWire>,
    @SerializedName("relatedFiles") val relatedFiles: List<String>
) {
    companion object {
        fun from(fm: FileMetadata) = FileMetadataWire(
            file = fm.file,
            locations = fm.locations.map { LocationWire(it.line, it.type, it.description, it.function, it.variable) },
            relatedFiles = fm.relatedFiles
        )
    }
}

private data class LocationWire(
    val line: Int,
    val type: String,
    val description: String,
    val function: String,
    val variable: String?
)

/** Incoming response shape from the backend. */
private data class AnalyzeResponseWire(
    val graph: GraphWire,
    val usage: UsageWire?,
    val cost: CostWire?
) {
    fun toDomain() = AnalyzeResponse(
        graph = graph.toDomain(),
        usage = usage?.toDomain(),
        cost = cost?.toDomain()
    )
}

private data class GraphWire(
    val nodes: List<NodeWire> = emptyList(),
    val edges: List<EdgeWire> = emptyList(),
    @SerializedName("llms_detected") val llmsDetected: List<String> = emptyList(),
    val workflows: List<WorkflowWire> = emptyList()
) {
    fun toDomain() = WorkflowGraph(
        nodes = nodes.map { it.toDomain() },
        edges = edges.map { it.toDomain() },
        llmsDetected = llmsDetected,
        workflows = workflows.map { it.toDomain() }
    )
}

private data class NodeWire(
    val id: String,
    val label: String,
    val type: String,
    val description: String? = null,
    val source: SourceWire? = null,
    val model: String? = null,
    val temperature: Float? = null
) {
    fun toDomain() = GraphNode(id, label, type, description, source?.toDomain(), model, temperature)
}

private data class SourceWire(val file: String, val line: Int, val function: String? = null) {
    fun toDomain() = SourceLocation(file, line, function)
}

private data class EdgeWire(
    val source: String,
    val target: String,
    val label: String? = null,
    val payload: PayloadWire? = null,
    val condition: String? = null
) {
    fun toDomain() = GraphEdge(source, target, label, payload?.toDomain(), condition)
}

private data class PayloadWire(val name: String, val type: String, val description: String) {
    fun toDomain() = EdgePayload(name, type, description)
}

private data class WorkflowWire(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerializedName("nodeIds") val nodeIds: List<String> = emptyList(),
    val components: List<ComponentWire> = emptyList()
) {
    fun toDomain() = WorkflowMetadata(id, name, description, nodeIds, components.map { it.toDomain() })
}

private data class ComponentWire(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerializedName("nodeIds") val nodeIds: List<String> = emptyList()
) {
    fun toDomain() = ComponentMetadata(id, name, description, nodeIds)
}

private data class UsageWire(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int,
    @SerializedName("cached_tokens") val cachedTokens: Int = 0
) {
    fun toDomain() = TokenUsage(inputTokens, outputTokens, totalTokens, cachedTokens)
}

private data class CostWire(
    @SerializedName("input_cost") val inputCost: Double,
    @SerializedName("output_cost") val outputCost: Double,
    @SerializedName("total_cost") val totalCost: Double
) {
    fun toDomain() = CostData(inputCost, outputCost, totalCost)
}
