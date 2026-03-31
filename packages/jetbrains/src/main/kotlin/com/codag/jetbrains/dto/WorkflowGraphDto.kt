package com.codag.jetbrains.dto

data class SourceLocation(
    val file: String,
    val line: Int,
    val function: String? = null
)

data class EdgePayload(
    val name: String,
    val type: String,
    val description: String
)

data class GraphNode(
    val id: String,
    val label: String,
    val type: String,
    val description: String? = null,
    val source: SourceLocation? = null,
    val model: String? = null,
    val temperature: Float? = null
)

data class GraphEdge(
    val source: String,
    val target: String,
    val label: String? = null,
    val payload: EdgePayload? = null,
    val condition: String? = null
)

data class ComponentMetadata(
    val id: String,
    val name: String,
    val description: String? = null,
    val nodeIds: List<String>
)

data class WorkflowMetadata(
    val id: String,
    val name: String,
    val description: String? = null,
    val nodeIds: List<String>,
    val components: List<ComponentMetadata> = emptyList()
)

data class WorkflowGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val llmsDetected: List<String> = emptyList(),
    val workflows: List<WorkflowMetadata> = emptyList()
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val cachedTokens: Int = 0
)

data class CostData(
    val inputCost: Double,
    val outputCost: Double,
    val totalCost: Double
)

data class AnalyzeResponse(
    val graph: WorkflowGraph,
    val usage: TokenUsage? = null,
    val cost: CostData? = null
)
