// data/database/memory/LongTermMemoryEntity.kt
package com.example.aiagent.data.database.memory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Долговременная память - профиль, решения, знания
 * Хранит постоянную информацию о пользователе и его предпочтениях
 */
@Entity(tableName = "long_term_memory")
data class LongTermMemoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "memoryKey")
    val key: String,
    val value: String,
    val category: MemoryCategory,
    val confidence: Float = 1.0f,
    val accessCount: Int = 0,          // Сколько раз использовалась эта информация
    val lastAccessed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Категории долговременной памяти
 */
enum class MemoryCategory {
    PROFILE,           // Информация о профиле пользователя (имя, возраст, интересы)
    PREFERENCES,       // Предпочтения (стиль общения, формат ответов)
    KNOWLEDGE,        // Знания (факты, которые пользователь упомянул)
    DECISIONS,        // Решения (выборы, которые пользователь сделал)
    LEARNING_GOALS,    // Цели обучения (языки, навыки)
    ACHIEVEMENTS,     // Достижения (успехи в обучении)
    CUSTOM             // Пользовательские категории
}
