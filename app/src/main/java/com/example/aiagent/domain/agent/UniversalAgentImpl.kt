package com.example.aiagent.domain.agent

import android.util.Log
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "UniversalAgent"

/**
 * Универсальный агент - единственное место, где хранится контекст диалога
 */
class UniversalAgentImpl(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl
) : UniversalAgent {

    // Текущий тип репозитория
    private var currentType = RepositoryType.GIGACHAT

    // КОНТЕКСТ ДИАЛОГА - хранится здесь!
    private val messageHistory = mutableListOf<ChatMessage>()

    // Текущая модель HuggingFace (если выбрана)
    private var currentHuggingFaceModel: HuggingFaceModel? = null

    init {
        // Добавляем системный промпт по умолчанию при создании
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
    }

    override suspend fun processMessage(message: String, temperature: Double): AgentResponse {
        Log.d(TAG, "🚀 UniversalAgent обрабатывает сообщение через ${currentType}")
        Log.d(TAG, "📚 История сообщений до запроса: ${messageHistory.size}")

        // Добавляем сообщение пользователя в историю
        messageHistory.add(ChatMessage(role = "user", content = message))

        return withContext(Dispatchers.IO) {
            try {
                // Получаем историю в формате для API
                val apiHistory = messageHistory.map {
                    com.example.aiagent.data.giga.GigaMessage(
                        role = it.role,
                        content = it.content
                    )
                }

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

                // Добавляем ответ ассистента в историю
                messageHistory.add(ChatMessage(role = "assistant", content = response.text))

                Log.d(TAG, "✅ Агент получил ответ. История после запроса: ${messageHistory.size}")

                // Возвращаем ответ
                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка агента: ${e.message}")
                // В случае ошибки удаляем последнее сообщение пользователя
                if (messageHistory.isNotEmpty() && messageHistory.last().role == "user") {
                    messageHistory.removeAt(messageHistory.size - 1)
                }
                throw e
            }
        }
    }

    override fun clearHistory() {
        // Очищаем историю, но сохраняем системный промпт
        val systemMessages = messageHistory.filter { it.role == "system" }

        messageHistory.clear()
        messageHistory.addAll(systemMessages)

        Log.d(TAG, "🧹 История очищена. Системных сообщений: ${systemMessages.size}")
    }

    override fun clearAll() {
        messageHistory.clear()
        // Восстанавливаем системный промпт по умолчанию
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
        Log.d(TAG, "🧹 Полная очистка контекста")
    }

    override fun setSystemPrompt(prompt: String) {
        // Удаляем все системные сообщения
        messageHistory.removeAll { it.role == "system" }

        // Добавляем новое системное сообщение в начало
        messageHistory.add(0, ChatMessage(role = "system", content = prompt))

        Log.d(TAG, "🔄 Системный промпт установлен: \"${prompt.take(50)}...\"")
    }

    override fun getSystemPrompt(): String? {
        return messageHistory.firstOrNull { it.role == "system" }?.content
    }

    override fun getCurrentAgentInfo(): String {
        return when (currentType) {
            RepositoryType.GIGACHAT -> "GigaChat"
            RepositoryType.HUGGINGFACE -> {
                currentHuggingFaceModel?.displayName ?: "HuggingFace"
            }
        }
    }

    override fun switchRepository(type: RepositoryType) {
        if (currentType != type) {
            currentType = type
            // ПРИ СМЕНЕ РЕПОЗИТОРИЯ ОЧИЩАЕМ КОНТЕКСТ!
            clearAll()
            Log.d(TAG, "🔄 Переключено на ${type}, контекст очищен")
        }
    }

    override fun getCurrentRepositoryType(): RepositoryType = currentType

    override fun setHuggingFaceModel(modelType: HuggingFaceModel) {
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
     * Получить текущую историю сообщений (для отладки/UI)
     */
    fun getMessageHistory(): List<ChatMessage> = messageHistory.toList()

    /**
     * Удалить последнее сообщение
     */
    private fun removeLastMessage() {
        if (messageHistory.isNotEmpty()) {
            messageHistory.removeAt(messageHistory.size - 1)
        }
    }
}