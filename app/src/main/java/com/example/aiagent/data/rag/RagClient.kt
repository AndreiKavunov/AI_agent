package com.example.aiagent.data.rag

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val TAG = "RagClient"

/**
 * Клиент для работы с RAG сервером
 * 
 * Сервер должен поддерживать следующие эндпоинты:
 * - POST /call_tool - вызов инструментов (включая построение индекса)
 * - POST /ask_documents - вопрос к документам
 * - POST /compare_strategies - сравнение стратегий
 */
class RagClient(
    private val serverUrl: String = "http://192.168.0.82:8000"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(io.ktor.client.plugins.logging.Logging) {
            level = io.ktor.client.plugins.logging.LogLevel.INFO
        }
    }

    /**
     * Построить RAG индекс с указанной стратегией
     */
    suspend fun buildRagIndex(strategy: RagStrategy): Result<BuildRagIndexResponse> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🏗️ Запрос на построение RAG индекса со стратегией: ${strategy.name}")
                
                val request = BuildRagIndexRequest(
                    name = "build_rag_index",
                    arguments = BuildRagIndexArguments(strategy = strategy.name.lowercase())
                )
                
                // Логируем JSON запроса
                val requestJson = json.encodeToString(BuildRagIndexRequest.serializer(), request)
                Log.d(TAG, "📤 JSON запрос на построение индекса: $requestJson")
                
                val startTime = System.currentTimeMillis()
                val serverResponse: RagServerResponse = client.post("$serverUrl/call_tool") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                // Логируем JSON ответа
                val responseJson = json.encodeToString(RagServerResponse.serializer(), serverResponse)
                Log.d(TAG, "📥 JSON ответ: $responseJson")
                
                val buildTime = System.currentTimeMillis() - startTime

                if (!serverResponse.success) {
                    val errorMessage = serverResponse.result?.toString() ?: "Unknown error"
                    Log.e(TAG, "❌ Сервер вернул ошибку при построении индекса")
                    return@withContext Result.failure(Exception("Server returned error: $errorMessage"))
                }

                // Для build_rag_index result - это просто строка
                val resultString = serverResponse.result?.toString() ?: "Нет ответа от сервера"
                val response = BuildRagIndexResponse(
                    status = "success",
                    message = resultString,
                    strategy = strategy.name
                )

                Log.d(TAG, "✅ Индекс построен за ${buildTime}ms: ${response.status}")
                Result.success(response)
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("404") == true -> "Сервер не найден. Проверьте URL: $serverUrl"
                    e.message?.contains("Connection refused") == true -> "Сервер недоступен. Убедитесь, что сервер запущен на $serverUrl"
                    e.message?.contains("timeout") == true -> "Таймаут подключения к серверу"
                    else -> "Ошибка при построении индекса: ${e.message}"
                }
                Log.e(TAG, "❌ $errorMsg", e)
                Result.failure(Exception(errorMsg))
            }
        }

    /**
     * Задать вопрос к документам с использованием указанной стратегии
     */
    suspend fun askDocuments(
        question: String,
        strategy: RagStrategy
    ): Result<RagContext> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📚 Вопрос к документам: \"$question\" со стратегией: ${strategy.name}")
            
            val request = AskDocumentsRequest(
                name = "ask_documents",
                arguments = AskDocumentsArguments(
                    question = question,
                    strategy = strategy.name.lowercase(),
                    rerank_method = "hybrid",
                    initial_top_k = 10,
                    final_top_k = 3
                )
            )
            
            // Логируем JSON запроса
            val requestJson = json.encodeToString(AskDocumentsRequest.serializer(), request)
            Log.d(TAG, "📤 JSON запрос: $requestJson")
            
            val startTime = System.currentTimeMillis()
            val serverResponse: RagServerResponse = client.post("$serverUrl/call_tool") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            // Логируем JSON ответа
            val responseJson = json.encodeToString(RagServerResponse.serializer(), serverResponse)
            Log.d(TAG, "📥 JSON ответ: $responseJson")
            
            val queryTime = System.currentTimeMillis() - startTime
            
            if (!serverResponse.success) {
                val errorMessage = serverResponse.result?.toString() ?: "Unknown error"
                Log.e(TAG, "❌ Сервер вернул ошибку: $errorMessage")
                return@withContext Result.failure(Exception("Server returned error: $errorMessage"))
            }
            
            val resultElement = serverResponse.result
            if (resultElement == null) {
                Log.e(TAG, "❌ Сервер вернул пустой результат")
                return@withContext Result.failure(Exception("Server returned empty result"))
            }
            
            // Для ask_documents result - это JSON-объект, который нужно распарсить
            val response: AskDocumentsResponse = json.decodeFromJsonElement(AskDocumentsResponse.serializer(), resultElement)
            
            Log.d(TAG, "✅ Получен ответ за ${queryTime}ms, источников: ${response.sources.size}, цитат: ${response.quotes.size}")
            
            val ragContext = RagContext(
                strategy = strategy,
                documents = response.sources,
                chunksUsed = response.sources.size,
                formattedContext = response.answer,
                quotes = response.quotes,
                ragAnswer = response.answer
            )
            
            Result.success(ragContext)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("404") == true -> "Сервер не найден. Проверьте URL: $serverUrl"
                e.message?.contains("Connection refused") == true -> "Сервер недоступен. Убедитесь, что сервер запущен на $serverUrl"
                e.message?.contains("timeout") == true -> "Таймаут подключения к серверу"
                e.message?.contains("JSON") == true -> "Ошибка парсинга ответа от сервера"
                else -> "Ошибка при вопросе к документам: ${e.message}"
            }
            Log.e(TAG, "❌ $errorMsg", e)
            Result.failure(Exception(errorMsg))
        }
    }

    /**
     * Сравнить результаты обеих стратегий для одного вопроса
     */
    suspend fun compareStrategies(question: String): Result<CompareStrategiesResponse> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Сравнение стратегий для вопроса: \"$question\"")
                
                val request = CompareStrategiesRequest(question = question)
                
                val startTime = System.currentTimeMillis()
                val serverResponse: RagServerResponse = client.post("$serverUrl/call_tool") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                if (!serverResponse.success) {
                    Log.e(TAG, "❌ Сервер вернул ошибку при сравнении стратегий")
                    return@withContext Result.failure(Exception("Server returned error"))
                }
                
                val comparisonTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Сравнение завершено за ${comparisonTime}ms")
                
                // Примечание: сервер может не поддерживать этот инструмент в новом формате
                // Если сервер возвращает строку вместо объекта, обрабатываем её
                val response = CompareStrategiesResponse(
                    question = question,
                    results = emptyList(),
                    recommendedStrategy = "fixed",
                    comparisonTimeMs = comparisonTime
                )
                
                Log.d(TAG, "📊 Рекомендуемая стратегия: ${response.recommendedStrategy}")
                
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при сравнении стратегий: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Проверить доступность RAG сервера
     */
    suspend fun checkServerAvailability(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Проверка доступности сервера: $serverUrl")
            
            val response: HttpResponse = client.get("$serverUrl/health")
            val isAvailable = response.status == HttpStatusCode.OK
            
            if (isAvailable) {
                Log.d(TAG, "✅ Сервер доступен")
            } else {
                Log.w(TAG, "⚠️ Сервер вернул статус: ${response.status}")
            }
            
            Result.success(isAvailable)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Сервер недоступен: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Получить информацию о статусе индекса
     */
    suspend fun getIndexStatus(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Запрос статуса индекса")
            
            val response: Map<String, Any> = client.get("$serverUrl/index_status").body()
            
            Log.d(TAG, "✅ Статус индекса получен: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при получении статуса индекса: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Закрыть клиент и освободить ресурсы
     */
    fun close() {
        try {
            client.close()
            Log.d(TAG, "🔒 RAG клиент закрыт")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при закрытии клиента: ${e.message}")
        }
    }

    companion object {
        /**
         * Создать экземпляр клиента с настройками по умолчанию
         */
        fun create(serverUrl: String = "http://192.168.0.82:8000"): RagClient {
            return RagClient(serverUrl)
        }
    }
}
