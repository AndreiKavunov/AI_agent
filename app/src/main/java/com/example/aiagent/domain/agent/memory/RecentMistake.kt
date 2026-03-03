// domain/agent/memory/RecentMistake.kt
package com.example.aiagent.domain.agent.memory

import kotlinx.serialization.Serializable

/**
 * Представляет ошибку, сделанную пользователем при изучении языка
 */
@Serializable
data class RecentMistake(
    val id: String,
    val error: String,              // Ошибочная фраза
    val correction: String,         // Правильный вариант
    val rule: String,               // Грамматическое правило (например: "past_simple")
    val explanation: String? = null, // Объяснение почему это ошибка
    val timestamp: Long = System.currentTimeMillis(),
    val occurrences: Int = 1        // Сколько раз повторилась эта ошибка
) {
    companion object {
        fun create(
            error: String,
            correction: String,
            rule: String,
            explanation: String? = null
        ): RecentMistake {
            return RecentMistake(
                id = "mistake_${System.currentTimeMillis()}_${error.hashCode()}",
                error = error,
                correction = correction,
                rule = rule,
                explanation = explanation
            )
        }
    }

    fun incrementOccurrences(): RecentMistake {
        return copy(
            occurrences = occurrences + 1,
            timestamp = System.currentTimeMillis()
        )
    }

    fun formatForPrompt(): String {
        return """
            Ошибка: "$error"
            Правильно: "$correction"
            Правило: $rule
            ${explanation?.let { "Объяснение: $it" } ?: ""}
        """.trimIndent()
    }
}
