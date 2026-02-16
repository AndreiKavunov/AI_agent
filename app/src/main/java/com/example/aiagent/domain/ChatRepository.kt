package com.example.aiagent.domain

import com.example.aiagent.data.response.AgentResponse

interface ChatRepository {
    suspend fun sendMessage(message: String): AgentResponse
}