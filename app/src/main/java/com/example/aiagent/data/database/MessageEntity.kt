package com.example.aiagent.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String, // для поддержки разных сессий, если понадобится
    val role: String, // "system", "user", "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val repositoryType: String, // тип репозитория для контекста
    val modelName: String? = null // для HF моделей
)