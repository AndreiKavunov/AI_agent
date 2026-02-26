package com.example.aiagent.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val summary: String,
    val messageCount: Int,  // Сколько сообщений в этой суммаризации
    val startMessageId: String,  // ID первого сообщения
    val endMessageId: String,    // ID последнего сообщения
    val tokenCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)