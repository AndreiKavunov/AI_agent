package com.example.aiagent

import com.example.aiagent.data.mcp.McpRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit тест для McpRepository
 * 
 * ВНИМАНИЕ: Этот тест требует, чтобы MCP сервер был запущен и доступен
 * по адресу http://192.168.0.82:8000
 * 
 * Для запуска теста:
 * 1. Убедитесь, что MCP сервер запущен
 * 2. Запустите тест через Android Studio или ./gradlew test
 */
class McpRepositoryTest {
    
    @Test
    fun testGetTools() = runBlocking {
        val repository = McpRepository()
        
        try {
            // Получаем инструменты
            val response = repository.getTools()
            
            // Проверяем ответ
            assertTrue("Ответ должен быть успешным", response.success)
            assertTrue("Количество инструментов должно быть >= 0", response.count >= 0)
            assertEquals("Количество инструментов должно совпадать с размером списка", 
                response.count, response.tools.size)
            
            // Если есть инструменты, проверяем их структуру
            if (response.tools.isNotEmpty()) {
                val tool = response.tools[0]
                assertNotNull("Название инструмента не должно быть null", tool.name)
                assertNotNull("Описание инструмента не должно быть null", tool.description)
                assertNotNull("Схема ввода не должна быть null", tool.inputSchema)
                assertNotNull("Тип схемы не должен быть null", tool.inputSchema.type)
                
                println("✓ Тест пройден успешно!")
                println("  Получено инструментов: ${response.count}")
                response.tools.forEach { tool ->
                    println("  - ${tool.name}: ${tool.description}")
                }
            }
            
        } catch (e: Exception) {
            println("✗ Тест не пройден: ${e.message}")
            println("  Убедитесь, что MCP сервер запущен по адресу http://192.168.0.82:8000")
            throw e
        } finally {
            repository.close()
        }
    }
}
