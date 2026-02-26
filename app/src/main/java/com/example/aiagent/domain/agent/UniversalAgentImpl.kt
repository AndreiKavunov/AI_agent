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

        // Генерируем ID для сообщения пользователя
        val userMessageId = UUID.randomUUID().toString()

        // Сохраняем сообщение пользователя
        localRepository.saveMessage(
            id = userMessageId,
            role = "user",
            content = message,
            repositoryType = currentType,
            modelName = currentHuggingFaceModel?.displayName
        )

        Log.d(TAG, "💬 Сообщение пользователя сохранено: $userMessageId")

        return withContext(Dispatchers.IO) {
            try {
                // Получаем контекст через SummaryManager
                val context = summaryManager.getHistoryForApi(currentType)

                // Строим историю для API
                val apiHistory = buildApiHistory(context)

                // Считаем токены промпта
                val promptTokens = calculatePromptTokens(context)

                Log.d(TAG, "📤 Отправка запроса в API...")

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

                Log.d(TAG, "📥 Получен ответ от API: ${response.text.take(100)}...")

                // Сохраняем ответ ассистента
                val assistantMessageId = UUID.randomUUID().toString()
                realTokenCounts[assistantMessageId] = response.tokenCount

                localRepository.saveMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = response.text,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName,
                    realTokenCount = response.tokenCount
                )

                // Запускаем проверку необходимости суммаризации (в фоне)
                summaryManager.checkAndSummarizeIfNeeded(currentType)

                // Подсчитываем итоговые токены
                val totalTokens = calculateTotalTokens()

                Log.d(TAG, "📊 Статистика:")
                Log.d(TAG, "   ├─ Токенов в промпте: $promptTokens")
                Log.d(TAG, "   ├─ Токенов в ответе: ${response.tokenCount}")
                Log.d(TAG, "   └─ Всего токенов: $totalTokens")

                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount,
                    promptTokens = promptTokens,
                    totalHistoryTokens = totalTokens
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка: ${e.message}")
                e.printStackTrace()
                // В случае ошибки удаляем сообщение пользователя
                localRepository.deleteMessage(userMessageId)
                throw e
            }
        }
    }

    private fun buildApiHistory(context: SummaryManager.HistoryContext): List<GigaMessage> {
        val history = mutableListOf<GigaMessage>()

        // Добавляем суммаризации как системные сообщения
        context.summaries.forEachIndexed { index, summary ->
            history.add(
                GigaMessage(
                    role = "system",
                    content = "[Суммаризация части диалога ${index + 1}]: ${summary.summary}"
                )
            )
        }

        // Добавляем свежие сообщения
        history.addAll(
            context.freshMessages.map {
                GigaMessage(role = it.role, content = it.content)
            }
        )

        return history
    }

    private suspend fun calculatePromptTokens(context: SummaryManager.HistoryContext): Int {
        val messagesTokens = context.freshMessages.sumOf { msg ->
            msg.realTokenCount ?: tokenCounter.estimateTokens(
                msg.content,
                isSystemPrompt = msg.role == "system"
            )
        }

        val summariesTokens = context.summaries.sumOf { it.tokenCount }

        return messagesTokens + summariesTokens
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