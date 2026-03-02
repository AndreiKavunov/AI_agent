// data/database/memory/WorkingMemoryEntity.kt
package com.example.aiagent.data.database.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Рабочая память - данные текущей задачи
 * Хранит информацию о текущем прогрессе в изучении языка
 */
@Entity(tableName = "working_memory")
data class WorkingMemoryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    
    // Данные о языке
    val languageToLearn: String? = null,      // Какой язык изучаем (например: "English", "Spanish")
    val learningGoal: String? = null,           // Цель изучения (conversation, translation, grammar, etc.)
    val currentLevel: String? = null,            // Текущий уровень (A1, A2, B1, B2, C1, C2)
    
    // Прогресс обучения
    val lessonsCompleted: Int = 0,              // Количество пройденных уроков
    val exercisesCompleted: Int = 0,             // Количество выполненных упражнений
    val correctAnswers: Int = 0,                 // Количество правильных ответов
    val totalAnswers: Int = 0,                   // Общее количество ответов
    
    // Успехи и достижения
    val achievements: String = "",                 // JSON список достижений
    val streakDays: Int = 0,                    // Дней подряд без перерывов
    val lastPracticeDate: Long? = null,           // Дата последней практики
    
    // Текущая тема урока
    val currentTopic: String? = null,            // Текущая тема (например: "Past Tense")
    val currentLessonId: String? = null,          // ID текущего урока
    
    val updatedAt: Long = System.currentTimeMillis()
)
