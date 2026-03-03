// domain/agent/memory/LanguageMemoryManager.kt
package com.example.aiagent.domain.agent.memory

import android.util.Log
import com.example.aiagent.data.database.memory.MemoryCategory
import com.example.aiagent.data.database.memory.MemoryRepository
import com.example.aiagent.data.database.memory.ShortTermMemoryEntity
import com.example.aiagent.data.database.memory.WorkingMemoryEntity
import com.example.aiagent.data.huggingFace.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "LanguageMemoryManager"

/**
 * Менеджер трёхуровневой памяти для изучения языка
 * 
 * УРОВЕНЬ 1: SHORT_TERM_MEMORY (Краткосрочная)
 * - Последние 10-20 сообщений текущего диалога
 * - Текущий контекст разговора
 * - Недавние исправления ошибок
 * 
 * УРОВЕНЬ 2: WORKING_MEMORY (Рабочая память)
 * - Текущая тема урока
 * - Цели урока
 * - Временные ошибки
 * - Контекст задания
 * 
 * УРОВЕНЬ 3: LONG_TERM_MEMORY (Долговременная)
 * - Профиль ученика
 * - Словарный запас
 * - Грамматические правила
 * - Прогресс обучения
 */
class LanguageMemoryManager(
    private val memoryRepository: MemoryRepository,
    private val factExtractor: LanguageFactExtractor
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val MAX_SHORT_TERM_MESSAGES = 20

    /**
     * Обрабатывает сообщение и сохраняет его в соответствующий уровень памяти
     */
    suspend fun processMessage(
        sessionId: String,
        message: ChatMessage,
        repositoryType: com.example.aiagent.domain.RepositoryType,
        modelName: String? = null
    ): ProcessResult {
        Log.d(TAG, "📥 Обработка сообщения в LanguageMemoryManager")
        
        try {
            // 1. Обновляем краткосрочную память
            updateShortTermMemory(sessionId, message)
            
            // 2. Извлекаем языковые факты из сообщения
            val languageFacts = factExtractor.extractLanguageFacts(
                message = message.content,
                repositoryType = repositoryType,
                modelName = modelName
            )
            
            // 3. Определяем тип памяти для каждого факта
            val memoryType = factExtractor.determineMemoryType(languageFacts)
            
            // 4. Сохраняем факты в соответствующий уровень памяти
            when (memoryType) {
                MemoryType.WORKING_MEMORY -> {
                    updateWorkingMemory(sessionId, languageFacts)
                }
                MemoryType.LONG_TERM_MEMORY -> {
                    updateLongTermMemory(sessionId, languageFacts)
                }
                MemoryType.SHORT_TERM_MEMORY -> {
                    // Уже обновлено в шаге 1
                }
            }
            
            return ProcessResult(
                success = true,
                memoryType = memoryType,
                factsExtracted = languageFacts
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при обработке сообщения: ${e.message}")
            return ProcessResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Обновляет краткосрочную память (УРОВЕНЬ 1)
     */
    private suspend fun updateShortTermMemory(
        sessionId: String,
        message: ChatMessage
    ) {
        Log.d(TAG, "📝 Обновление краткосрочной памяти")
        
        // Получаем или создаём краткосрочную память
        val shortTermMemory = memoryRepository.getShortTermMemory(sessionId).first()
        if (shortTermMemory == null) {
            memoryRepository.createShortTermMemory(sessionId)
        }
        
        // Обновляем последнее сообщение
        when (message.role) {
            "user" -> {
                memoryRepository.updateLastUserMessage(sessionId, message.content)
            }
            "assistant" -> {
                memoryRepository.updateLastAgentResponse(sessionId, message.content)
            }
        }
        
        // TODO: Добавить хранение последних 10-20 сообщений
        // Для этого нужно добавить поле recentMessages в ShortTermMemoryEntity
    }

    /**
     * Обновляет рабочую память (УРОВЕНЬ 2)
     */
    private suspend fun updateWorkingMemory(
        sessionId: String,
        facts: ExtractedLanguageFacts
    ) {
        Log.d(TAG, "📝 Обновление рабочей памяти")
        
        val workingMemory = memoryRepository.getWorkingMemory(sessionId).first()
        
        val currentWorkingMemory = if (workingMemory == null) {
            // Создаём новую рабочую память, если её нет
            val firstTopic = facts.lessonTopics.firstOrNull()
            memoryRepository.createWorkingMemory(
                sessionId = sessionId,
                languageToLearn = null,
                learningGoal = facts.learningGoals.firstOrNull(),
                currentLevel = firstTopic?.difficulty
            )
        } else {
            // Обновляем существующую рабочую память
            
            // Обновляем тему урока
            facts.lessonTopics.firstOrNull()?.let { topic ->
                memoryRepository.updateWorkingMemoryCurrentTopic(workingMemory.id, topic.topic)
            }
            
            // Обновляем ID упражнения
            facts.exerciseContext?.exercise_id?.let { exerciseId ->
                memoryRepository.updateCurrentLessonId(workingMemory.id, exerciseId)
            }
            
            workingMemory
        }
        
        // Сохраняем ошибки в рабочую память
        facts.mistakes.forEach { mistake ->
            val recentMistake = RecentMistake.create(
                error = mistake.error,
                correction = mistake.correction,
                rule = mistake.rule ?: "unknown",
                explanation = mistake.explanation
            )
            // Сохраняем ошибку в долговременной памяти как JSON
            memoryRepository.saveToLongTermMemory(
                key = "mistake_${currentWorkingMemory.id}_${mistake.error.hashCode()}",
                value = json.encodeToString(recentMistake),
                category = MemoryCategory.CUSTOM
            )
        }
    }

    /**
     * Обновляет долговременную память (УРОВЕНЬ 3)
     */
    private suspend fun updateLongTermMemory(
        sessionId: String,
        facts: ExtractedLanguageFacts
    ) {
        Log.d(TAG, "📝 Обновление долговременной памяти")
        
        // Сохраняем словарь
        facts.vocabularyWords.forEach { vocab ->
            val vocabItem = VocabularyItem.create(
                word = vocab.word,
                translation = vocab.translation ?: "",
                partOfSpeech = vocab.part_of_speech,
                example = vocab.example,
                topic = vocab.topic
            )
            memoryRepository.saveToLongTermMemory(
                key = "vocab_${vocab.word}",
                value = json.encodeToString(vocabItem),
                category = MemoryCategory.CUSTOM // TODO: Добавить VOCABULARY категорию
            )
        }
        
        // Сохраняем грамматические правила
        facts.grammarRules.forEach { rule ->
            memoryRepository.saveToLongTermMemory(
                key = "grammar_${rule.rule.hashCode()}",
                value = json.encodeToString(rule),
                category = MemoryCategory.CUSTOM // TODO: Добавить GRAMMAR_RULES категорию
            )
        }
        
        // Сохраняем цели обучения
        facts.learningGoals.forEach { goal ->
            memoryRepository.saveLearningGoal(
                language = "en", // TODO: Получать из контекста
                goal = goal
            )
        }
    }

    /**
     * Строит промпт с контекстом из всех уровней памяти
     */
    suspend fun buildPrompt(sessionId: String, basePrompt: String): String {
        Log.d(TAG, "🔨 Построение промпта с контекстом памяти")
        
        val builder = StringBuilder(basePrompt)
        
        // 1. Добавляем контекст из краткосрочной памяти
        val shortTermMemory = memoryRepository.getShortTermMemory(sessionId).first()
        if (shortTermMemory != null) {
            builder.append("\n\n=== КОНТЕКСТ ДИАЛОГА ===\n")
            shortTermMemory.lastUserMessage?.let {
                builder.append("Последнее сообщение пользователя: $it\n")
            }
            shortTermMemory.lastAgentResponse?.let {
                builder.append("Последний ответ ассистента: ${it.take(100)}...\n")
            }
            shortTermMemory.currentTopic?.let {
                builder.append("Текущая тема: $it\n")
            }
        }
        
        // 2. Добавляем контекст из рабочей памяти
        val workingMemory = memoryRepository.getWorkingMemory(sessionId).first()
        if (workingMemory != null) {
            builder.append("\n=== ТЕКУЩИЙ УРОК ===\n")
            workingMemory.languageToLearn?.let {
                builder.append("Изучаемый язык: $it\n")
            }
            workingMemory.learningGoal?.let {
                builder.append("Цель обучения: $it\n")
            }
            workingMemory.currentLevel?.let {
                builder.append("Уровень: $it\n")
            }
            workingMemory.currentTopic?.let {
                builder.append("Тема урока: $it\n")
            }
            builder.append("Пройдено уроков: ${workingMemory.lessonsCompleted}\n")
            builder.append("Выполнено упражнений: ${workingMemory.exercisesCompleted}\n")
            builder.append("Правильных ответов: ${workingMemory.correctAnswers}/${workingMemory.totalAnswers}\n")
            
            // TODO: Добавить последние ошибки, цели урока, целевой словарь
        }
        
        // 3. Добавляем контекст из долговременной памяти
        val userProfile = memoryRepository.getUserProfile()
        if (userProfile.isNotEmpty()) {
            builder.append("\n=== ПРОФИЛЬ УЧЕНИКА ===\n")
            userProfile.forEach { (key, value) ->
                builder.append("$key: $value\n")
            }
        }
        
        // TODO: Добавить словарный запас и грамматические правила
        
        return builder.toString()
    }

    /**
     * Получает профиль пользователя
     */
    suspend fun getUserProfile(): UserProfile? {
        val profileData = memoryRepository.getUserProfile()
        if (profileData.isEmpty()) return null
        
        return UserProfile(
            name = profileData["profile_name"],
            nativeLanguage = profileData["native_language"] ?: "ru",
            targetLanguage = profileData["target_language"] ?: "en",
            overallLevel = profileData["overall_level"] ?: "A1",
            learningGoals = profileData["learning_goals"]?.split(",")?.map { it.trim() } ?: emptyList(),
            preferredLessonDuration = profileData["preferred_duration"]?.toIntOrNull() ?: 20,
            weeklyGoal = profileData["weekly_goal"]?.toIntOrNull() ?: 180,
            interests = profileData["interests"]?.split(",")?.map { it.trim() } ?: emptyList(),
            age = profileData["age"]?.toIntOrNull(),
            profession = profileData["profession"]
        )
    }

    /**
     * Сохраняет профиль пользователя
     */
    suspend fun saveUserProfile(profile: UserProfile) {
        memoryRepository.saveUserProfile(
            name = profile.name,
            age = profile.age,
            interests = profile.interests.joinToString(", ")
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "native_language",
            value = profile.nativeLanguage,
            category = MemoryCategory.PROFILE
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "target_language",
            value = profile.targetLanguage,
            category = MemoryCategory.PROFILE
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "overall_level",
            value = profile.overallLevel,
            category = MemoryCategory.PROFILE
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "learning_goals",
            value = profile.learningGoals.joinToString(","),
            category = MemoryCategory.LEARNING_GOALS
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "preferred_duration",
            value = profile.preferredLessonDuration.toString(),
            category = MemoryCategory.PROFILE
        )
        
        memoryRepository.saveToLongTermMemory(
            key = "weekly_goal",
            value = profile.weeklyGoal.toString(),
            category = MemoryCategory.PROFILE
        )
    }

    /**
     * Очищает краткосрочную память для сессии
     */
    suspend fun clearShortTermMemory(sessionId: String) {
        memoryRepository.clearShortTermMemory(sessionId)
        Log.d(TAG, "🧹 Краткосрочная память очищена для сессии: $sessionId")
    }

    /**
     * Очищает рабочую память для сессии
     */
    suspend fun clearWorkingMemory(sessionId: String) {
        memoryRepository.clearWorkingMemory(sessionId)
        Log.d(TAG, "🧹 Рабочая память очищена для сессии: $sessionId")
    }

    /**
     * Получает статистику обучения
     */
    suspend fun getLearningStats(sessionId: String): LearningStats? {
        val workingMemory = memoryRepository.getWorkingMemory(sessionId).first()
            ?: return null
        
        return LearningStats(
            lessonsCompleted = workingMemory.lessonsCompleted,
            exercisesCompleted = workingMemory.exercisesCompleted,
            correctAnswers = workingMemory.correctAnswers,
            totalAnswers = workingMemory.totalAnswers,
            accuracy = if (workingMemory.totalAnswers > 0) {
                workingMemory.correctAnswers.toFloat() / workingMemory.totalAnswers.toFloat()
            } else 0f,
            streakDays = workingMemory.streakDays,
            currentLevel = workingMemory.currentLevel
        )
    }
}

/**
 * Результат обработки сообщения
 */
data class ProcessResult(
    val success: Boolean,
    val memoryType: MemoryType? = null,
    val factsExtracted: ExtractedLanguageFacts? = null,
    val error: String? = null
)

/**
 * Статистика обучения
 */
data class LearningStats(
    val lessonsCompleted: Int,
    val exercisesCompleted: Int,
    val correctAnswers: Int,
    val totalAnswers: Int,
    val accuracy: Float,
    val streakDays: Int,
    val currentLevel: String?
)
