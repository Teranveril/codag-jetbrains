package com.codag.jetbrains.dto

data class LocationMetadata(
    val line: Int,
    val type: String,
    val description: String,
    val function: String,
    val variable: String? = null
)

data class FileMetadata(
    val file: String,
    val locations: List<LocationMetadata>,
    val relatedFiles: List<String>
)

data class AnalyzeRequest(
    val code: String,
    val filePaths: List<String>,
    val frameworkHint: String? = null,
    val metadata: List<FileMetadata> = emptyList(),
    val httpConnections: String? = null
)

data class FunctionContext(
    val name: String,
    val line: Int,
    val type: String,
    val calls: List<String>,
    val code: String? = null
)

data class FileStructureContext(
    val filePath: String,
    val functions: List<FunctionContext>,
    val imports: List<String>
)

data class MetadataRequest(
    val files: List<FileStructureContext>,
    val code: String? = null
)

data class FunctionMetadataResult(
    val name: String,
    val label: String,
    val description: String
)

data class FileMetadataResult(
    val filePath: String,
    val functions: List<FunctionMetadataResult>,
    val edgeLabels: Map<String, String> = emptyMap()
)
