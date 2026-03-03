// domain/agent/memory/LessonGoal.kt
package com.example.aiagent.domain.agent.memory

import kotlinx.serialization.Serializable

/**
 * Представляет цель урока
 */
@Serializable
data class LessonGoal(
    val id: String,
    val title: String,                   // Название цели (например: "practice_past_tense")
    val description: String? = null,     // Описание цели
    val skillType: SkillType,            // Тип навыка (grammar, vocabulary, listening, etc.)
    val targetLevel: String? = null,      // Целевой уровень (A1-C2)
    val isCompleted: Boolean = false,     // Выполнена ли цель
    val progress: Float = 0.0f,           // Прогресс выполнения (0.0 - 1.0)
    val exercisesCompleted: Int = 0,     // Количество выполненных упражнений
    val exercisesTotal: Int = 0,          // Общее количество упражнений
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    enum class SkillType {
        GRAMMAR,           // Грамматика
        VOCABULARY,        // Словарный запас
        LISTENING,         // Аудирование
        SPEAKING,          // Говорение
        READING,           // Чтение
        WRITING,           // Письмо
        PRONUNCIATION,     // Произношение
        CONVERSATION       // Разговорная практика
    }

    companion object {
        fun create(
            title: String,
            description: String? = null,
            skillType: SkillType,
            targetLevel: String? = null
        ): LessonGoal {
            return LessonGoal(
                id = "goal_${System.currentTimeMillis()}_${title.hashCode()}",
                title = title,
                description = description,
                skillType = skillType,
                targetLevel = targetLevel
            )
        }
    }

    fun updateProgress(
        exercisesCompleted: Int = this.exercisesCompleted,
        exercisesTotal: Int = this.exercisesTotal,
        isCompleted: Boolean = this.isCompleted
    ): LessonGoal {
        val newProgress = if (exercisesTotal > 0) {
            exercisesCompleted.toFloat() / exercisesTotal.toFloat()
        } else {
            0f
        }
        
        return copy(
            exercisesCompleted = exercisesCompleted,
            exercisesTotal = exercisesTotal,
            progress = newProgress.coerceIn(0f, 1f),
            isCompleted = isCompleted || newProgress >= 1.0f,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun formatForPrompt(): String {
        val builder = StringBuilder()
        builder.append("• $title")
        description?.let { builder.append(": $it") }
        builder.append(" (прогресс: ${(progress * 100).toInt()}%)")
        return builder.toString()
    }
}
