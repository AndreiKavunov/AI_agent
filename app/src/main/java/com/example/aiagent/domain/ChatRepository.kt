package com.example.aiagent.domain

import com.example.aiagent.data.response.AgentResponse

interface ChatRepository {
    suspend fun sendMessage(message: String): AgentResponse

    // Новый метод с температурой
    suspend fun sendMessage(message: String, temperature: Double): AgentResponse

    fun clearChatHistory()
}

