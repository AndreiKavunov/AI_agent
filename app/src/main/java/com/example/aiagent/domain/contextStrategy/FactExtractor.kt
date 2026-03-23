// domain/contextStrategy/FactExtractor.kt
package com.example.aiagent.domain.contextStrategy

import android.util.Log
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.local.LocalModelRepository
import com.example.aiagent.domain.RepositoryType
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

private const val TAG = "FactExtractor"

class FactExtractor(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl,
    private val localModelRepository: LocalModelRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun extractFacts(
        message: String,
        repositoryType: RepositoryType,
        modelName: String? = null
    ): ExtractedFacts {
        Log.d(TAG, "🔍 Извлечение фактов из: $message")

        val prompt = """
            Извлеки факты из сообщения пользователя. 
            
            Сообщение: "$message"
            
            Верни ТОЛЬКО JSON с найденными фактами по следующей схеме:
            {
                "user_name": "имя если пользователь представился",
                "user_full_name": "полное имя если указано",
                "age": число (возраст),
                "likes": ["список того, что нравится"],
                "dislikes": ["список того, что не нравится"],
                "goals": ["список целей"],
                "limitations": ["список ограничений"],
                "occupation": "чем занимается (работа/учеба)",
                "profession": "профессия",
                "agreements": ["список договоренностей"],
                "decisions": ["список решений"]
            }
            
            Если факт не найден, оставь поле пустым или null.
            Верни только JSON, без пояснений и без markdown.
        """.trimIndent()

        val history = listOf(
            GigaMessage(
                role = "system",
                content = "Ты помощник, который извлекает факты из сообщений и возвращает их в JSON формате. Никогда не добавляй пояснения, только JSON."
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

            val facts = json.decodeFromString<ExtractedFacts>(jsonText)
            Log.d(TAG, "✅ Извлечены факты: $facts")

            facts

        } catch (e: SerializationException) {
            Log.e(TAG, "❌ Ошибка парсинга JSON: ${e.message}")
            ExtractedFacts()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка извлечения фактов: ${e.message}")
            ExtractedFacts()
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        // Ищем JSON объект в ответе (между { и })
        val jsonRegex = """(\{.*\})""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL))
        return jsonRegex.find(text)?.value?.trim() ?: run {
            // Если не нашли, пробуем найти после последнего {
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