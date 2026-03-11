package com.example.aiagent.data.meetings

import android.util.Log
import com.example.aiagent.data.mcp.McpRepository

class MeetingsRepository {
    
    private val tag = "MeetingsRepository"
    private val mcpRepository = McpRepository()
    
    /**
     * Получает список встреч из MCP сервера
     * @return Result с MeetingsData или ошибкой
     */
    suspend fun getMeetings(): Result<MeetingsData> {
        Log.d(tag, "========================================")
        Log.d(tag, "Fetching meetings from MCP server")
        Log.d(tag, "========================================")
        
        return try {
            // Вызываем инструмент get_meetings через MCP репозиторий
            Log.d(tag, "Calling MCP tool 'get_meetings'...")
            val response = mcpRepository.callTool("get_meetings")
            
            Log.d(tag, "MCP response received")
            Log.d(tag, "Success: ${response.success}")
            Log.d(tag, "Result: ${response.result}")
            Log.d(tag, "Error: ${response.error}")
            
            if (response.success && response.result != null) {
                Log.d(tag, "✓ Meetings fetched successfully")
                Log.d(tag, "Meetings data: ${response.result}")
                Result.success(
                    MeetingsData(
                        meetings = response.result
                    )
                )
            } else {
                val errorMsg = response.error ?: "Unknown error"
                Log.e(tag, "✗ Failed to fetch meetings: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Error fetching meetings", e)
            Log.e(tag, "Error message: ${e.message}")
            Log.e(tag, "Error type: ${e.javaClass.simpleName}")
            // Возвращаем mock данные если сервер недоступен
            Log.d(tag, "Returning mock meetings data")
            val mockData = getMockMeetingsData()
            Log.d(tag, "Mock data: ${mockData.meetings}")
            Result.success(mockData)
        }
    }
    
    /**
     * Возвращает mock данные о встречах для тестирования
     */
    private fun getMockMeetingsData(): MeetingsData {
        return MeetingsData(
            meetings = "Встречи на сегодня: 10:00 - Планерка с командой, 14:30 - Встреча с клиентом (проект MCP), 17:00 - Созвон по код-ревью"
        )
    }
    
    /**
     * Закрывает HTTP клиент
     */
    fun close() {
        mcpRepository.close()
    }
}
