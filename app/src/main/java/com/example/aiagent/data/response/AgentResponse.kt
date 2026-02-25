package com.example.aiagent.data.response

import kotlinx.serialization.Serializable


@Serializable
data class AgentResponse(
    val text: String,
    val toolUsed: String? = null,
    val responseTimeMs: Long,
    val tokenCount: Int,
    // Новые поля для подсчёта токенов
    val promptTokens: Int? = null,        // Токены в отправленном промпте
    val totalHistoryTokens: Int? = null,  // Всего токенов в истории после ответа
    val estimatedCost: Double? = null      // Оценка стоимости (если нужно)
) {
    /**
     * Форматированный вывод информации о токенах
     */
    fun getTokenInfo(): String {
        return buildString {
            append("📊 Токены: ответ=$tokenCount")
            promptTokens?.let { append(", промпт=$it") }
            totalHistoryTokens?.let { append(", всего в истории=$it") }
            estimatedCost?.let { append(", стоимость=$${"%.6f".format(it)}") }
        }
    }
}