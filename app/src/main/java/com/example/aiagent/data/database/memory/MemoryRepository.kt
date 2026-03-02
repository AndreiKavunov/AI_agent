// data/database/memory/MemoryRepository.kt
package com.example.aiagent.data.database.memory

import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Репозиторий для работы с памятью ассистента
 */
class MemoryRepository(
    private val memoryDao: MemoryDao
) {
    
    // ==================== Краткосрочная память ====================
    
    suspend fun createShortTermMemory(sessionId: String): ShortTermMemoryEntity {
        val memory = ShortTermMemoryEntity(sessionId = sessionId)
        memoryDao.insertShortTermMemory(memory)
        return memory
    }
    
    fun getShortTermMemory(sessionId: String): Flow<ShortTermMemoryEntity?> {
        return memoryDao.getShortTermMemory(sessionId)
    }
    
    suspend fun updateLastUserMessage(sessionId: String, message: String) {
        memoryDao.updateLastUserMessage(sessionId, message)
    }
    
    suspend fun updateLastAgentResponse(sessionId: String, response: String) {
        memoryDao.updateLastAgentResponse(sessionId, response)
    }
    
    suspend fun updateShortTermCurrentTopic(sessionId: String, topic: String?) {
        memoryDao.updateShortTermMemoryCurrentTopic(sessionId, topic)
    }
    
    suspend fun clearShortTermMemory(sessionId: String) {
        memoryDao.deleteShortTermMemory(sessionId)
    }
    
    // ==================== Рабочая память ====================
    
    suspend fun createWorkingMemory(
        sessionId: String,
        languageToLearn: String? = null,
        learningGoal: String? = null,
        currentLevel: String? = null
    ): WorkingMemoryEntity {
        val memory = WorkingMemoryEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            languageToLearn = languageToLearn,
            learningGoal = learningGoal,
            currentLevel = currentLevel
        )
        memoryDao.insertWorkingMemory(memory)
        return memory
    }
    
    fun getWorkingMemory(sessionId: String): Flow<WorkingMemoryEntity?> {
        return memoryDao.getWorkingMemory(sessionId)
    }
    
    suspend fun incrementLessonsCompleted(id: String) {
        memoryDao.incrementLessonsCompleted(id)
    }
    
    suspend fun incrementExercisesCompleted(id: String) {
        memoryDao.incrementExercisesCompleted(id)
    }
    
    suspend fun addCorrectAnswer(id: String) {
        memoryDao.addCorrectAnswer(id)
    }
    
    suspend fun addIncorrectAnswer(id: String) {
        memoryDao.addIncorrectAnswer(id)
    }
    
    suspend fun updateAchievements(id: String, achievements: String) {
        memoryDao.updateAchievements(id, achievements)
    }
    
    suspend fun updateWorkingMemoryCurrentTopic(id: String, topic: String?) {
        memoryDao.updateWorkingMemoryCurrentTopic(id, topic)
    }
    
    suspend fun updateCurrentLessonId(id: String, lessonId: String?) {
        memoryDao.updateCurrentLessonId(id, lessonId)
    }
    
    suspend fun clearWorkingMemory(sessionId: String) {
        memoryDao.deleteWorkingMemory(sessionId)
    }
    
    // ==================== Долговременная память ====================
    
    suspend fun saveToLongTermMemory(
        key: String,
        value: String,
        category: MemoryCategory,
        confidence: Float = 1.0f
    ) {
        val existing = memoryDao.getLongTermMemoryByKey(key)
        if (existing != null) {
            memoryDao.updateLongTermMemory(key, value)
        } else {
            val memory = LongTermMemoryEntity(
                key = key,
                value = value,
                category = category,
                confidence = confidence
            )
            memoryDao.insertLongTermMemory(memory)
        }
    }
    
    suspend fun getLongTermMemory(key: String): LongTermMemoryEntity? {
        val memory = memoryDao.getLongTermMemoryByKey(key)
        if (memory != null) {
            memoryDao.accessLongTermMemory(key)
        }
        return memory
    }
    
    fun getLongTermMemoryByCategory(category: MemoryCategory): Flow<List<LongTermMemoryEntity>> {
        return memoryDao.getLongTermMemoryByCategory(category.name)
    }
    
    fun getRecentLongTermMemory(limit: Int = 20): Flow<List<LongTermMemoryEntity>> {
        return memoryDao.getRecentLongTermMemory(limit)
    }
    
    fun getMostAccessedByCategory(category: MemoryCategory, limit: Int = 10): Flow<List<LongTermMemoryEntity>> {
        return memoryDao.getMostAccessedByCategory(category.name, limit)
    }
    
    suspend fun deleteLongTermMemory(key: String) {
        memoryDao.deleteLongTermMemory(key)
    }
    
    suspend fun deleteLongTermMemoryByCategory(category: MemoryCategory) {
        memoryDao.deleteLongTermMemoryByCategory(category.name)
    }
    
    suspend fun clearAllLongTermMemory() {
        memoryDao.clearAllLongTermMemory()
    }
    
    // ==================== Специализированные методы ====================
    
    fun getAllLanguageLearningSessions(): Flow<List<WorkingMemoryEntity>> {
        return memoryDao.getAllLanguageLearningSessions()
    }
    
    fun getLearningGoals(): Flow<List<LongTermMemoryEntity>> {
        return memoryDao.getLearningGoals()
    }
    
    suspend fun saveLearningGoal(language: String, goal: String) {
        saveToLongTermMemory(
            key = "learning_goal_$language",
            value = goal,
            category = MemoryCategory.LEARNING_GOALS
        )
    }
    
    suspend fun saveUserProfile(name: String?, age: Int?, interests: String?) {
        name?.let {
            saveToLongTermMemory("profile_name", it, MemoryCategory.PROFILE)
        }
        age?.let {
            saveToLongTermMemory("profile_age", it.toString(), MemoryCategory.PROFILE)
        }
        interests?.let {
            saveToLongTermMemory("profile_interests", it, MemoryCategory.PROFILE)
        }
    }
    
    suspend fun getUserProfile(): Map<String, String> {
        val profile = mutableMapOf<String, String>()
        memoryDao.getLongTermMemoryByCategory(MemoryCategory.PROFILE.name)
            .collect { memories ->
                memories.forEach { memory ->
                    profile[memory.key] = memory.value
                }
            }
        return profile
    }
}
