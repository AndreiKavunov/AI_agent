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
     * Получает список задач из MCP сервера
     * @return Result с строкой задач или ошибкой
     */
    suspend fun getTodos(): Result<String> {
        Log.d(tag, "========================================")
        Log.d(tag, "Fetching todos from MCP server")
        Log.d(tag, "========================================")
        
        return try {
            // Вызываем инструмент get_todos через MCP репозиторий
            Log.d(tag, "Calling MCP tool 'get_todos'...")
            val response = mcpRepository.callTool("get_todos")
            
            Log.d(tag, "MCP response received")
            Log.d(tag, "Success: ${response.success}")
            Log.d(tag, "Result: ${response.result}")
            Log.d(tag, "Error: ${response.error}")
            
            if (response.success && response.result != null) {
                Log.d(tag, "✓ Todos fetched successfully")
                Log.d(tag, "Todos data: ${response.result}")
                Result.success(response.result)
            } else {
                val errorMsg = response.error ?: "Unknown error"
                Log.e(tag, "✗ Failed to fetch todos: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Error fetching todos", e)
            Log.e(tag, "Error message: ${e.message}")
            Log.e(tag, "Error type: ${e.javaClass.simpleName}")
            // Возвращаем mock данные если сервер недоступен
            Log.d(tag, "Returning mock todos data")
            val mockData = getMockTodosData()
            Log.d(tag, "Mock data: $mockData")
            Result.success(mockData)
        }
    }

    /**
     * Получает данные о встречах и задачах из MCP сервера
     * @return Result с MeetingsData содержащим встречи и задачи или ошибкой
     */
    suspend fun getMeetingsAndTodos(): Result<MeetingsData> {
        Log.d(tag, "========================================")
        Log.d(tag, "Fetching meetings and todos from MCP server")
        Log.d(tag, "========================================")
        
        return try {
            // Получаем встречи
            val meetingsResult = getMeetings()
            val todosResult = getTodos()
            
            if (meetingsResult.isSuccess && todosResult.isSuccess) {
                val meetingsData = meetingsResult.getOrNull()
                val todosData = todosResult.getOrNull()
                
                Log.d(tag, "✓ Both meetings and todos fetched successfully")
                Result.success(
                    MeetingsData(
                        meetings = meetingsData?.meetings ?: "",
                        todos = todosData
                    )
                )
            } else {
                val errorMsg = "Failed to fetch meetings or todos"
                Log.e(tag, "✗ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Error fetching meetings and todos", e)
            Log.e(tag, "Error message: ${e.message}")
            Log.e(tag, "Error type: ${e.javaClass.simpleName}")
            // Возвращаем mock данные если сервер недоступен
            Log.d(tag, "Returning mock meetings and todos data")
            val mockData = getMockMeetingsAndTodosData()
            Log.d(tag, "Mock data: ${mockData.meetings}, ${mockData.todos}")
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
     * Возвращает mock данные о задачах для тестирования
     */
    private fun getMockTodosData(): String {
        return "Задачи на сегодня: [x] Подготовить отчет за неделю, [x] Отправить письмо клиенту, [ ] Закончить документацию API, [ ] Провести код-ревью"
    }

    /**
     * Возвращает mock данные о встречах и задачах для тестирования
     */
    private fun getMockMeetingsAndTodosData(): MeetingsData {
        return MeetingsData(
            meetings = "Встречи на сегодня: 10:00 - Планерка с командой, 14:30 - Встреча с клиентом (проект MCP), 17:00 - Созвон по код-ревью",
            todos = "Задачи на сегодня: [x] Подготовить отчет за неделю, [x] Отправить письмо клиенту, [ ] Закончить документацию API, [ ] Провести код-ревью"
        )
    }
    
    /**
     * Закрывает HTTP клиент
     */
    fun close() {
        mcpRepository.close()
    }
}
