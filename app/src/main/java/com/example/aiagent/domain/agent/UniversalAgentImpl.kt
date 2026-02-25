// domain/agent/UniversalAgentImpl.kt
package com.example.aiagent.domain.agent

import android.util.Log
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
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
    private val localRepository: MessageLocalRepository
) : UniversalAgent {

    private var currentType = RepositoryType.GIGACHAT
    private var currentHuggingFaceModel: HuggingFaceModel? = null
    private val tokenCounter = TokenCounter()

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
        val userMessage = ChatMessage(
            id = userMessageId,
            role = "user",
            content = message
        )

        // Сохраняем сообщение пользователя
        localRepository.saveMessage(
            id = userMessageId,
            role = "user",
            content = message,
            repositoryType = currentType,
            modelName = currentHuggingFaceModel?.displayName
        )

        return withContext(Dispatchers.IO) {
            try {
                // Получаем всю историю
                val history = localRepository.getMessageHistory()

                // Считаем токены в промпте (используем estimate для consistency)
                val promptTokens = history.sumOf { msg ->
                    tokenCounter.estimateTokens(
                        msg.content,
                        isSystemPrompt = msg.role == "system"
                    )
                }

                val apiHistory = history.map {
                    com.example.aiagent.data.giga.GigaMessage(
                        role = it.role,
                        content = it.content
                    )
                }

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

                // Сохраняем реальные токены из API для ответа
                val assistantMessageId = UUID.randomUUID().toString()
                realTokenCounts[assistantMessageId] = response.tokenCount

                // Сохраняем ответ ассистента с реальными токенами
                localRepository.saveMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = response.text,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName,
                    realTokenCount = response.tokenCount // Сохраняем в БД
                )

                // Получаем обновленную историю
                val updatedHistory = localRepository.getMessageHistory()

                // Собираем все реальные токены для подсчёта
                val allRealTokens = mutableMapOf<String, Int>()
                updatedHistory.forEach { msg ->
                    msg.realTokenCount?.let { allRealTokens[msg.id] = it }
                }

                // Подсчитываем токены с учётом реальных значений
                val tokenStats = tokenCounter.detailedCount(updatedHistory, allRealTokens)

                Log.d(TAG, "📊 ИТОГО:")
                Log.d(TAG, "   ├─ Системные: ${tokenStats.systemTokens}")
                Log.d(TAG, "   ├─ Пользователь: ${tokenStats.userTokens}")
                Log.d(TAG, "   ├─ Ассистент: ${tokenStats.assistantTokens}")
                Log.d(TAG, "   └─ Всего: ${tokenStats.totalTokens}")
                Log.d(TAG, "   └─ Последний ответ (реальные): ${response.tokenCount}")

                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount,
                    promptTokens = promptTokens,
                    totalHistoryTokens = tokenStats.totalTokens
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка: ${e.message}")
                // В случае ошибки удаляем сообщение пользователя
                localRepository.deleteMessage(userMessageId)
                throw e
            }
        }
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

    // Остальные методы...
    override suspend fun clearHistory() {
        localRepository.clearHistory(keepSystemPrompt = true)
        realTokenCounts.clear()
    }

    override suspend fun clearAll() {
        localRepository.clearHistory(keepSystemPrompt = false)
        realTokenCounts.clear()
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
    }

    override suspend fun setSystemPrompt(prompt: String) {
        localRepository.setSystemPrompt(prompt)
    }

    override suspend fun getSystemPrompt(): String? = localRepository.getSystemPrompt()
    override fun getCurrentAgentInfo(): String = currentType.name
    override fun getCurrentRepositoryType(): RepositoryType = currentType

    override suspend fun switchRepository(type: RepositoryType) {
        currentType = type
        clearAll()
    }

    override suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentHuggingFaceModel = modelType
        if (currentType == RepositoryType.HUGGINGFACE) {
            clearAll()
        }
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel? = currentHuggingFaceModel
    override fun getTokenCounter(): TokenCounter = tokenCounter

    suspend fun getMessageHistory(): List<ChatMessage> = localRepository.getMessageHistory()
    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> = localRepository.getMessageHistoryFlow()
}