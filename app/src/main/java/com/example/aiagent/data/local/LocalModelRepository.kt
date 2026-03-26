// data/local/LocalModelRepository.kt
package com.example.aiagent.data.local

import android.util.Log
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.ChatRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private const val TAG = "LocalModel"

/**
 * Репозиторий для работы с локальной моделью через Ollama API
 * API endpoint: http://192.168.0.82:11434/api/generate
 */
class LocalModelRepository : ChatRepository {

    private val baseUrl = "http://192.168.0.82:11434"
    private val defaultModel = "qwen2.5:3b"

    companion object {
        @Volatile
        private var instance: LocalModelRepository? = null

        fun getInstance(): LocalModelRepository {
            return instance ?: synchronized(this) {
                instance ?: LocalModelRepository().also { instance = it }
            }
        }
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(jsonParser)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 300000L  // 5 минут для локальной модели (увеличено)
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = 300000L
            }

            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    override suspend fun sendMessageWithHistory(
        history: List<GigaMessage>,
        temperature: Double,
        maxTokens: Int
    ): AgentResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // Формируем промпт из истории сообщений
            val prompt = buildPromptFromHistory(history)

            // Создаем запрос к Ollama API
            val request = OllamaRequest(
                model = defaultModel,
                prompt = prompt,
                stream = false,
                options = OllamaOptions(
                    temperature = temperature,
                    num_predict = maxTokens
                )
            )

            Log.d(TAG, "Отправка запроса к локальной модели: $baseUrl/api/generate")
            Log.d(TAG, "Модель: ${request.model}, Промпт (первые 200 символов): ${request.prompt.take(200)}...")
            Log.d(TAG, "Опции: temperature=${request.options?.temperature}, num_predict=${request.options?.num_predict}")

            val response = client.post("$baseUrl/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseTime = System.currentTimeMillis() - startTime

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "Ошибка API: ${response.status} - $errorBody")
                throw Exception("Ошибка API локальной модели: ${response.status} - $errorBody")
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "Ответ API (первые 200 символов): ${responseBody.take(200)}...")

            val ollamaResponse = jsonParser.decodeFromString<OllamaResponse>(responseBody)

            if (!ollamaResponse.done) {
                Log.w(TAG, "Ответ не завершен (done=false)")
            }

            // Оцениваем количество токенов (приблизительно 4 символа на токен)
            val estimatedTokens = (ollamaResponse.response.length / 4).coerceAtLeast(1)

            Log.d(TAG, "Получен ответ от локальной модели. Длина: ${ollamaResponse.response.length} символов, Токены: ~$estimatedTokens")

            AgentResponse(
                text = ollamaResponse.response,
                toolUsed = null,
                responseTimeMs = responseTime,
                tokenCount = estimatedTokens
            )

        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "Ошибка при отправке сообщения локальной модели", e)
            throw Exception("Не удалось получить ответ от локальной модели: ${e.message}", e)
        }
    }

    @Deprecated("Use sendMessageWithHistory instead")
    override suspend fun sendMessage(message: String): AgentResponse {
        return sendMessageWithHistory(
            history = listOf(GigaMessage(role = "user", content = message)),
            temperature = 0.7,
            maxTokens = 512
        )
    }

    @Deprecated("Use sendMessageWithHistory instead")
    override suspend fun sendMessage(message: String, temperature: Double): AgentResponse {
        return sendMessageWithHistory(
            history = listOf(GigaMessage(role = "user", content = message)),
            temperature = temperature,
            maxTokens = 512
        )
    }

    override fun clearChatHistory() {
        // Локальная модель не хранит историю на своей стороне
        Log.d(TAG, "clearChatHistory: история локальной модели очищена (нечего делать)")
    }

    /**
     * Формирует промпт из истории сообщений
     * Ollama API принимает только один промпт, поэтому объединяем историю
     * Для локальной модели убираем системные сообщения и промпты извлечения фактов
     * Отправляем только текст сообщения пользователя без дополнительных инструкций
     */
    private fun buildPromptFromHistory(history: List<GigaMessage>): String {
        return buildString {
            for (message in history) {
                when (message.role) {
                    "user" -> {
                        // Проверяем, не является ли сообщение промптом для извлечения фактов
                        val content = message.content.trim()
                        if (content.contains("Извлеки факты") ||
                            content.contains("Верни ТОЛЬКО JSON") ||
                            content.contains("Извлеки языковые факты")) {
                            // Пропускаем промпты извлечения фактов
                            Log.d(TAG, "Пропущен промпт извлечения фактов")
                        } else {
                            // Отправляем только чистый текст сообщения
                            append(content)
                        }
                    }
                    "assistant" -> append("${message.content}\n\n")
                    "system" -> {
                        // Пропускаем системные сообщения для локальной модели
                        Log.d(TAG, "Пропущено системное сообщение: ${message.content.take(100)}...")
                    }
                }
            }
        }
    }
}

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    val num_predict: Int? = null
)

@Serializable
data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean
)
