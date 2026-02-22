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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Data классы для разных типов ответов

class HuggingFaceRepositoryImpl : ChatRepository, HuggingFaceRepository {

    private val TAG = "HuggingFace"
//    private val BASE_URL = "https://router.huggingface.co/hf-inference/models"
    private val BASE_URL = "https://api-inference.huggingface.co/models/"
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
                requestTimeoutMillis = 60000L
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = 60000L
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
        Log.d(TAG, "🚀 Отправляем запрос к модели: ${currentModel.displayName}")
        Log.d(TAG, "📝 Сообщение: \"${message.take(50)}...\"")
        Log.d(TAG, "📊 Тип задачи: ${currentModel.taskType}")

        return withContext(Dispatchers.IO) {
            val client = createClient()

            try {
                val response = when (currentModel.taskType) {
                    TaskType.TEXT_CLASSIFICATION -> queryClassificationModel(client, message)
                    TaskType.TEXT_GENERATION -> queryTextGenerationModel(client, message, temperature)
                    TaskType.QUESTION_ANSWERING -> queryQuestionAnsweringModel(client, message)
                }

                AgentResponse(
                    text = response,
                    toolUsed = detectTool(message)
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
        Log.d(TAG, "📤 Запрос: $requestBody")

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
        Log.d(TAG, "📥 Ответ: $jsonString")

        return try {
            val predictions = jsonParser.decodeFromString<List<ClassificationResponse>>(jsonString)
            formatClassificationResponse(message, predictions)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
            "Анализ: ${jsonString.take(200)}"
        }
    }

    private suspend fun queryTextGenerationModel(
        client: HttpClient,
        message: String,
        temperature: Double
    ): String {
        messageHistory.add(HuggingFaceMessage(role = "user", content = message))

        // Используем НОВЫЙ URL для чат-комплейшнс
        val url = CHAT_BASE_URL

        // Формируем историю сообщений в формате, который понимают современные модели
        val messagesForApi = messageHistory.map { msg ->
            buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }

        val requestBody = buildJsonObject {
            put("model", currentModel.modelId) // Здесь указываем ID модели
            put("messages", JsonArray(messagesForApi))
            putJsonObject("parameters") { // Параметры могут быть вложены или на верхнем уровне, но часто достаточно так
                put("temperature", temperature)
                put("max_tokens", 500)
                put("top_p", 0.95)
                // Другие параметры
            }
            // Параметры для ожидания модели
            putJsonObject("options") {
                put("wait_for_model", true)
            }
        }

        Log.d(TAG, "📤 URL (чат): $url")
        Log.d(TAG, "📤 Модель: ${currentModel.modelId}")
        Log.d(TAG, "📤 Сообщения: $messagesForApi")

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
        Log.d(TAG, "📥 Ответ: $jsonString")

        return try {
            // Парсим ответ в новом формате OpenAI
            val answer = parseChatCompletionResponse(jsonString)
            messageHistory.add(HuggingFaceMessage(role = "assistant", content = answer))
            answer
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
            messageHistory.removeLast()
            throw Exception("Ошибка обработки ответа: ${e.message}")
        }
    }

    // Новая функция парсинга для OpenAI-совместимого ответа
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

    private fun parseGenerationResponse(jsonString: String): String {
        return try {
            val jsonElement = jsonParser.parseToJsonElement(jsonString)

            when {
                jsonElement.jsonArray.isNotEmpty() -> {
                    val firstElement = jsonElement.jsonArray.first()
                    firstElement.jsonObject["generated_text"]?.jsonPrimitive?.content
                        ?: firstElement.toString()
                }
                jsonElement.jsonObject.containsKey("generated_text") -> {
                    jsonElement.jsonObject["generated_text"]?.jsonPrimitive?.content
                        ?: jsonString
                }
                else -> jsonString
            }
        } catch (e: Exception) {
            jsonString
        }
    }

    // Для вопросно-ответных моделей (можно добавить позже)
    private suspend fun queryQuestionAnsweringModel(client: HttpClient, message: String): String {
        // Для QA моделей нужен контекст
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

    private fun formatPromptForModel(message: String): String {
        return when (currentModel) {
            HuggingFaceModel.MEDIUM -> {
                // Формат для SmolLM2 (ChatML формат)
                buildString {
                    messageHistory.forEach { msg ->
                        when (msg.role) {
                            "system" -> append("<|im_start|>system\n${msg.content}<|im_end|>\n")
                            "user" -> append("<|im_start|>user\n${msg.content}<|im_end|>\n")
                            "assistant" -> append("<|im_start|>assistant\n${msg.content}<|im_end|>\n")
                        }
                    }
                    append("<|im_start|>assistant\n")
                }
            }

            HuggingFaceModel.STRONG -> {
                // Правильный формат для Mistral-Instruct
                buildString {
                    var firstUserMessage = true

                    messageHistory.forEachIndexed { index, msg ->
                        when (msg.role) {
                            "system" -> {
                                if (index == 0) {
                                    append("<s>[INST] <<SYS>>\n${msg.content}\n<</SYS>>\n\n")
                                } else {
                                    append("<<SYS>>\n${msg.content}\n<</SYS>>\n\n")
                                }
                            }
                            "user" -> {
                                if (firstUserMessage) {
                                    append("${msg.content} [/INST] ")
                                    firstUserMessage = false
                                } else {
                                    append("</s><s>[INST] ${msg.content} [/INST] ")
                                }
                            }
                            "assistant" -> {
                                append("${msg.content}")
                            }
                        }
                    }
                }
            }

            else -> message
        }
    }

    private fun extractGenerationResponse(jsonString: String): String {
        return try {
            val jsonElement = jsonParser.parseToJsonElement(jsonString)

            when {
                jsonElement.jsonArray.isNotEmpty() -> {
                    val firstElement = jsonElement.jsonArray.first()
                    val generatedText = firstElement.jsonObject["generated_text"]?.jsonPrimitive?.content
                    generatedText?.trim() ?: firstElement.toString()
                }

                jsonElement.jsonObject.isNotEmpty() -> {
                    val obj = jsonElement.jsonObject
                    val generatedText = obj["generated_text"]?.jsonPrimitive?.content
                    generatedText?.trim() ?: obj.toString()
                }

                else -> jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга: ${e.message}")
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