
package com.example.aiagent.data.database

import android.content.Context
import android.content.SharedPreferences
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.domain.RepositoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MessageLocalRepository private constructor(
    private val messageDao: MessageDao,
    private val context: Context
) {

    // Используем SharedPreferences для сохранения sessionId между запусками
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_agent_prefs", Context.MODE_PRIVATE)

    // Получаем или создаем постоянный sessionId
    private val currentSessionId: String by lazy {
        var sessionId = prefs.getString("session_id", null)
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            prefs.edit().putString("session_id", sessionId).apply()
        }
        sessionId
    }

    suspend fun saveMessage(
        role: String,
        content: String,
        repositoryType: RepositoryType,
        modelName: String? = null
    ) {
        val entity = MessageEntity(
            sessionId = currentSessionId,
            role = role,
            content = content,
            repositoryType = repositoryType.name,
            modelName = modelName
        )
        messageDao.insertMessage(entity)
    }

    suspend fun saveMessages(messages: List<ChatMessage>, repositoryType: RepositoryType, modelName: String? = null) {
        val entities = messages.map { chatMessage ->
            MessageEntity(
                sessionId = currentSessionId,
                role = chatMessage.role,
                content = chatMessage.content,
                repositoryType = repositoryType.name,
                modelName = modelName
            )
        }
        messageDao.insertAllMessages(entities)
    }

    suspend fun getMessageHistory(): List<ChatMessage> {
        return messageDao.getMessagesForSession(currentSessionId)
            .map { entity -> ChatMessage(entity.role, entity.content) }
    }

    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> {
        return messageDao.getMessagesFlow(currentSessionId)
            .map { entities -> entities.map { ChatMessage(it.role, it.content) } }
    }

    suspend fun clearHistory(keepSystemPrompt: Boolean) {
        if (keepSystemPrompt) {
            messageDao.deleteNonSystemMessages(currentSessionId)
        } else {
            messageDao.deleteAllMessages(currentSessionId)
        }
    }

    suspend fun setSystemPrompt(prompt: String) {
        // Проверяем, есть ли уже системное сообщение
        val existingSystem = messageDao.getSystemMessage(currentSessionId)

        if (existingSystem == null) {
            // Если нет - создаем новое
            messageDao.insertMessage(
                MessageEntity(
                    sessionId = currentSessionId,
                    role = "system",
                    content = prompt,
                    repositoryType = "system"
                )
            )
        } else {
            // Если есть - обновляем
            messageDao.updateSystemMessage(currentSessionId, prompt)
        }
    }

    suspend fun getSystemPrompt(): String? {
        return messageDao.getSystemMessage(currentSessionId)?.content
    }

    // Метод для сброса сессии (если нужно начать новую)
    suspend fun resetSession() {
        // Очищаем все сообщения текущей сессии
        messageDao.deleteAllMessages(currentSessionId)

        // Генерируем новый sessionId
        val newSessionId = UUID.randomUUID().toString()
        prefs.edit().putString("session_id", newSessionId).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: MessageLocalRepository? = null

        fun getInstance(context: Context): MessageLocalRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getInstance(context)
                MessageLocalRepository(database.messageDao(), context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}