package com.example.aiagent.domain

import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.response.AgentResponse

interface ChatRepository {
    // Новый метод, который принимает всю историю
    suspend fun sendMessageWithHistory(
        history: List<GigaMessage>,
        temperature: Double,
        maxTokens: Int = 512
    ): AgentResponse

    // Старые методы больше не нужны но оставим для обратной совместимости
    @Deprecated("Use sendMessageWithHistory instead")
    suspend fun sendMessage(message: String): AgentResponse

    @Deprecated("Use sendMessageWithHistory instead")
    suspend fun sendMessage(message: String, temperature: Double): AgentResponse

    // Репозиторий больше не управляет историей!
    fun clearChatHistory() // Оставляем только для обратной совместимости
}
