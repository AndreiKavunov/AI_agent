// domain/agent/memory/VocabularyItem.kt
package com.example.aiagent.domain.agent.memory

import kotlinx.serialization.Serializable

/**
 * Представляет слово или фразу для изучения
 */
@Serializable
data class VocabularyItem(
    val id: String,
    val word: String,                    // Слово или фраза
    val translation: String,             // Перевод
    val partOfSpeech: String? = null,    // Часть речи (noun, verb, adjective, etc.)
    val pronunciation: String? = null,   // Транскрипция
    val example: String? = null,         // Пример использования
    val level: String? = null,            // Уровень сложности (A1-C2)
    val topic: String? = null,           // Тема (restaurant, travel, etc.)
    val masteryLevel: Float = 0.0f,      // Уровень освоения (0.0 - 1.0)
    val reviewCount: Int = 0,            // Сколько раз повторялось
    val lastReviewed: Long? = null,      // Когда последний раз повторялось
    val nextReview: Long? = null,        // Когда нужно повторить (Spaced Repetition)
    val isFavorite: Boolean = false,    // В избранном
    val tags: List<String> = emptyList(), // Теги для организации
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(
            word: String,
            translation: String,
            partOfSpeech: String? = null,
            pronunciation: String? = null,
            example: String? = null,
            level: String? = null,
            topic: String? = null
        ): VocabularyItem {
            return VocabularyItem(
                id = "vocab_${System.currentTimeMillis()}_${word.hashCode()}",
                word = word,
                translation = translation,
                partOfSpeech = partOfSpeech,
                pronunciation = pronunciation,
                example = example,
                level = level,
                topic = topic
            )
        }
    }

    fun updateMastery(newLevel: Float): VocabularyItem {
        val now = System.currentTimeMillis()
        return copy(
            masteryLevel = newLevel.coerceIn(0f, 1f),
            reviewCount = reviewCount + 1,
            lastReviewed = now,
            nextReview = calculateNextReview(newLevel),
            updatedAt = now
        )
    }

    private fun calculateNextReview(masteryLevel: Float): Long {
        // Простая реализация Spaced Repetition
        val now = System.currentTimeMillis()
        val hours = when {
            masteryLevel < 0.3f -> 1        // Через 1 час
            masteryLevel < 0.5f -> 6        // Через 6 часов
            masteryLevel < 0.7f -> 24       // Через 1 день
            masteryLevel < 0.9f -> 72       // Через 3 дня
            else -> 168                     // Через неделю
        }
        return now + (hours * 60 * 60 * 1000)
    }

    fun formatForPrompt(): String {
        val builder = StringBuilder()
        builder.append("$word")
        pronunciation?.let { builder.append(" [$it]") }
        builder.append(" - $translation")
        partOfSpeech?.let { builder.append(" ($it)") }
        example?.let { builder.append("\n  Пример: $it") }
        return builder.toString()
    }

    fun isDueForReview(): Boolean {
        val next = nextReview ?: return true
        return System.currentTimeMillis() >= next
    }
}
