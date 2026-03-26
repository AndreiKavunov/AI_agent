// data/giga/GigaChatRepository.kt
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.X509TrustManager

private const val TAG = "GigaChat"

/**
 * Репозиторий для работы с GigaChat API
 * Отвечает только за отправку запросов и получение ответов
 * Подсчёт токенов и детект инструментов вынесены в агент
 */
class GigaChatRepository : ChatRepository {

    private val authKey = BuildConfig.GIGACHAT_AUTH_KEY
    private val TOKEN_TTL = 25 * 60 * 1000L // 25 минут в миллисекундах

    // Кеш токена в памяти
    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0

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
                        Log.d("Ktor-GigaChat", message)
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
        Log.d(TAG, "🚀 GigaChat получает историю из ${history.size} сообщений")
        Log.d(TAG, "🌡️ Температура: $temperature")

        return withContext(Dispatchers.IO) {
            val client = createClient()
            try {
                // 1. ПОЛУЧАЕМ ТОКЕН
                Log.d(TAG, "1. 📡 Получаю Access Token...")
                val accessToken = getAccessToken(client)

                // 2. ФОРМИРУЕМ ЗАПРОС С ИСТОРИЕЙ ОТ АГЕНТА
                Log.d(TAG, "2. 🤖 Запрос к GigaChat API...")

                val chatRequest = ChatRequest(
                    model = "GigaChat",
                    messages = history, // Используем историю от агента!
                    temperature = temperature,
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
                    throw Exception("Ошибка GigaChat: ${chatResponse.status} - $error")
                }

                val responseJson = chatResponse.bodyAsText()
                Log.d(TAG, "   📄 Ответ получен, парсинг...")

                val chatResponseObj = jsonParser.decodeFromString<ChatResponse>(responseJson)
                val answer = chatResponseObj.choices.firstOrNull()?.message?.content
                    ?: throw Exception("Ответ не найден в JSON")

                // Получаем количество токенов из ответа API (если есть)
                val tokenCount = chatResponseObj.usage?.total_tokens ?: 0
                val formattedAnswer = formatAnswer(answer)

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                Log.d(TAG, "✅ Успех! Ответ получен (${answer.length} символов)")
                Log.d(TAG, "⏱️ Время ответа: ${responseTime}ms")
                Log.d(TAG, "📊 Токенов (из API): $tokenCount")

                // Возвращаем только базовый ответ, без лишних вычислений
                AgentResponse(
                    text = formattedAnswer,
                    toolUsed = null, // Детект инструментов теперь в агенте
                    responseTimeMs = responseTime,
                    tokenCount = tokenCount
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

    // Deprecated методы для обратной совместимости
    @Deprecated(
        "Use sendMessageWithHistory instead",
        ReplaceWith("sendMessageWithHistory(listOf(GigaMessage(role = \"user\", content = message)), temperature)")
    )
    override suspend fun sendMessage(message: String): AgentResponse {
        return sendMessage(message, 0.7)
    }

    @Deprecated(
        "Use sendMessageWithHistory instead",
        ReplaceWith("sendMessageWithHistory(listOf(GigaMessage(role = \"user\", content = message)), temperature)")
    )
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
}
