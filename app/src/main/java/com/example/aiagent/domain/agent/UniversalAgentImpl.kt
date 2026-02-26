// domain/agent/UniversalAgentImpl.kt
package com.example.aiagent.domain.agent

import android.util.Log
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.database.SummaryDao
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.summary.SummaryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "UniversalAgent"

class UniversalAgentImpl(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl,
    private val localRepository: MessageLocalRepository,
    private val summaryDao: SummaryDao  // Добавляем SummaryDao
) : UniversalAgent {

    private var currentType = RepositoryType.GIGACHAT
    private var currentHuggingFaceModel: HuggingFaceModel? = null
    private val tokenCounter = TokenCounter()

    // Инициализируем SummaryManager
    private val summaryManager by lazy {
        SummaryManager(
            localRepository = localRepository,
            summaryDao = summaryDao,
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository
        )
    }

    // Храним реальные токены из API для каждого сообщения
    private val realTokenCounts = mutableMapOf<String, Int>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeSystemPrompt()
        }
    }

    private suspend fun initializeSystemPrompt() {
        try {
            val existingPrompt = localRepository.getSystemPrompt()
            if (existingPrompt == null) {
                setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при инициализации системного промпта: ${e.message}")
        }
    }

    override suspend fun processMessage(message: String, temperature: Double): AgentResponse {
        Log.d(TAG, "🚀 UniversalAgent обрабатывает сообщение через ${currentType}")

        val userMessageId = UUID.randomUUID().toString()

        localRepository.saveMessage(
            id = userMessageId,
            role = "user",
            content = message,
            repositoryType = currentType,
            modelName = currentHuggingFaceModel?.displayName
        )

        return withContext(Dispatchers.IO) {
            try {
                // Получаем сообщения для API через SummaryManager
                val messagesForApi = summaryManager.getMessagesForApi()

                // Конвертируем в GigaMessage
                val apiHistory = summaryManager.toGigaMessages(messagesForApi)

                // Логируем для проверки
                Log.d(TAG, "📤 Отправка в API (${apiHistory.size} сообщений):")
                apiHistory.forEachIndexed { index, msg ->
                    val preview = if (msg.role == "system") {
                        msg.content.take(150) + "..."
                    } else {
                        msg.content.take(50) + "..."
                    }
                    Log.d(TAG, "   [$index] ${msg.role}: $preview")
                }

                // Проверяем, что system сообщение только одно
                val systemCount = apiHistory.count { it.role == "system" }
                if (systemCount != 1) {
                    Log.e(TAG, "❌ КРИТИЧЕСКАЯ ОШИБКА: в истории ${systemCount} system сообщений!")
                }

                // Отправляем запрос
                val response = when (currentType) {
                    RepositoryType.GIGACHAT -> {
                        gigaChatRepository.sendMessageWithHistory(
                            history = apiHistory,
                            temperature = temperature
                        )
                    }
                    RepositoryType.HUGGINGFACE -> {
                        huggingFaceRepository.sendMessageWithHistory(
                            history = apiHistory,
                            temperature = temperature,
                        )
                    }
                }

                // Сохраняем ответ
                val assistantMessageId = UUID.randomUUID().toString()
                localRepository.saveMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = response.text,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName,
                    realTokenCount = response.tokenCount
                )

                // Запускаем проверку суммаризации
                summaryManager.checkAndSummarizeIfNeeded(currentType)

                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount,
                    promptTokens = response.tokenCount ?: 0,
                    totalHistoryTokens = response.tokenCount ?: 0
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка: ${e.message}")
                e.printStackTrace()
                localRepository.deleteMessage(userMessageId)
                throw e
            }
        }
    }

    private suspend fun calculateTotalTokens(): Int {
        val history = localRepository.getMessageHistory()
        val allRealTokens = mutableMapOf<String, Int>()
        history.forEach { msg ->
            msg.realTokenCount?.let { allRealTokens[msg.id] = it }
        }
        val tokenStats = tokenCounter.detailedCount(history, allRealTokens)
        return tokenStats.totalTokens
    }

    override suspend fun getTotalHistoryTokens(): DetailedTokenCount {
        val history = localRepository.getMessageHistory()
        val allRealTokens = mutableMapOf<String, Int>()
        history.forEach { msg ->
            msg.realTokenCount?.let { allRealTokens[msg.id] = it }
        }
        return tokenCounter.detailedCount(history, allRealTokens)
    }

    override suspend fun getLastResponseTokens(): Int? {
        val history = localRepository.getMessageHistory()
        val lastAssistant = history.lastOrNull { it.role == "assistant" }
        return lastAssistant?.realTokenCount ?: lastAssistant?.let {
            tokenCounter.estimateTokens(it.content)
        }
    }

    override suspend fun getCurrentQueryTokens(): Int {
        val history = localRepository.getMessageHistory()
        val lastUser = history.lastOrNull { it.role == "user" }
        return lastUser?.let {
            it.realTokenCount ?: tokenCounter.estimateTokens(it.content)
        } ?: 0
    }

    override suspend fun clearHistory() {
        localRepository.clearHistory(keepSystemPrompt = true)
        summaryDao.deleteAllSummaries(localRepository.provideSessionId()) // Исправлено
        realTokenCounts.clear()
        Log.d(TAG, "🧹 История очищена (системный промпт сохранен)")
    }

    override suspend fun clearAll() {
        localRepository.clearHistory(keepSystemPrompt = false)
        summaryDao.deleteAllSummaries(localRepository.provideSessionId()) // Исправлено
        realTokenCounts.clear()
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
        Log.d(TAG, "🧹 Полная очистка выполнена")
    }

    override suspend fun setSystemPrompt(prompt: String) {
        localRepository.setSystemPrompt(prompt)
        Log.d(TAG, "⚙️ Системный промпт обновлен: $prompt")
    }

    override suspend fun getSystemPrompt(): String? = localRepository.getSystemPrompt()

    override fun getCurrentAgentInfo(): String = currentType.name

    override fun getCurrentRepositoryType(): RepositoryType = currentType

    override suspend fun switchRepository(type: RepositoryType) {
        Log.d(TAG, "🔄 Смена репозитория: ${currentType} -> $type")
        currentType = type
        clearAll()
    }

    override suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentHuggingFaceModel = modelType
        if (currentType == RepositoryType.HUGGINGFACE) {
            clearAll()
        }
        Log.d(TAG, "🤗 Модель HuggingFace: ${modelType.displayName}")
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel? = currentHuggingFaceModel

    override fun getTokenCounter(): TokenCounter = tokenCounter

    suspend fun getMessageHistory(): List<ChatMessage> = localRepository.getMessageHistory()

    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> = localRepository.getMessageHistoryFlow()
}