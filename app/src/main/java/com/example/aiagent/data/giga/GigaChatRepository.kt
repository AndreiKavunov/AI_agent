package com.example.aiagent.data.giga

import android.util.Log
import com.example.aiagent.BuildConfig
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.ChatRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.X509TrustManager

class GigaChatRepository : ChatRepository {
    private val authKey = BuildConfig.GIGACHAT_AUTH_KEY
    private val TAG = "GigaChat"
    private val TOKEN_TTL = 25 * 60 * 1000L // 25 минут в миллисекундах

    // Кеш токена в памяти
    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0

    // История сообщений - сохраняется в синглтоне
    private val messageHistory = mutableListOf<GigaMessage>()

    companion object {
        @Volatile
        private var instance: GigaChatRepository? = null

        fun getInstance(): GigaChatRepository {
            return instance ?: synchronized(this) {
                instance ?: GigaChatRepository().also { instance = it }
            }
        }
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                https {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) = Unit

                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) = Unit

                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                }

                endpoint {
                    connectTimeout = 30000
                    requestTimeout = 45000
                    socketTimeout = 45000
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 45000L
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = 45000L
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
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        Log.d("Ktor", message)
                    }
                }
            }
        }
    }

    override suspend fun sendMessage(message: String): AgentResponse {
        // Используем значение по умолчанию для обратной совместимости
        return sendMessage(message, 0.7)
    }

    override suspend fun sendMessage(message: String, temperature: Double): AgentResponse {
        Log.d(TAG, "🚀 Начинаем запрос для: \"${message.take(50)}\"")
        Log.d(TAG, "🌡️ Температура: $temperature")
        Log.d(TAG, "📚 Текущий размер истории: ${messageHistory.size} сообщений")

        return withContext(Dispatchers.IO) {
            val client = createClient()
            try {
                // 1. ПОЛУЧАЕМ ТОКЕН
                Log.d(TAG, "1. 📡 Получаю Access Token...")
                val accessToken = getAccessToken(client)

                // 2. ДОБАВЛЯЕМ СИСТЕМНОЕ СООБЩЕНИЕ, ЕСЛИ ИСТОРИЯ ПУСТА
                if (messageHistory.isEmpty()) {
                    val systemMessage = GigaMessage(
                        role = "system",
//                        content = "Ты полезный ассистент. Отвечай кратко и по делу на русском языке."
                        content = "Ты креативный помощник. Отвечай разнообразно и творчески."
                    )
                    messageHistory.add(systemMessage)
                    Log.d(TAG, "   ✨ Добавлено системное сообщение")
                }

                // 3. ДОБАВЛЯЕМ СООБЩЕНИЕ ПОЛЬЗОВАТЕЛЯ В ИСТОРИЮ
                val userMessage = GigaMessage(role = "user", content = message)
                messageHistory.add(userMessage)
                Log.d(TAG, "   📝 Добавлено сообщение пользователя")

                // 4. ФОРМИРУЕМ ЗАПРОС С ПОЛНОЙ ИСТОРИЕЙ
                Log.d(TAG, "2. 🤖 Запрос к GigaChat API с ${messageHistory.size} сообщениями...")

                val chatRequest = ChatRequest(
                    model = "GigaChat",
                    messages = messageHistory.toList(), // Отправляем всю историю
                    temperature = temperature, // Используем переданную температуру
                    max_tokens = 1000,
                    stream = false
                )

                val chatResponse = client.post(
                    "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
                ) {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    header("Accept", "application/json")
                    setBody(chatRequest)
                }

                Log.d(TAG, "   📥 Статус чата: ${chatResponse.status}")

                if (!chatResponse.status.isSuccess()) {
                    val error = chatResponse.bodyAsText()
                    Log.e(TAG, "❌ Ошибка GigaChat: $error")

                    // Удаляем сообщение пользователя из истории в случае ошибки
                    messageHistory.removeLast()
                    throw Exception("Ошибка GigaChat: ${chatResponse.status}")
                }

                val responseJson = chatResponse.bodyAsText()
                Log.d(TAG, "   📄 Ответ получен, парсинг...")

                val chatResponseObj = jsonParser.decodeFromString<ChatResponse>(responseJson)
                val answer = chatResponseObj.choices.firstOrNull()?.message?.content
                    ?: throw Exception("Ответ не найден в JSON")

                // 5. ДОБАВЛЯЕМ ОТВЕТ АССИСТЕНТА В ИСТОРИЮ
                val assistantMessage = GigaMessage(role = "assistant", content = answer)
                messageHistory.add(assistantMessage)

                Log.d(TAG, "✅ Успех! Ответ получен (${answer.length} символов)")
                Log.d(TAG, "📚 История теперь содержит ${messageHistory.size} сообщений")

                AgentResponse(
                    text = formatAnswer(answer),
                    toolUsed = detectTool(message)
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                throw e
            } finally {
                client.close()
                Log.d(TAG, "🔚 HTTP клиент закрыт")
            }
        }
    }

    override fun clearChatHistory() {
        // Сохраняем системное сообщение, если оно есть
        val systemMessages = messageHistory.filter { it.role == "system" }

        messageHistory.clear()

        // Добавляем обратно системное сообщение, если оно было
        if (systemMessages.isNotEmpty()) {
            messageHistory.addAll(systemMessages)
            Log.d(TAG, "🧹 История диалога очищена. Системный промпт сохранен.")
        } else {
            Log.d(TAG, "🧹 История диалога очищена.")
        }
    }

    /**
     * Отправка сообщения с кастомным системным промптом и температурой
     */
    suspend fun sendMessageWithCustomPrompt(
        message: String,
        systemPrompt: String,
        temperature: Double = 0.7
    ): AgentResponse {
        setSystemPrompt(systemPrompt)
        return sendMessage(message, temperature)
    }

    /**
     * Установка или изменение системного промпта
     */
    fun setSystemPrompt(prompt: String) {
        // Удаляем существующее системное сообщение
        removeSystemMessages()

        // Добавляем новое системное сообщение в начало
        messageHistory.add(0, GigaMessage(role = "system", content = prompt))

        Log.d(TAG, "🔄 Системный промпт обновлен")
    }

    /**
     * Получение полной истории сообщений
     */
    fun getFullHistory(): List<GigaMessage> {
        return messageHistory.toList()
    }

    /**
     * Получение истории диалога (без системных сообщений)
     */
    fun getConversationHistory(): List<GigaMessage> {
        return messageHistory.filter { it.role != "system" }
    }

    /**
     * Получение количества сообщений в истории
     */
    fun getHistorySize(): Int {
        return messageHistory.size
    }

    /**
     * Очистка истории сообщений (полная, включая системный промпт)
     */
    fun clearFullHistory() {
        messageHistory.clear()
        Log.d(TAG, "🧹 Полная история очищена")
    }

    /**
     * Удаление системных сообщений
     */
    private fun removeSystemMessages() {
        val iterator = messageHistory.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().role == "system") {
                iterator.remove()
            }
        }
    }

    /**
     * Удаление последнего сообщения из истории
     */
    fun removeLastMessage() {
        if (messageHistory.isNotEmpty()) {
            val removed = messageHistory.removeAt(messageHistory.size - 1)
            Log.d(TAG, "🗑️ Удалено последнее сообщение: ${removed.role}")
        }
    }

    /**
     * Удаление последнего обмена (пара сообщений)
     */
    fun removeLastExchange() {
        if (messageHistory.size >= 2) {
            val lastTwo = messageHistory.takeLast(2)
            messageHistory.removeAll(lastTwo)
            Log.d(TAG, "🗑️ Удален последний обмен сообщениями")
        }
    }

    private suspend fun getAccessToken(client: HttpClient): String {
        val currentTime = System.currentTimeMillis()

        // Проверяем кеш
        if (cachedToken != null && currentTime < tokenExpiryTime) {
            Log.d(TAG, "   ♻️ Использую кешированный токен")
            return cachedToken!!
        }

        Log.d(TAG, "   🔄 Токен истек или отсутствует, запрашиваю новый...")

        val rqUid = UUID.randomUUID().toString()

        val tokenResponse = client.submitForm(
            url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
            formParameters = Parameters.build {
                append("scope", "GIGACHAT_API_PERS")
            }
        ) {
            header("Authorization", "Basic $authKey")
            header("RqUID", rqUid)
            header("Accept", "application/json")
        }

        Log.d(TAG, "   📥 Статус токена: ${tokenResponse.status}")

        if (!tokenResponse.status.isSuccess()) {
            val error = tokenResponse.bodyAsText()
            Log.e(TAG, "❌ Ошибка токена: $error")
            throw Exception("Ошибка получения токена: ${tokenResponse.status}")
        }

        val tokenJson = tokenResponse.bodyAsText()
        Log.d(TAG, "   📄 Ответ токена: ${tokenJson.take(200)}...")

        val tokenResponseObj = jsonParser.decodeFromString<TokenResponse>(tokenJson)
        val accessToken = tokenResponseObj.access_token
            ?: throw Exception("Токен не найден в ответе")

        // Кешируем токен
        cachedToken = accessToken
        tokenExpiryTime = currentTime + TOKEN_TTL

        Log.d(TAG, "   ✅ Новый токен получен и закеширован")
        return accessToken
    }

    private fun formatAnswer(answer: String): String {
        return answer.trim()
            .replace("\\n\\n\\n+".toRegex(), "\n\n") // Убираем лишние пустые строки
            .replace("\\s+".toRegex(), " ")          // Убираем лишние пробелы
    }

    private fun detectTool(message: String): String? {
        val normalized = message.lowercase()

        return when {
            normalized.contains("погод") -> "get_weather"
            normalized.contains("считай") ||
                    normalized.contains("посчитай") ||
                    normalized.contains("сколько будет") -> "calculate"
            normalized.contains("курс") ||
                    normalized.contains("доллар") ||
                    normalized.contains("евро") -> "get_exchange_rate"
            normalized.contains("перевод") -> "translate"
            else -> null
        }
    }

    // Метод для очистки кеша токена
    fun clearTokenCache() {
        cachedToken = null
        tokenExpiryTime = 0
        Log.d(TAG, "🧹 Кеш токена очищен")
    }

    // Полная очистка всего
    fun clearAll() {
        clearTokenCache()
        clearFullHistory()
        Log.d(TAG, "🧹 Полная очистка всех данных")
    }
}