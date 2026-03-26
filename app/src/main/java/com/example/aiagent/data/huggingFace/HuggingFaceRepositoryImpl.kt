// data/huggingFace/HuggingFaceRepositoryImpl.kt
package com.example.aiagent.data.huggingFace

import android.util.Log
import com.example.aiagent.BuildConfig
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.ChatRepository
import com.example.aiagent.domain.HuggingFaceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "HuggingFace"

class HuggingFaceRepositoryImpl : ChatRepository, HuggingFaceRepository {

    private val BASE_URL = "https://router.huggingface.co/hf-inference/models"
    private val CHAT_BASE_URL = "https://router.huggingface.co/v1/chat/completions"

    private val apiToken = BuildConfig.HF_TOKEN

    private var currentModel: HuggingFaceModel = HuggingFaceModel.MEDIUM

    companion object {
        @Volatile
        private var instance: HuggingFaceRepositoryImpl? = null

        fun getInstance(): HuggingFaceRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: HuggingFaceRepositoryImpl().also { instance = it }
            }
        }
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 120000L // Увеличиваем таймаут для больших моделей
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = 120000L
            }

            install(HttpRequestRetry) {
                maxRetries = 2
                delayMillis { retry -> retry * 2000L }
            }

            install(ContentNegotiation) {
                json(jsonParser)
            }

            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("Ktor-HF", message)
                    }
                }
            }
        }
    }

    // ОСНОВНОЙ МЕТОД - принимает историю от агента
    override suspend fun sendMessageWithHistory(
        history: List<GigaMessage>,
        temperature: Double,
        maxTokens: Int
    ): AgentResponse {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🚀 HuggingFace получает историю из ${history.size} сообщений")
        Log.d(TAG, "📝 Модель: ${currentModel.displayName}")
        Log.d(TAG, "📊 Тип задачи: ${currentModel.taskType}")
        Log.d(TAG, "🌡️ Температура: $temperature")

        return withContext(Dispatchers.IO) {
            val client = createClient()

            try {
                val response = when (currentModel.taskType) {
                    TaskType.TEXT_CLASSIFICATION -> queryClassificationModel(client, history)
                    TaskType.TEXT_GENERATION -> queryTextGenerationModel(client, history, temperature)
                    TaskType.QUESTION_ANSWERING -> queryQuestionAnsweringModel(client, history)
                }

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                val tokenCount = estimateTokenCount(response)

                Log.d(TAG, "✅ Успех! Ответ получен")
                Log.d(TAG, "⏱️ Время ответа: ${responseTime}ms")
                Log.d(TAG, "📊 Токенов: ~$tokenCount")

                AgentResponse(
                    text = response,
                    toolUsed = detectTool(history.lastOrNull { it.role == "user" }?.content ?: ""),
                    responseTimeMs = responseTime,
                    tokenCount = tokenCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка: ${e.message}")
                e.printStackTrace()
                throw e
            } finally {
                client.close()
            }
        }
    }

    // Deprecated методы для обратной совместимости
    @Deprecated("Use sendMessageWithHistory instead", ReplaceWith("sendMessageWithHistory(listOf(GigaMessage(role = \"user\", content = message)), temperature)"))
    override suspend fun sendMessage(message: String): AgentResponse {
        return sendMessage(message, 0.7)
    }

    @Deprecated("Use sendMessageWithHistory instead", ReplaceWith("sendMessageWithHistory(listOf(GigaMessage(role = \"user\", content = message)), temperature)"))
    override suspend fun sendMessage(message: String, temperature: Double): AgentResponse {
        // Создаем историю из одного сообщения для обратной совместимости
        val history = listOf(
            GigaMessage(role = "user", content = message)
        )
        return sendMessageWithHistory(history, temperature, maxTokens = 512)
    }

    override fun clearChatHistory() {
        // Репозиторий больше не хранит историю
        Log.d(TAG, "clearChatHistory вызван, но репозиторий не хранит историю")
    }

    // Реализация HuggingFaceRepository
    override fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentModel = modelType
        Log.d(TAG, "🔄 Модель HuggingFace изменена на: ${modelType.displayName}")
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel = currentModel

    // Для генеративных моделей через chat completion endpoint
    private suspend fun queryTextGenerationModel(
        client: HttpClient,
        history: List<GigaMessage>,
        temperature: Double
    ): String {
        val url = CHAT_BASE_URL

        // Конвертируем GigaMessage в формат для API
        val messagesForApi = history.map { msg ->
            buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }

        val requestBody = buildJsonObject {
            put("model", currentModel.modelId)
            put("messages", JsonArray(messagesForApi))
            put("temperature", temperature)
            put("max_tokens", 1000)
            put("top_p", 0.95)
        }

        Log.d(TAG, "📤 URL чата: $url")
        Log.d(TAG, "📤 Модель: ${currentModel.modelId}")

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiToken")
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            Log.e(TAG, "❌ Ошибка модели: $error")
            throw Exception("Ошибка ${currentModel.displayName}: ${response.status} - $error")
        }

        val jsonString = response.bodyAsText()
        return parseChatCompletionResponse(jsonString)
    }

    // Для моделей классификации
    private suspend fun queryClassificationModel(
        client: HttpClient,
        history: List<GigaMessage>
    ): String {
        // Берем последнее сообщение пользователя для классификации
        val lastUserMessage = history.lastOrNull { it.role == "user" }?.content ?: ""

        val url = "$BASE_URL/${currentModel.modelId}"

        val requestBody = buildJsonObject {
            put("inputs", lastUserMessage)
        }

        Log.d(TAG, "📤 URL классификации: $url")

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiToken")
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            Log.e(TAG, "Ошибка классификации: $error")
            throw Exception("Ошибка ${currentModel.displayName}: ${response.status}")
        }

        val jsonString = response.bodyAsText()

        return try {
            val predictions = jsonParser.decodeFromString<List<ClassificationResponse>>(jsonString)
            formatClassificationResponse(lastUserMessage, predictions)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
            "Анализ: ${jsonString.take(200)}"
        }
    }

    // Для моделей вопрос-ответ
    private suspend fun queryQuestionAnsweringModel(
        client: HttpClient,
        history: List<GigaMessage>
    ): String {
        // Здесь нужен контекст для ответа на вопрос
        // В реальном приложении контекст должен передаваться отдельно
        val lastUserMessage = history.lastOrNull { it.role == "user" }?.content ?: ""
        val context = "Контекст для ответа на вопрос временно недоступен."

        val requestBody = buildJsonObject {
            put("inputs", buildJsonObject {
                put("question", lastUserMessage)
                put("context", context)
            })
        }

        val url = "$BASE_URL/${currentModel.modelId}"

        Log.d(TAG, "📤 URL QA: $url")
        Log.d(TAG, "⚠️ QA функционал требует передачи контекста")

        return "❌ Функционал вопрос-ответ временно недоступен. Пожалуйста, используйте текстовую модель."
    }

    private fun parseChatCompletionResponse(jsonString: String): String {
        return try {
            val jsonElement = jsonParser.parseToJsonElement(jsonString)
            val choices = jsonElement.jsonObject["choices"]?.jsonArray

            if (!choices.isNullOrEmpty()) {
                val firstChoice = choices.first().jsonObject
                val message = firstChoice["message"]?.jsonObject
                message?.get("content")?.jsonPrimitive?.content ?: jsonString
            } else {
                // Fallback для старых моделей, которые возвращают generated_text
                jsonElement.jsonObject["generated_text"]?.jsonPrimitive?.content ?: jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга chat completion: ${e.message}")
            jsonString
        }
    }

    private fun formatClassificationResponse(query: String, predictions: List<ClassificationResponse>): String {
        val builder = StringBuilder()
        builder.append("📊 Анализ тональности:\n")
        builder.append("   \"${query.take(100)}\"\n\n")

        predictions.forEachIndexed { index, pred ->
            val sentiment = when (pred.label.lowercase()) {
                "positive" -> "😊 ПОЗИТИВНЫЙ"
                "negative" -> "😠 НЕГАТИВНЫЙ"
                "neutral" -> "😐 НЕЙТРАЛЬНЫЙ"
                "LABEL_0" -> "😐 НЕЙТРАЛЬНЫЙ"
                "LABEL_1" -> "😊 ПОЗИТИВНЫЙ"
                "LABEL_2" -> "😠 НЕГАТИВНЫЙ"
                else -> pred.label.uppercase()
            }
            val score = pred.score * 100
            builder.append("   ${index + 1}. $sentiment: ${"%.1f".format(score)}%\n")
        }

        return builder.toString()
    }

    private fun estimateTokenCount(text: String): Int {
        // Грубая оценка: для русского и английского ~4 символа на токен
        return (text.length / 4).coerceAtLeast(1)
    }

    private fun detectTool(message: String): String? {
        val normalized = message.lowercase()
        return when {
            normalized.contains("погод") -> "get_weather"
            normalized.contains("считай") || normalized.contains("посчитай") ||
                    normalized.contains("сколько будет") -> "calculate"
            normalized.contains("курс") || normalized.contains("доллар") ||
                    normalized.contains("евро") -> "get_exchange_rate"
            normalized.contains("перевод") || normalized.contains("translate") -> "translate"
            else -> null
        }
    }
}