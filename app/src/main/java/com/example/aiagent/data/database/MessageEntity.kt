package com.example.aiagent.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val repositoryType: String,
    val modelName: String? = null,
    val realTokenCount: Int? = null // Добавляем реальные токены
)