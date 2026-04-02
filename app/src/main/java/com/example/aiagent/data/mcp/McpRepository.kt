package com.example.aiagent.data.mcp

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Repository for communicating with MCP server
 */
class McpRepository(baseUrl: String = "http://192.168.0.82:8000") {
    
    private val tag = "McpRepository"
    
    // Base URL of the MCP server
    private val baseUrl = baseUrl
    
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
     * Calls a specific tool on the MCP server
     * @param toolName The name of the tool to call
     * @param arguments Optional arguments for the tool
     * @return CallToolResponse containing the result
     * @throws Exception if the request fails
     */
    suspend fun callTool(toolName: String, arguments: Map<String, String> = emptyMap()): CallToolResponse {
        return try {
            Log.d(tag, "Calling tool: $toolName")
            
            val request = CallToolRequest(name = toolName, arguments = arguments)
            
            val response: CallToolResponse = client.post("$baseUrl/call_tool") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            if (response.success) {
                Log.d(tag, "Tool $toolName executed successfully: ${response.result}")
            } else {
                Log.e(tag, "Tool $toolName failed: ${response.error}")
            }
            
            response
        } catch (e: Exception) {
            Log.e(tag, "Error calling tool $toolName: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Closes the HTTP client
     */
    fun close() {
        client.close()
    }
    
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    
    /**
     * Search for text in files
     * @param pattern Text to search for
     * @param filePattern File pattern (e.g., *.kt)
     * @return FileSearchResponse containing search results
     */
    suspend fun fileSearch(pattern: String, filePattern: String = "*"): FileSearchResponse {
        Log.d(tag, "fileSearch called: pattern='$pattern', filePattern='$filePattern'")
        
        return try {
            val arguments = mapOf(
                "pattern" to pattern,
                "file_pattern" to filePattern
            )
            
            Log.d(tag, "POST to $baseUrl/file/search")
            
            // Get response as text first to debug
            val responseText = client.post("$baseUrl/file/search") {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }.body<String>()
            
            Log.d(tag, "Raw response: $responseText")
            
            // Parse manually
            val response = jsonParser.decodeFromString<FileSearchResponse>(responseText)
            
            Log.d(tag, "fileSearch response: success=${response.success}, count=${response.count}")
            response
        } catch (e: Exception) {
            Log.e(tag, "fileSearch error: ${e.message}", e)
            FileSearchResponse(false, pattern, filePattern, 0, emptyList())
        }
    }
    
    /**
     * Read the contents of a file
     * @param path Path to the file
     * @return FileReadResponse containing file content
     */
    suspend fun fileRead(path: String): FileReadResponse {
        Log.d(tag, "fileRead called: path='$path'")
        
        return try {
            val arguments = mapOf("path" to path)
            
            Log.d(tag, "POST to $baseUrl/file/read")
            
            val responseText = client.post("$baseUrl/file/read") {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<FileReadResponse>(responseText)
            
            Log.d(tag, "fileRead response: success=${response.success}, size=${response.size}")
            response
        } catch (e: Exception) {
            Log.e(tag, "fileRead error: ${e.message}", e)
            FileReadResponse(false, "", path, 0)
        }
    }
    
    /**
     * Write or create a file
     * @param path Path to the file
     * @param content Content to write
     * @return FileWriteResponse with diff information
     */
    suspend fun fileWrite(path: String, content: String): FileWriteResponse {
        Log.d(tag, "fileWrite called: path='$path', content length=${content.length}")
        
        return try {
            val arguments = mapOf(
                "path" to path,
                "content" to content
            )
            
            Log.d(tag, "POST to $baseUrl/file/write")
            
            val responseText = client.post("$baseUrl/file/write") {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<FileWriteResponse>(responseText)
            
            Log.d(tag, "fileWrite response: success=${response.success}, was_new=${response.was_new}")
            response
        } catch (e: Exception) {
            Log.e(tag, "fileWrite error: ${e.message}", e)
            FileWriteResponse(false, e.message ?: "Unknown error", false, "")
        }
    }
    
    /**
     * List files matching a pattern
     * @param pattern File pattern (e.g., *.kt)
     * @return FileListResponse containing list of files
     */
    suspend fun fileList(pattern: String = "*"): FileListResponse {
        Log.d(tag, "fileList called: pattern='$pattern'")
        
        return try {
            val arguments = mapOf("pattern" to pattern)
            
            Log.d(tag, "POST to $baseUrl/file/list")
            
            val responseText = client.post("$baseUrl/file/list") {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<FileListResponse>(responseText)
            
            Log.d(tag, "fileList response: success=${response.success}, count=${response.count}")
            response
        } catch (e: Exception) {
            Log.e(tag, "fileList error: ${e.message}", e)
            FileListResponse(false, pattern, 0, emptyList())
        }
    }
    
    /**
     * Analyze files through RAG
     * @param query Question to ask about the code
     * @param filePattern File pattern to analyze (e.g., *.kt)
     * @return FileAnalyzeResponse with analysis results
     */
    suspend fun fileAnalyze(query: String, filePattern: String = "*"): FileAnalyzeResponse {
        Log.d(tag, "fileAnalyze called: query='$query', filePattern='$filePattern'")
        
        return try {
            val arguments = mapOf(
                "query" to query,
                "file_pattern" to filePattern
            )
            
            Log.d(tag, "POST to $baseUrl/file/analyze")
            
            val responseText = client.post("$baseUrl/file/analyze") {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<FileAnalyzeResponse>(responseText)
            
            Log.d(tag, "fileAnalyze response: success=${response.success}, sources=${response.sources.size}")
            response
        } catch (e: Exception) {
            Log.e(tag, "fileAnalyze error: ${e.message}", e)
            FileAnalyzeResponse(false, query, filePattern, e.message ?: "Unknown error", emptyList())
        }
    }
    
    /**
     * Generate README from project structure
     * @return GenerateResponse with README content
     */
    suspend fun generateReadme(): GenerateResponse {
        Log.d(tag, "generateReadme called")
        
        return try {
            Log.d(tag, "POST to $baseUrl/file/generate_readme")
            
            val responseText = client.post("$baseUrl/file/generate_readme") {
                contentType(ContentType.Application.Json)
                setBody(emptyMap<String, String>())
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<GenerateResponse>(responseText)
            
            Log.d(tag, "generateReadme response: success=${response.success}")
            response
        } catch (e: Exception) {
            Log.e(tag, "generateReadme error: ${e.message}", e)
            GenerateResponse(false, "", e.message ?: "Unknown error", null)
        }
    }
    
    /**
     * Generate CHANGELOG from git history
     * @return GenerateResponse with CHANGELOG content
     */
    suspend fun generateChangelog(): GenerateResponse {
        Log.d(tag, "generateChangelog called")
        
        return try {
            Log.d(tag, "POST to $baseUrl/file/generate_changelog")
            
            val responseText = client.post("$baseUrl/file/generate_changelog") {
                contentType(ContentType.Application.Json)
                setBody(emptyMap<String, String>())
            }.body<String>()
            
            Log.d(tag, "Raw response: ${responseText.take(100)}...")
            
            val response = jsonParser.decodeFromString<GenerateResponse>(responseText)
            
            Log.d(tag, "generateChangelog response: success=${response.success}")
            response
        } catch (e: Exception) {
            Log.e(tag, "generateChangelog error: ${e.message}", e)
            GenerateResponse(false, "", e.message ?: "Unknown error", null)
        }
    }
}
