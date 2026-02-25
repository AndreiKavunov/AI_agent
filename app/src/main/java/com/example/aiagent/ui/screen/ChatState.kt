// ui/screen/ChatState.kt
package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType

sealed interface Message {
    val id: String
    val content: String
    val timestamp: Long

    data class UserMessage(
        override val id: String,
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message

    data class AgentMessage(
        override val id: String,
        override val content: String,
        val toolUsed: String? = null,
        val responseTimeMs: Long = 0,
        val tokenCount: Int = 0,
        val promptTokens: Int? = null,  // Токены в промпте
        val totalHistoryTokens: Int? = null, // Всего токенов в истории
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message
}

data class TokenStats(
    val totalTokens: Int = 0,
    val systemTokens: Int = 0,
    val userTokens: Int = 0,
    val assistantTokens: Int = 0,
    val lastResponseTokens: Int? = null,
    val currentQueryTokens: Int = 0
) {
    fun getFormattedString(): String = buildString {
        appendLine("📊 Статистика токенов:")
        appendLine("├─ Всего: $totalTokens")
        appendLine("├─ Системные: $systemTokens")
        appendLine("├─ Пользователь: $userTokens")
        appendLine("├─ Ассистент: $assistantTokens")
        lastResponseTokens?.let {
            appendLine("├─ Последний ответ: $it")
        }
        append("└─ Текущий запрос: $currentQueryTokens")
    }
}

data class LastResponseInfo(
    val timeMs: Long,
    val tokenCount: Int,
    val promptTokens: Int?,
    val tokensPerSecond: Double?
)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val temperature: Double = 0.7,
    val currentRepositoryType: RepositoryType = RepositoryType.GIGACHAT,
    val huggingFaceModel: HuggingFaceModel = HuggingFaceModel.MEDIUM,
    val lastResponse: LastResponseInfo? = null,
    val tokenStats: TokenStats = TokenStats(),
    val showTokenDetails: Boolean = false
)