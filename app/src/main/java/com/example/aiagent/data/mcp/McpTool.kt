package com.example.aiagent.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Represents a tool from the MCP server
 */
@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: InputSchema
)

/**
 * Input schema for a tool
 */
@Serializable
data class InputSchema(
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap())
)

/**
 * Response from /tools endpoint
 */
@Serializable
data class McpToolsResponse(
    val success: Boolean,
    val tools: List<McpTool>,
    val count: Int
)
