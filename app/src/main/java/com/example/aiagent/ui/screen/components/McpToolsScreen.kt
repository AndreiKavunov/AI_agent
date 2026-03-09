package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aiagent.data.mcp.McpRepository
import com.example.aiagent.data.mcp.McpTool
import com.example.aiagent.data.mcp.McpToolsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Simple screen to display MCP tools from the server
 */
@Composable
fun McpToolsScreen() {
    var toolsResponse by remember { mutableStateOf<McpToolsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val repository = remember { McpRepository() }
    
    // Function to fetch tools
    val fetchTools = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    repository.getTools()
                }
                toolsResponse = response
                Log.d("McpToolsScreen", "Loaded ${response.count} tools")
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                Log.e("McpToolsScreen", "Failed to fetch tools", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    // Auto-fetch on first composition
    LaunchedEffect(Unit) {
        fetchTools()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "MCP Server Tools",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Server info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Server URL: http://192.168.0.82:8000",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Endpoint: /tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        
        // Loading state
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            Text(
                text = "Loading tools...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Error state
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Button(
                onClick = { fetchTools() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Retry")
            }
        }
        
        // Success state - display tools
        toolsResponse?.let { response ->
            if (response.success) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "✓ Connected successfully",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Found ${response.count} tool(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                if (response.tools.isEmpty()) {
                    Text(
                        text = "No tools available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(response.tools) { tool ->
                            ToolCard(tool)
                        }
                    }
                }
            } else {
                Text(
                    text = "Server returned unsuccessful response",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Card displaying a single tool
 */
@Composable
fun ToolCard(tool: McpTool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Input Schema: ${tool.inputSchema.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
