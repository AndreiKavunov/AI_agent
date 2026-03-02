// data/database/memory/ShortTermMemoryEntity.kt
package com.example.aiagent.data.database.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Краткосрочная память - данные текущего диалога
 * Хранит временные данные, которые теряются при закрытии сессии
 */
@Entity(tableName = "short_term_memory")
data class ShortTermMemoryEntity(
    @PrimaryKey
    val sessionId: String,
    val lastUserMessage: String? = null,
    val lastAgentResponse: String? = null,
    val currentTopic: String? = null,
    val conversationContext: String = "",
    val lastActivityTime: Long = System.currentTimeMillis()
)
