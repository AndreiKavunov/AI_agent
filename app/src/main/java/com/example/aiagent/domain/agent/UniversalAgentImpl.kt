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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "UniversalAgent"

/**
 * Универсальный агент - использует базу данных для хранения контекста диалога
 */
class UniversalAgentImpl(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl,
    private val localRepository: MessageLocalRepository
) : UniversalAgent {

    // Текущий тип репозитория
    private var currentType = RepositoryType.GIGACHAT

    // Текущая модель HuggingFace (если выбрана)
    private var currentHuggingFaceModel: HuggingFaceModel? = null

    init {
        // Инициализируем системный промпт в корутине
        CoroutineScope(Dispatchers.IO).launch {
            initializeSystemPrompt()
        }
    }

    private suspend fun initializeSystemPrompt() {
        try {
            val existingPrompt = localRepository.getSystemPrompt()
            if (existingPrompt == null) {
                // Если системного промпта нет, устанавливаем по умолчанию
                setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
                Log.d(TAG, "✅ Системный промпт по умолчанию установлен")
            } else {
                Log.d(TAG, "✅ Системный промпт восстановлен из БД: \"${existingPrompt.take(50)}...\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при инициализации системного промпта: ${e.message}")
        }
    }

    override suspend fun processMessage(message: String, temperature: Double): AgentResponse {
        Log.d(TAG, "🚀 UniversalAgent обрабатывает сообщение через ${currentType}")

        // Получаем текущую историю для логирования
        val currentHistory = localRepository.getMessageHistory()
        Log.d(TAG, "📚 История сообщений до запроса: ${currentHistory.size}")

        // Сохраняем сообщение пользователя
        localRepository.saveMessage(
            role = "user",
            content = message,
            repositoryType = currentType,
            modelName = currentHuggingFaceModel?.displayName
        )

        return withContext(Dispatchers.IO) {
            try {
                // Получаем актуальную историю из БД
                val history = localRepository.getMessageHistory()

                // Конвертируем в формат для API
                val apiHistory = history.map {
                    com.example.aiagent.data.giga.GigaMessage(
                        role = it.role,
                        content = it.content
                    )
                }

                Log.d(TAG, "📤 Отправляем историю из ${history.size} сообщений")

                // Делегируем запрос соответствующему репозиторию
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

                // Сохраняем ответ ассистента
                localRepository.saveMessage(
                    role = "assistant",
                    content = response.text,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName
                )

                val newHistorySize = localRepository.getMessageHistory().size
                Log.d(TAG, "✅ Агент получил ответ. История после запроса: $newHistorySize")

                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка агента: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun clearHistory() {
        localRepository.clearHistory(keepSystemPrompt = true)
        Log.d(TAG, "🧹 История очищена. Системный промпт сохранен")
    }

    override suspend fun clearAll() {
        localRepository.clearHistory(keepSystemPrompt = false)
        // Устанавливаем системный промпт по умолчанию
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
        Log.d(TAG, "🧹 Полная очистка контекста")
    }

    override suspend fun setSystemPrompt(prompt: String) {
        localRepository.setSystemPrompt(prompt)
        Log.d(TAG, "🔄 Системный промпт установлен: \"${prompt.take(50)}...\"")
    }

    override suspend fun getSystemPrompt(): String? {
        return localRepository.getSystemPrompt()
    }

    override fun getCurrentAgentInfo(): String {
        return when (currentType) {
            RepositoryType.GIGACHAT -> "GigaChat"
            RepositoryType.HUGGINGFACE -> {
                currentHuggingFaceModel?.displayName ?: "HuggingFace"
            }
        }
    }

    override suspend fun switchRepository(type: RepositoryType) {
        if (currentType != type) {
            currentType = type
            // ПРИ СМЕНЕ РЕПОЗИТОРИЯ ОЧИЩАЕМ КОНТЕКСТ!
            clearAll()
            Log.d(TAG, "🔄 Переключено на ${type}, контекст очищен")
        }
    }

    override fun getCurrentRepositoryType(): RepositoryType = currentType

    override suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentHuggingFaceModel = modelType
        // При смене модели внутри HuggingFace тоже очищаем контекст
        if (currentType == RepositoryType.HUGGINGFACE) {
            clearAll()
        }
        Log.d(TAG, "🔄 Модель HuggingFace изменена на: ${modelType.displayName}")
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel? {
        return currentHuggingFaceModel
    }

    /**
     * Получить текущую историю сообщений как Flow (для UI)
     */
    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> {
        return localRepository.getMessageHistoryFlow()
    }

    /**
     * Получить текущую историю сообщений (для отладки)
     */
    suspend fun getMessageHistory(): List<ChatMessage> {
        return localRepository.getMessageHistory()
    }
}