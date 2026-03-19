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
                
                val request = BuildRagIndexRequest(strategy = strategy.name.lowercase())
                val response: BuildRagIndexResponse = client.post("$serverUrl/call_tool") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()

                Log.d(TAG, "✅ Индекс построен: ${response.status}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при построении индекса: ${e.message}", e)
                Result.failure(e)
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
                question = question,
                strategy = strategy.name.lowercase()
            )
            
            val startTime = System.currentTimeMillis()
            val response: AskDocumentsResponse = client.post("$serverUrl/call_tool") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            val queryTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Получен ответ за ${queryTime}ms, использовано ${response.chunksUsed} фрагментов")
            
            val ragContext = RagContext(
                strategy = strategy,
                documents = response.sources,
                chunksUsed = response.chunksUsed,
                formattedContext = response.answer
            )
            
            Result.success(ragContext)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при вопросе к документам: ${e.message}", e)
            Result.failure(e)
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
                val response: CompareStrategiesResponse = client.post("$serverUrl/call_tool") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                val comparisonTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Сравнение завершено за ${comparisonTime}ms")
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
