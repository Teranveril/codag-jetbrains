package com.codag.jetbrains.api

import com.codag.jetbrains.dto.*
import com.codag.jetbrains.settings.CodagSettingsState
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class HealthResponse(
    val status: String,
    val apiKeyStatus: String
)

class CodagApiClient(config: CodagSettingsState) {

    val baseUrl: String = config.backendUrl.trimEnd('/')
    val timeoutMs: Long = config.timeoutMs

    fun buildUrl(path: String): String = "$baseUrl$path"

    fun checkHealth(): Result<HealthResponse> = runCatching {
        val response = httpGet(buildUrl("/health"))
        parseHealthResponse(response)
    }

    fun analyzeWorkflow(request: AnalyzeRequest): Result<AnalyzeResponse> = runCatching {
        val body = Gson().toJson(mapOf(
            "code" to request.code,
            "file_paths" to request.filePaths,
            "framework_hint" to request.frameworkHint,
            "metadata" to request.metadata.map { fm ->
                mapOf(
                    "file" to fm.file,
                    "locations" to fm.locations.map { loc ->
                        mapOf(
                            "line" to loc.line,
                            "type" to loc.type,
                            "description" to loc.description,
                            "function" to loc.function,
                            "variable" to loc.variable
                        )
                    },
                    "relatedFiles" to fm.relatedFiles
                )
            },
            "http_connections" to request.httpConnections
        ))
        val response = httpPost(buildUrl("/analyze"), body)
        parseAnalyzeResponse(response)
    }

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

    companion object {
        private val gson = Gson()

        fun parseHealthResponse(json: String): HealthResponse {
            val obj = JsonParser.parseString(json).asJsonObject
            return HealthResponse(
                status = obj.get("status").asString,
                apiKeyStatus = obj.get("api_key_status").asString
            )
        }

        fun parseAnalyzeResponse(json: String): AnalyzeResponse {
            val obj = JsonParser.parseString(json).asJsonObject
            val graphObj = obj.getAsJsonObject("graph")

            val nodes = graphObj.getAsJsonArray("nodes").map { nodeEl ->
                val n = nodeEl.asJsonObject
                GraphNode(
                    id = n.get("id").asString,
                    label = n.get("label").asString,
                    type = n.get("type").asString,
                    description = n.get("description")?.takeIf { !it.isJsonNull }?.asString,
                    source = n.getAsJsonObject("source")?.let { s ->
                        SourceLocation(
                            file = s.get("file").asString,
                            line = s.get("line").asInt,
                            function = s.get("function")?.takeIf { !it.isJsonNull }?.asString
                        )
                    },
                    model = n.get("model")?.takeIf { !it.isJsonNull }?.asString,
                    temperature = n.get("temperature")?.takeIf { !it.isJsonNull }?.asFloat
                )
            }

            val edges = graphObj.getAsJsonArray("edges").map { edgeEl ->
                val e = edgeEl.asJsonObject
                GraphEdge(
                    source = e.get("source").asString,
                    target = e.get("target").asString,
                    label = e.get("label")?.takeIf { !it.isJsonNull }?.asString,
                    payload = e.getAsJsonObject("payload")?.let { p ->
                        EdgePayload(
                            name = p.get("name").asString,
                            type = p.get("type").asString,
                            description = p.get("description").asString
                        )
                    },
                    condition = e.get("condition")?.takeIf { !it.isJsonNull }?.asString
                )
            }

            val llmsDetected = graphObj.getAsJsonArray("llms_detected")
                .map { it.asString }

            val workflows = graphObj.getAsJsonArray("workflows").map { wfEl ->
                val wf = wfEl.asJsonObject
                WorkflowMetadata(
                    id = wf.get("id").asString,
                    name = wf.get("name").asString,
                    description = wf.get("description")?.takeIf { !it.isJsonNull }?.asString,
                    nodeIds = wf.getAsJsonArray("nodeIds").map { it.asString },
                    components = (wf.getAsJsonArray("components") ?: emptyList<Any>()).map { compEl ->
                        val c = (compEl as com.google.gson.JsonElement).asJsonObject
                        ComponentMetadata(
                            id = c.get("id").asString,
                            name = c.get("name").asString,
                            description = c.get("description")?.takeIf { !it.isJsonNull }?.asString,
                            nodeIds = c.getAsJsonArray("nodeIds").map { it.asString }
                        )
                    }
                )
            }

            val graph = WorkflowGraph(
                nodes = nodes,
                edges = edges,
                llmsDetected = llmsDetected,
                workflows = workflows
            )

            val usage = obj.get("usage")?.takeIf { !it.isJsonNull }?.asJsonObject?.let { u ->
                TokenUsage(
                    inputTokens = u.get("input_tokens").asInt,
                    outputTokens = u.get("output_tokens").asInt,
                    totalTokens = u.get("total_tokens").asInt,
                    cachedTokens = u.get("cached_tokens")?.asInt ?: 0
                )
            }

            val cost = obj.get("cost")?.takeIf { !it.isJsonNull }?.asJsonObject?.let { c ->
                CostData(
                    inputCost = c.get("input_cost").asDouble,
                    outputCost = c.get("output_cost").asDouble,
                    totalCost = c.get("total_cost").asDouble
                )
            }

            return AnalyzeResponse(graph = graph, usage = usage, cost = cost)
        }
    }
}
