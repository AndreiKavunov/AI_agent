package com.example.aiagent.domain.agent

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType

interface UniversalAgent {
    suspend fun processMessage(message: String, temperature: Double): AgentResponse
    suspend fun clearHistory()
    suspend fun clearAll()
    suspend fun setSystemPrompt(prompt: String)
    suspend fun getSystemPrompt(): String?
    fun getCurrentAgentInfo(): String
    suspend fun switchRepository(type: RepositoryType)
    fun getCurrentRepositoryType(): RepositoryType
    suspend fun setHuggingFaceModel(modelType: HuggingFaceModel)
    fun getCurrentHuggingFaceModel(): HuggingFaceModel?

    // Новые методы для подсчёта токенов
    suspend fun getCurrentQueryTokens(): Int
    suspend fun getTotalHistoryTokens(): DetailedTokenCount
    suspend fun getLastResponseTokens(): Int?
    fun getTokenCounter(): TokenCounter
}