package com.example.aiagent.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Insert
    suspend fun insertAllMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesFlow(sessionId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId AND role != 'system'")
    suspend fun deleteNonSystemMessages(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteAllMessages(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId AND role = 'system'")
    suspend fun deleteSystemMessages(sessionId: String)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND role = 'system' LIMIT 1")
    suspend fun getSystemMessage(sessionId: String): MessageEntity?

    @Transaction
    suspend fun updateSystemMessage(sessionId: String, newContent: String) {
        // Удаляем старые системные сообщения
        deleteSystemMessages(sessionId)
        // Вставляем новое сгенерируем id внутри метода
        val newMessage = MessageEntity(
            id = java.util.UUID.randomUUID().toString(), // Генерируем ID здесь
            sessionId = sessionId,
            role = "system",
            content = newContent,
            repositoryType = "system",
            realTokenCount = null
        )
        insertMessage(newMessage)
    }

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String) // Изменил на String
}