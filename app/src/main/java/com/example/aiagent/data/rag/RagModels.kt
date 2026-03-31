package com.example.aiagent.data.rag

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    val name: String = "build_rag_index",
    val arguments: BuildRagIndexArguments
)

@Serializable
data class BuildRagIndexArguments(
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
    val name: String = "ask_documents",
    val arguments: AskDocumentsArguments
)

@Serializable
data class AskDocumentsArguments(
    val question: String,
    val strategy: String,
    val rerank_method: String = "hybrid",
    val initial_top_k: Int = 10,
    val final_top_k: Int = 3
)

/**
 * Информация о документе-источнике
 */
@Serializable
data class RagDocumentSource(
    val file: String,
    val chunk_id: Int,
    val section: String,
    val relevance: Double
)

/**
 * Ответ на вопрос к документам (новый формат)
 */
@Serializable
data class AskDocumentsResponse(
    val answer: String,
    val sources: List<RagDocumentSource>,
    val quotes: List<String>
)

/**
 * Обертка для ответа от сервера
 * result может быть строкой (для build_rag_index) или объектом (для ask_documents)
 */
@Serializable
data class RagServerResponse(
    val success: Boolean,
    val result: JsonElement? = null,  // Может быть строкой или объектом
    val server: String? = null,
    val tool: String? = null
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
    val formattedContext: String,
    val quotes: List<String> = emptyList(),
    val ragAnswer: String = ""  // Ответ от RAG сервера
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
                appendLine("  ${index + 1}. ${source.file}")
                appendLine("     Раздел: ${source.section}")
                appendLine("     ID чанка: ${source.chunk_id}")
                appendLine("     Релевантность: ${"%.2f".format(source.relevance * 100)}%")
            }
            if (quotes.isNotEmpty()) {
                appendLine("\nЦитаты:")
                quotes.forEachIndexed { index, quote ->
                    appendLine("  ${index + 1}. ${quote.take(150)}${if (quote.length > 150) "..." else ""}")
                }
            }
            appendLine("=== КОНЕЦ КОНТЕКСТА ===\n")
        }
    }
}

/**
 * Pull Request информация
 */
@Serializable
data class PullRequest(
    val pr_number: String,
    val repo: String,
    val changed_files: List<String>,
    val created_at: String
)

/**
 * Ответ с списком ожидающих PR
 */
@Serializable
data class PendingPrsResponse(
    val success: Boolean,
    val count: Int,
    val prs: List<PullRequest>
)

/**
 * Детальная информация о PR
 */
@Serializable
data class PrInfo(
    val pr_number: String,
    val repo: String,
    val changed_files: List<String>,
    val prompt: String,
    val status: String
)

/**
 * Ответ с детальной информацией о PR
 */
@Serializable
data class PrInfoResponse(
    val success: Boolean,
    val pr_number: String,
    val repo: String,
    val changed_files: List<String>,
    val prompt: String,
    val status: String
)

/**
 * Ответ с diff конкретного PR
 */
@Serializable
data class PrDiffResponse(
    val success: Boolean,
    val pr_number: String,
    val diff: String
)
