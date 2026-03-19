package com.example.aiagent.data.rag

import kotlinx.serialization.Serializable

/**
 * Стратегия чанкинга для RAG
 */
enum class RagStrategy {
    FIXED,
    STRUCTURAL
}

/**
 * Запрос на построение RAG индекса
 */
@Serializable
data class BuildRagIndexRequest(
    val strategy: String
)

/**
 * Ответ на построение RAG индекса
 */
@Serializable
data class BuildRagIndexResponse(
    val status: String = "success",
    val message: String = "",
    val chunksCount: Int? = null,
    val strategy: String? = null
)

/**
 * Запрос на вопрос к документам
 */
@Serializable
data class AskDocumentsRequest(
    val question: String,
    val strategy: String
)

/**
 * Информация о документе-источнике
 */
@Serializable
data class RagDocumentSource(
    val filename: String,
    val chunkId: Int,
    val page: Int? = null,
    val section: String? = null,
    val relevanceScore: Double? = null
)

/**
 * Ответ на вопрос к документам
 */
@Serializable
data class AskDocumentsResponse(
    val answer: String,
    val sources: List<RagDocumentSource>,
    val strategy: String,
    val chunksUsed: Int,
    val queryTimeMs: Long
)

/**
 * Запрос на сравнение стратегий
 */
@Serializable
data class CompareStrategiesRequest(
    val question: String
)

/**
 * Результат одной стратегии при сравнении
 */
@Serializable
data class StrategyResult(
    val strategy: String,
    val answer: String,
    val sources: List<RagDocumentSource>,
    val chunksUsed: Int,
    val relevanceScore: Double
)

/**
 * Ответ на сравнение стратегий
 */
@Serializable
data class CompareStrategiesResponse(
    val question: String,
    val results: List<StrategyResult>,
    val recommendedStrategy: String,
    val comparisonTimeMs: Long
)

/**
 * Контекст с найденными документами для передачи в LLM
 */
data class RagContext(
    val strategy: RagStrategy,
    val documents: List<RagDocumentSource>,
    val chunksUsed: Int,
    val formattedContext: String
) {
    /**
     * Форматирует контекст для включения в промпт LLM
     */
    fun formatForPrompt(): String {
        return buildString {
            appendLine("\n=== КОНТЕКСТ ИЗ ДОКУМЕНТОВ ===")
            appendLine("Стратегия чанкинга: ${strategy.name.lowercase()}")
            appendLine("Использовано фрагментов: $chunksUsed")
            appendLine("Источники:")
            documents.forEachIndexed { index, source ->
                appendLine("  ${index + 1}. ${source.filename}")
                source.page?.let { appendLine("     Страница: $it") }
                source.section?.let { appendLine("     Раздел: $it") }
                source.relevanceScore?.let { 
                    appendLine("     Релевантность: ${"%.2f".format(it * 100)}%")
                }
            }
            appendLine("=== КОНЕЦ КОНТЕКСТА ===\n")
        }
    }
}
