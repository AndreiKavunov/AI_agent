// domain/agent/memory/LanguageFactExtractor.kt
package com.example.aiagent.domain.agent.memory

import android.util.Log
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.local.LocalModelRepository
import com.example.aiagent.domain.RepositoryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

private const val TAG = "LanguageFactExtractor"

/**
 * Извлекает факты, специфичные для изучения языка, из сообщений пользователя
 */
class LanguageFactExtractor(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl,
    private val localModelRepository: LocalModelRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Извлекает языковые факты из сообщения
     */
    suspend fun extractLanguageFacts(
        message: String,
        repositoryType: RepositoryType,
        modelName: String? = null
    ): ExtractedLanguageFacts {
        Log.d(TAG, "🔍 Извлечение языковых фактов из: $message")

        val prompt = """
            Извлеки языковые факты из сообщения пользователя в контексте изучения языка.
            
            Сообщение: "$message"
            
            Верни ТОЛЬКО JSON с найденными фактами по следующей схеме:
            {
                "vocabulary_words": [
                    {
                        "word": "слово или фраза",
                        "translation": "перевод",
                        "part_of_speech": "часть речи (noun, verb, adjective, etc.)",
                        "example": "пример использования",
                        "topic": "тема (restaurant, travel, etc.)"
                    }
                ],
                "grammar_rules": [
                    {
                        "rule": "название правила",
                        "description": "описание правила",
                        "example": "пример"
                    }
                ],
                "mistakes": [
                    {
                        "error": "ошибочная фраза",
                        "correction": "правильный вариант",
                        "rule": "грамматическое правило",
                        "explanation": "объяснение"
                    }
                ],
                "lesson_topics": [
                    {
                        "topic": "тема урока",
                        "subtopic": "подтема",
                        "difficulty": "уровень (A1-C2)"
                    }
                ],
                "learning_goals": [
                    "цель обучения"
                ],
                "exercise_context": {
                    "exercise_type": "тип упражнения",
                    "exercise_id": "идентификатор упражнения"
                }
            }
            
            Если факт не найден, оставь поле пустым или null.
            Верни только JSON, без пояснений и без markdown.
        """.trimIndent()

        val history = listOf(
            GigaMessage(
                role = "system",
                content = "Ты помощник, который извлекает языковые факты из сообщений и возвращает их в JSON формате. Никогда не добавляй пояснения, только JSON."
            ),
            GigaMessage(role = "user", content = prompt)
        )

        return try {
            val response = when (repositoryType) {
                RepositoryType.GIGACHAT -> {
                    gigaChatRepository.sendMessageWithHistory(
                        history = history,
                        temperature = 0.3 // Низкая температура для консистентности
                    )
                }
                RepositoryType.HUGGINGFACE -> {
                    huggingFaceRepository.sendMessageWithHistory(
                        history = history,
                        temperature = 0.3,
                    )
                }
                RepositoryType.LOCAL -> {
                    localModelRepository.sendMessageWithHistory(
                        history = history,
                        temperature = 0.3
                    )
                }
            }

            Log.d(TAG, "📥 Ответ от LLM: ${response.text}")

            // Парсим JSON из ответа
            val jsonText = extractJsonFromResponse(response.text)
            Log.d(TAG, "📊 Извлеченный JSON: $jsonText")

            val facts = json.decodeFromString<ExtractedLanguageFacts>(jsonText)
            Log.d(TAG, "✅ Извлечены языковые факты: $facts")

            facts

        } catch (e: SerializationException) {
            Log.e(TAG, "❌ Ошибка парсинга JSON: ${e.message}")
            ExtractedLanguageFacts()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка извлечения языковых фактов: ${e.message}")
            ExtractedLanguageFacts()
        }
    }

    /**
     * Определяет тип памяти для хранения факта
     */
    fun determineMemoryType(fact: ExtractedLanguageFacts): MemoryType {
        return when {
            fact.mistakes.isNotEmpty() -> MemoryType.WORKING_MEMORY
            fact.lessonTopics.isNotEmpty() -> MemoryType.WORKING_MEMORY
            fact.exerciseContext != null -> MemoryType.WORKING_MEMORY
            fact.vocabularyWords.isNotEmpty() -> MemoryType.LONG_TERM_MEMORY
            fact.grammarRules.isNotEmpty() -> MemoryType.LONG_TERM_MEMORY
            fact.learningGoals.isNotEmpty() -> MemoryType.LONG_TERM_MEMORY
            else -> MemoryType.SHORT_TERM_MEMORY
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        val jsonRegex = """(\{.*\})""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL))
        return jsonRegex.find(text)?.value?.trim() ?: run {
            val lastBrace = text.lastIndexOf('{')
            val lastBraceEnd = text.lastIndexOf('}')
            if (lastBrace >= 0 && lastBraceEnd > lastBrace) {
                text.substring(lastBrace, lastBraceEnd + 1)
            } else {
                "{}"
            }
        }
    }
}

/**
 * Извлеченные языковые факты
 */
@Serializable
data class ExtractedLanguageFacts(
    val vocabularyWords: List<ExtractedVocabularyWord> = emptyList(),
    val grammarRules: List<ExtractedGrammarRule> = emptyList(),
    val mistakes: List<ExtractedMistake> = emptyList(),
    val lessonTopics: List<ExtractedLessonTopic> = emptyList(),
    val learningGoals: List<String> = emptyList(),
    val exerciseContext: ExtractedExerciseContext? = null
)

@Serializable
data class ExtractedVocabularyWord(
    val word: String,
    val translation: String? = null,
    val part_of_speech: String? = null,
    val example: String? = null,
    val topic: String? = null
)

@Serializable
data class ExtractedGrammarRule(
    val rule: String,
    val description: String? = null,
    val example: String? = null
)

@Serializable
data class ExtractedMistake(
    val error: String,
    val correction: String,
    val rule: String? = null,
    val explanation: String? = null
)

@Serializable
data class ExtractedLessonTopic(
    val topic: String,
    val subtopic: String? = null,
    val difficulty: String? = null
)

@Serializable
data class ExtractedExerciseContext(
    val exercise_type: String? = null,
    val exercise_id: String? = null
)

enum class MemoryType {
    SHORT_TERM_MEMORY,
    WORKING_MEMORY,
    LONG_TERM_MEMORY
}
