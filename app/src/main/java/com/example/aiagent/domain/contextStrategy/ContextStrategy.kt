package com.example.aiagent.domain.contextStrategy

import com.example.aiagent.ui.screen.Message


sealed interface ContextStrategy {
    val name: String
    val description: String

    /** Стратегия 1: Скользящее окно */
    data class SlidingWindow(
        val maxMessages: Int = 10
    ) : ContextStrategy {
        override val name = "Sliding Window"
        override val description = "Хранит только последние $maxMessages сообщений"
    }

    /** Стратегия 2: Ключ-значение факты */
    data class StickyFacts(
        val maxMessages: Int = 10
    ) : ContextStrategy {
        override val name = "Sticky Facts"
        override val description = "Сохраняет важные факты из диалога + последние $maxMessages сообщений"
    }

    /** Стратегия 3: Ветвление */
    data class Branching(
        val maxBranches: Int = 5
    ) : ContextStrategy {
        override val name = "Branching"
        override val description = "Позволяет создавать и переключаться между ветками диалога"
    }

    /** Стратегия 4: Изучение языков */
    data class LanguageLearning(
        val maxMessages: Int = 10
    ) : ContextStrategy {
        override val name = "Language Learning"
        override val description = "Специальный режим для изучения языков с отслеживанием прогресса"
    }
}

data class DialogFact(
    val key: String,
    val value: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DialogBranch(
    val id: String,
    val name: String,
    val checkpointMessageId: String,
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)