package com.example.aiagent.domain.agent

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType

/**
 * Универсальный агент, который управляет контекстом и делегирует запросы
 */
interface UniversalAgent {
    /**
     * Отправить сообщение текущему агенту
     */
    suspend fun processMessage(message: String, temperature: Double = 0.7): AgentResponse

    /**
     * Очистить историю диалога (сохраняя системный промпт)
     */
    fun clearHistory()

    /**
     * Полностью очистить всё (включая системный промпт)
     */
    fun clearAll()

    /**
     * Установить системный промпт
     */
    fun setSystemPrompt(prompt: String)

    /**
     * Получить текущий системный промпт
     */
    fun getSystemPrompt(): String?

    /**
     * Получить информацию о текущем агенте/модели
     */
    fun getCurrentAgentInfo(): String

    /**
     * Переключить тип репозитория (очищает контекст)
     */
    fun switchRepository(type: RepositoryType)

    /**
     * Получить текущий тип репозитория
     */
    fun getCurrentRepositoryType(): RepositoryType

    /**
     * Установить модель HuggingFace (если текущий тип - HUGGINGFACE)
     */
    fun setHuggingFaceModel(modelType: HuggingFaceModel)

    /**
     * Получить текущую модель HuggingFace
     */
    fun getCurrentHuggingFaceModel(): HuggingFaceModel?
}
