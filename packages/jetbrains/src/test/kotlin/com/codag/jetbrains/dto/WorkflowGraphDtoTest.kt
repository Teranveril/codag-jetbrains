package com.codag.jetbrains.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: Tests for WorkflowGraph DTOs — mirrors backend models.py exactly.
 * Written BEFORE implementation.
 */
class WorkflowGraphDtoTest {

    @Test
    fun testGraphNodeRequiredFields() {
        val node = GraphNode(
            id = "node_1",
            label = "Validate Request",
            type = "step"
        )
        assertEquals("node_1", node.id)
        assertEquals("Validate Request", node.label)
        assertEquals("step", node.type)
        assertNull(node.description)
        assertNull(node.source)
        assertNull(node.model)
        assertNull(node.temperature)
    }

    @Test
    fun testGraphNodeLlmFields() {
        val node = GraphNode(
            id = "llm_1",
            label = "Gemini 2.5 Flash",
            type = "llm",
            model = "gemini-2.5-flash",
            temperature = 0.7f
        )
        assertEquals("llm", node.type)
        assertEquals("gemini-2.5-flash", node.model)
        assertEquals(0.7f, node.temperature)
    }

    @Test
    fun testGraphNodeWithSource() {
        val source = SourceLocation(file = "main.py", line = 42, function = "analyze")
        val node = GraphNode(
            id = "node_1",
            label = "Parse",
            type = "step",
            source = source
        )
        assertNotNull(node.source)
        assertEquals("main.py", node.source!!.file)
        assertEquals(42, node.source!!.line)
        assertEquals("analyze", node.source!!.function)
    }

    @Test
    fun testGraphEdgeRequiredFields() {
        val edge = GraphEdge(source = "node_1", target = "node_2")
        assertEquals("node_1", edge.source)
        assertEquals("node_2", edge.target)
        assertNull(edge.label)
        assertNull(edge.payload)
        assertNull(edge.condition)
    }

    @Test
    fun testGraphEdgeWithPayload() {
        val payload = EdgePayload(
            name = "request",
            type = "AnalyzeRequest",
            description = "User's code submission"
        )
        val edge = GraphEdge(
            source = "node_1",
            target = "llm_1",
            label = "submit",
            payload = payload,
            condition = "if valid"
        )
        assertNotNull(edge.payload)
        assertEquals("request", edge.payload!!.name)
        assertEquals("if valid", edge.condition)
    }

    @Test
    fun testWorkflowGraphStructure() {
        val graph = WorkflowGraph(
            nodes = listOf(
                GraphNode(id = "n1", label = "Start", type = "step"),
                GraphNode(id = "n2", label = "LLM Call", type = "llm", model = "gemini")
            ),
            edges = listOf(
                GraphEdge(source = "n1", target = "n2", label = "invoke")
            ),
            llmsDetected = listOf("gemini"),
            workflows = emptyList()
        )
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertEquals(listOf("gemini"), graph.llmsDetected)
    }

    @Test
    fun testWorkflowMetadataWithComponents() {
        val component = ComponentMetadata(
            id = "comp_1",
            name = "Error Handling",
            description = "Handles API errors",
            nodeIds = listOf("n3", "n4")
        )
        val workflow = WorkflowMetadata(
            id = "wf_1",
            name = "Analysis Pipeline",
            description = "Main analysis flow",
            nodeIds = listOf("n1", "n2", "n3", "n4"),
            components = listOf(component)
        )
        assertEquals("wf_1", workflow.id)
        assertEquals(1, workflow.components.size)
        assertEquals(listOf("n3", "n4"), workflow.components[0].nodeIds)
    }

    @Test
    fun testTokenUsageAndCost() {
        val usage = TokenUsage(
            inputTokens = 1000,
            outputTokens = 500,
            totalTokens = 1500,
            cachedTokens = 200
        )
        val cost = CostData(
            inputCost = 0.001,
            outputCost = 0.002,
            totalCost = 0.003
        )
        assertEquals(1500, usage.totalTokens)
        assertEquals(0.003, cost.totalCost, 0.0001)
    }

    @Test
    fun testAnalyzeResponse() {
        val graph = WorkflowGraph(
            nodes = emptyList(),
            edges = emptyList(),
            llmsDetected = emptyList(),
            workflows = emptyList()
        )
        val response = AnalyzeResponse(graph = graph, usage = null, cost = null)
        assertNotNull(response.graph)
        assertNull(response.usage)
        assertNull(response.cost)
    }
}
