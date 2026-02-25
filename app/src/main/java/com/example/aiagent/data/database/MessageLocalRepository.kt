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

    private val prefs: SharedPreferences = context.getSharedPreferences("ai_agent_prefs", Context.MODE_PRIVATE)

    private val currentSessionId: String by lazy {
        var sessionId = prefs.getString("session_id", null)
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            prefs.edit().putString("session_id", sessionId).apply()
        }
        sessionId
    }

    suspend fun saveMessage(
        id: String,
        role: String,
        content: String,
        repositoryType: RepositoryType,
        modelName: String? = null,
        realTokenCount: Int? = null
    ) {
        val entity = MessageEntity(
            id = id,
            sessionId = currentSessionId,
            role = role,
            content = content,
            repositoryType = repositoryType.name,
            modelName = modelName,
            realTokenCount = realTokenCount
        )
        messageDao.insertMessage(entity)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId) // Исправлено название метода
    }

    suspend fun getMessageHistory(): List<ChatMessage> {
        return messageDao.getMessagesForSession(currentSessionId)
            .map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    realTokenCount = entity.realTokenCount
                )
            }
    }

    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> {
        return messageDao.getMessagesFlow(currentSessionId)
            .map { entities ->
                entities.map {
                    ChatMessage(
                        id = it.id,
                        role = it.role,
                        content = it.content,
                        realTokenCount = it.realTokenCount
                    )
                }
            }
    }

    suspend fun clearHistory(keepSystemPrompt: Boolean) {
        if (keepSystemPrompt) {
            messageDao.deleteNonSystemMessages(currentSessionId)
        } else {
            messageDao.deleteAllMessages(currentSessionId)
        }
    }

    suspend fun setSystemPrompt(prompt: String) {
        val existingSystem = messageDao.getSystemMessage(currentSessionId)

        if (existingSystem == null) {
            // Если нет - создаем новое с ID
            messageDao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(), // Добавил ID
                    sessionId = currentSessionId,
                    role = "system",
                    content = prompt,
                    repositoryType = "system",
                    realTokenCount = null
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

    suspend fun resetSession() {
        messageDao.deleteAllMessages(currentSessionId)
        val newSessionId = UUID.randomUUID().toString()
        prefs.edit().putString("session_id", newSessionId).apply()
    }

    suspend fun removeLastMessage() {
        val messages = messageDao.getMessagesForSession(currentSessionId)
        if (messages.isNotEmpty()) {
            val lastMessage = messages.last()
            messageDao.deleteMessageById(lastMessage.id) // Исправлено
        }
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