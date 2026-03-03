// domain/agent/memory/UserProfile.kt
package com.example.aiagent.domain.agent.memory

import kotlinx.serialization.Serializable

/**
 * Полный профиль ученика для изучения языка
 */
@Serializable
data class UserProfile(
    val name: String? = null,
    val nativeLanguage: String = "ru",           // Родной язык
    val targetLanguage: String = "en",            // Изучаемый язык
    val overallLevel: String = "A1",              // Общий уровень (A1-C2)
    val learningGoals: List<String> = emptyList(), // Цели обучения (например: ["work_abroad", "watch_movies"])
    val preferredLessonDuration: Int = 20,         // Предпочтительная длительность урока (минут)
    val weeklyGoal: Int = 180,                    // Цель на неделю (минут)
    val interests: List<String> = emptyList(),   // Интересы (например: ["technology", "travel", "music"])
    val age: Int? = null,
    val profession: String? = null,
    val learningStyle: String? = null,            // Стиль обучения (visual, auditory, kinesthetic)
    val timezone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun update(
        name: String? = this.name,
        nativeLanguage: String = this.nativeLanguage,
        targetLanguage: String = this.targetLanguage,
        overallLevel: String = this.overallLevel,
        learningGoals: List<String> = this.learningGoals,
        preferredLessonDuration: Int = this.preferredLessonDuration,
        weeklyGoal: Int = this.weeklyGoal,
        interests: List<String> = this.interests,
        age: Int? = this.age,
        profession: String? = this.profession,
        learningStyle: String? = this.learningStyle,
        timezone: String? = this.timezone
    ): UserProfile {
        return copy(
            name = name,
            nativeLanguage = nativeLanguage,
            targetLanguage = targetLanguage,
            overallLevel = overallLevel,
            learningGoals = learningGoals,
            preferredLessonDuration = preferredLessonDuration,
            weeklyGoal = weeklyGoal,
            interests = interests,
            age = age,
            profession = profession,
            learningStyle = learningStyle,
            timezone = timezone,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun formatForPrompt(): String {
        val builder = StringBuilder()
        builder.append("=== ПРОФИЛЬ УЧЕНИКА ===\n")
        name?.let { builder.append("Имя: $it\n") }
        builder.append("Родной язык: $nativeLanguage\n")
        builder.append("Изучаемый язык: $targetLanguage\n")
        builder.append("Уровень: $overallLevel\n")
        
        if (learningGoals.isNotEmpty()) {
            builder.append("Цели обучения: ${learningGoals.joinToString(", ")}\n")
        }
        
        if (interests.isNotEmpty()) {
            builder.append("Интересы: ${interests.joinToString(", ")}\n")
        }
        
        profession?.let { builder.append("Профессия: $it\n") }
        age?.let { builder.append("Возраст: $it\n") }
        learningStyle?.let { builder.append("Стиль обучения: $it\n") }
        
        builder.append("Предпочтительная длительность урока: $preferredLessonDuration мин\n")
        builder.append("Цель на неделю: $weeklyGoal мин\n")
        
        return builder.toString()
    }

    fun isValid(): Boolean {
        return overallLevel in listOf("A1", "A2", "B1", "B2", "C1", "C2") &&
               preferredLessonDuration > 0 &&
               weeklyGoal > 0
    }
}
