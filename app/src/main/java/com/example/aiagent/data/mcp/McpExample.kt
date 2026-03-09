package com.example.aiagent.data.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Пример использования McpRepository для тестирования подключения к MCP серверу
 * 
 * Для использования в коде:
 * 1. Создайте экземпляр McpRepository
 * 2. Вызовите метод getTools() в корутине
 * 3. Обработайте результат или ошибку
 * 
 * Пример:
 * ```kotlin
 * val repository = McpRepository()
 * lifecycleScope.launch {
 *     try {
 *         val response = repository.getTools()
 *         Log.d("MCP", "Получено ${response.count} инструментов")
 *         response.tools.forEach { tool ->
 *             Log.d("MCP", "Инструмент: ${tool.name}")
 *         }
 *     } catch (e: Exception) {
 *         Log.e("MCP", "Ошибка: ${e.message}")
 *     }
 * }
 * ```
 */
object McpExample {
    
    private const val TAG = "McpExample"
    
    /**
     * Простой пример получения инструментов из MCP сервера
     */
    fun fetchToolsExample() {
        val repository = McpRepository()
        
        runBlocking {
            try {
                Log.d(TAG, "Начинаем загрузку инструментов...")
                
                // Выполняем запрос в IO потоке
                val response = withContext(Dispatchers.IO) {
                    repository.getTools()
                }
                
                // Обрабатываем ответ
                if (response.success) {
                    Log.d(TAG, "✓ Успешное подключение к серверу")
                    Log.d(TAG, "Количество инструментов: ${response.count}")
                    
                    response.tools.forEach { tool ->
                        Log.d(TAG, "─────────────────────────────")
                        Log.d(TAG, "Название: ${tool.name}")
                        Log.d(TAG, "Описание: ${tool.description}")
                        Log.d(TAG, "Тип схемы: ${tool.inputSchema.type}")
                    }
                    
                    Log.d(TAG, "─────────────────────────────")
                    Log.d(TAG, "Загрузка завершена успешно!")
                } else {
                    Log.e(TAG, "Сервер вернул неуспешный ответ")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при загрузке инструментов", e)
                Log.e(TAG, "Сообщение об ошибке: ${e.message}")
            } finally {
                repository.close()
            }
        }
    }
    
    /**
     * Пример использования в ViewModel или Activity с lifecycleScope
     */
    suspend fun fetchToolsInScope(repository: McpRepository): Result<McpToolsResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                repository.getTools()
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении инструментов", e)
            Result.failure(e)
        }
    }
}
