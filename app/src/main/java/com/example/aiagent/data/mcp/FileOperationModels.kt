package com.example.aiagent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
data class FileSearchResponse(
    val success: Boolean,
    val pattern: String,
    val file_pattern: String,
    val count: Int,
    val results: List<SearchResult>
)

@Serializable
data class SearchResult(
    val file: String,
    val line: Int,
    val content: String
)

@Serializable
data class FileReadResponse(
    val success: Boolean,
    val content: String,
    val path: String,
    val size: Int
)

@Serializable
data class GenerateResponse(
    val success: Boolean,
    val content: String,
    val message: String,
    val commits_count: Int? = null
)

@Serializable
data class FileWriteResponse(
    val success: Boolean,
    val message: String,
    val was_new: Boolean,
    val diff: String
)

@Serializable
data class FileListResponse(
    val success: Boolean,
    val pattern: String,
    val count: Int,
    val files: List<String>
)

@Serializable
data class FileAnalyzeResponse(
    val success: Boolean,
    val query: String,
    val file_pattern: String,
    val answer: String,
    val sources: List<AnalyzeSource> = emptyList()
)

@Serializable
data class AnalyzeSource(
    val file: String,
    val relevance: Double
)
