package com.example.aiagent.data.mcp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import android.util.Log

/**
 * Repository for communicating with MCP server
 */
class McpRepository {
    
    private val tag = "McpRepository"
    
    // Base URL of the MCP server
    private val baseUrl = "http://192.168.0.82:8000"
    
    // Ktor HTTP client with JSON support
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }
    
    /**
     * Fetches the list of available tools from the MCP server
     * @return McpToolsResponse containing the list of tools
     * @throws Exception if the request fails
     */
    suspend fun getTools(): McpToolsResponse {
        return try {
            Log.d(tag, "Fetching tools from $baseUrl/tools")
            
            val response: McpToolsResponse = client.get("$baseUrl/tools").body()
            
            Log.d(tag, "Successfully fetched ${response.count} tools")
            response.tools.forEach { tool ->
                Log.d(tag, "Tool: ${tool.name} - ${tool.description}")
            }
            
            response
        } catch (e: Exception) {
            Log.e(tag, "Error fetching tools: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Closes the HTTP client
     */
    fun close() {
        client.close()
    }
}
