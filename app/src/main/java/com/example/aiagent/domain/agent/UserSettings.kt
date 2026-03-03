// domain/agent/UserSettings.kt
package com.example.aiagent.domain.agent

import kotlinx.serialization.Serializable

/**
 * Настройки пользователя для персонализации ответов AI
 */
@Serializable
data class UserSettings(
    val style: String = "",
    val responseFormat: String = "",
    val constraints: String = ""
) {
    /**
     * Проверяет, заполнены ли настройки
     */
    fun isFilled(): Boolean {
        return style.isNotBlank() || responseFormat.isNotBlank() || constraints.isNotBlank()
    }

    /**
     * Генерирует строку для добавления в системный промпт
     */
    fun toSystemPromptString(): String {
        if (!isFilled()) return ""

        return buildString {
            appendLine("\n=== ПЕРСОНАЛИЗАЦИЯ ОТВЕТОВ ===")
            if (style.isNotBlank()) {
                appendLine("Стиль общения: $style")
            }
            if (responseFormat.isNotBlank()) {
                appendLine("Формат ответа: $responseFormat")
            }
            if (constraints.isNotBlank()) {
                appendLine("Ограничения: $constraints")
            }
            append("================================\n")
        }
    }

    companion object {
        /**
         * Создает настройки с предустановленными значениями
         */
        fun createDefault(): UserSettings {
            return UserSettings(
                style = "",
                responseFormat = "",
                constraints = ""
            )
        }

        /**
         * Предустановленные стили
         */
        val STYLE_OPTIONS = listOf(
            "Формальный",
            "Дружеский",
            "Краткий",
            "Подробный",
            "Образовательный",
            "Технический"
        )

        /**
         * Предустановленные форматы ответа
         */
        val FORMAT_OPTIONS = listOf(
            "Текст",
            "Списки",
            "Markdown",
            "Код",
            "Таблицы",
            "Смешанный"
        )
    }
}
