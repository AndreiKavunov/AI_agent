package com.example.aiagent.data.huggingFace

import android.util.Log
import com.example.aiagent.BuildConfig
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

class HuggingFaceRepositoryImpl : ChatRepository, HuggingFaceRepository {

    private val TAG = "HuggingFace"
    private val BASE_URL = "https://router.huggingface.co/hf-inference/models"
    private val CHAT_BASE_URL = "https://router.huggingface.co/v1/chat/completions"

    private val apiToken = BuildConfig.HF_TOKEN

    private var currentModel: HuggingFaceModel = HuggingFaceModel.MEDIUM
    private val messageHistory = mutableListOf<HuggingFaceMessage>()

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

    override suspend fun sendMessage(message: String): AgentResponse {
        return sendMessage(message, 0.7)
    }

    override suspend fun sendMessage(message: String, temperature: Double): AgentResponse {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🚀 Отправляем запрос к модели: ${currentModel.displayName}")
        Log.d(TAG, "📝 Сообщение: \"${message.take(50)}...\"")
        Log.d(TAG, "📊 Тип задачи: ${currentModel.taskType}")

        var tokenCount = 0
        var responseText = ""
        var responseTime = 0L

        return withContext(Dispatchers.IO) {
            val client = createClient()

            try {
                val response = when (currentModel.taskType) {
                    TaskType.TEXT_CLASSIFICATION -> queryClassificationModel(client, message)
                    TaskType.TEXT_GENERATION -> queryTextGenerationModel(client, message, temperature)
                    TaskType.QUESTION_ANSWERING -> queryQuestionAnsweringModel(client, message)
                }

                responseText = response
                tokenCount = estimateTokenCount(response)

                val endTime = System.currentTimeMillis()
                responseTime = endTime - startTime

                Log.d(TAG, "⏱️ Время ответа: ${responseTime}ms")
                Log.d(TAG, "📊 Токенов в ответе: ~$tokenCount")
                Log.d(TAG, "⚡ Токенов/сек: ${String.format("%.2f", tokenCount / (responseTime/1000.0))}")

                AgentResponse(
                    text = buildString {
                        append("【${currentModel.displayName}】\n")
                        append("⏱️ ${responseTime}ms | 📊 ${tokenCount} токенов | ⚡ ${String.format("%.1f", tokenCount / (responseTime/1000.0))} ток/с\n")
                        append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
                        append(response)
                    },
                    toolUsed = detectTool(message),
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

    override fun clearChatHistory() {
        messageHistory.clear()
        Log.d(TAG, "🧹 История чата очищена")
    }

    override fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentModel = modelType
        Log.d(TAG, "🔄 Модель HuggingFace изменена на: ${modelType.displayName}")

        if (modelType.taskType == TaskType.TEXT_GENERATION && messageHistory.isEmpty()) {
            messageHistory.add(
                HuggingFaceMessage(
                    role = "system",
                    content = "Ты полезный ассистент. Отвечай кратко и по делу на русском языке."
                )
            )
        }
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel = currentModel

    // Для моделей классификации (слабая)
    private suspend fun queryClassificationModel(client: HttpClient, message: String): String {
        val url = "$BASE_URL/${currentModel.modelId}"

        val requestBody = buildJsonObject {
            put("inputs", message)
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
            formatClassificationResponse(message, predictions)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
            "Анализ: ${jsonString.take(200)}"
        }
    }

    // Для генеративных моделей через новый эндпоинт
    private suspend fun queryTextGenerationModel(
        client: HttpClient,
        message: String,
        temperature: Double
    ): String {
        messageHistory.add(HuggingFaceMessage(role = "user", content = message))

        val url = CHAT_BASE_URL

        // Формируем историю сообщений в формате OpenAI
        val messagesForApi = messageHistory.map { msg ->
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
            messageHistory.removeLast()
            throw Exception("Ошибка ${currentModel.displayName}: ${response.status} - $error")
        }

        val jsonString = response.bodyAsText()

        return try {
            val answer = parseChatCompletionResponse(jsonString)
            messageHistory.add(HuggingFaceMessage(role = "assistant", content = answer))
            answer
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
            messageHistory.removeLast()
            throw Exception("Ошибка обработки ответа: ${e.message}")
        }
    }

    private suspend fun queryQuestionAnsweringModel(client: HttpClient, message: String): String {
        val context = "Здесь должен быть контекст для ответа на вопрос"

        val requestBody = buildJsonObject {
            put("inputs", buildJsonObject {
                put("question", message)
                put("context", context)
            })
        }

        val url = "$BASE_URL/${currentModel.modelId}"

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiToken")
            setBody(requestBody)
        }

        return "QA функционал в разработке"
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
                jsonString
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

// Data классы
//data class HuggingFaceMessage(
//    val role: String,
//    val content: String
//)
//
//data class ClassificationResponse(
//    val label: String,
//    val score: Double
//)