// domain/agent/TokenCounter.kt
package com.example.aiagent.domain.agent

import android.util.Log
import com.example.aiagent.data.huggingFace.ChatMessage

private const val TAG = "TokenCounter"

class TokenCounter {

    companion object {
        private const val AVG_CHARS_PER_TOKEN = 4
        private const val SYSTEM_PROMPT_RATIO = 1.2
    }

    /**
     * Подсчёт токенов для одного сообщения (приблизительный)
     */
    fun estimateTokens(text: String, isSystemPrompt: Boolean = false): Int {
        if (text.isBlank()) return 0

        val factor = if (isSystemPrompt) SYSTEM_PROMPT_RATIO else 1.0
        val tokens = kotlin.math.ceil(text.length / AVG_CHARS_PER_TOKEN.toDouble() * factor).toInt()
        return tokens.coerceAtLeast(1)
    }

    /**
     * Детальный подсчёт с возможностью использовать реальные токены из API
     */
    fun detailedCount(
        messages: List<ChatMessage>,
        realTokenCounts: Map<String, Int> = emptyMap() // messageId -> реальные токены
    ): DetailedTokenCount {
        var systemTokens = 0
        var userTokens = 0
        var assistantTokens = 0

        messages.forEach { message ->
            // Используем реальные токены из API, если есть
            val tokens = realTokenCounts[message.id] ?: estimateTokens(
                message.content,
                isSystemPrompt = message.role == "system"
            )

            when (message.role) {
                "system" -> systemTokens += tokens
                "user" -> userTokens += tokens
                "assistant" -> assistantTokens += tokens
            }
        }

        return DetailedTokenCount(
            systemTokens = systemTokens,
            userTokens = userTokens,
            assistantTokens = assistantTokens,
            totalTokens = systemTokens + userTokens + assistantTokens
        )
    }

    fun formatTokenReport(detailed: DetailedTokenCount): String {
        return """
            📊 Детальный подсчёт токенов:
            ├─ Системные: ${detailed.systemTokens}
            ├─ Пользователь: ${detailed.userTokens}
            ├─ Ассистент: ${detailed.assistantTokens}
            └─ ВСЕГО: ${detailed.totalTokens}
        """.trimIndent()
    }
}

data class DetailedTokenCount(
    val systemTokens: Int,
    val userTokens: Int,
    val assistantTokens: Int,
    val totalTokens: Int
)