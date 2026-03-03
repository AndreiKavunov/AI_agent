// data/database/memory/MemoryDao.kt
package com.example.aiagent.data.database.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    
    // ==================== Краткосрочная память ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortTermMemory(memory: ShortTermMemoryEntity)
    
    @Query("SELECT * FROM short_term_memory WHERE sessionId = :sessionId LIMIT 1")
    fun getShortTermMemory(sessionId: String): Flow<ShortTermMemoryEntity?>
    
    @Query("UPDATE short_term_memory SET lastUserMessage = :message, lastActivityTime = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateLastUserMessage(sessionId: String, message: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE short_term_memory SET lastAgentResponse = :response, lastActivityTime = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateLastAgentResponse(sessionId: String, response: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE short_term_memory SET currentTopic = :topic WHERE sessionId = :sessionId")
    suspend fun updateShortTermMemoryCurrentTopic(sessionId: String, topic: String?)
    
    @Query("UPDATE short_term_memory SET conversationContext = :context WHERE sessionId = :sessionId")
    suspend fun updateConversationContext(sessionId: String, context: String)
    
    @Query("DELETE FROM short_term_memory WHERE sessionId = :sessionId")
    suspend fun deleteShortTermMemory(sessionId: String)
    
    // ==================== Рабочая память ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkingMemory(memory: WorkingMemoryEntity)
    
    @Query("SELECT * FROM working_memory WHERE sessionId = :sessionId LIMIT 1")
    fun getWorkingMemory(sessionId: String): Flow<WorkingMemoryEntity?>
    
    @Query("UPDATE working_memory SET lessonsCompleted = lessonsCompleted + 1 WHERE id = :id")
    suspend fun incrementLessonsCompleted(id: String)
    
    @Query("UPDATE working_memory SET exercisesCompleted = exercisesCompleted + 1 WHERE id = :id")
    suspend fun incrementExercisesCompleted(id: String)
    
    @Query("UPDATE working_memory SET correctAnswers = correctAnswers + 1, totalAnswers = totalAnswers + 1 WHERE id = :id")
    suspend fun addCorrectAnswer(id: String)
    
    @Query("UPDATE working_memory SET totalAnswers = totalAnswers + 1 WHERE id = :id")
    suspend fun addIncorrectAnswer(id: String)
    
    @Query("UPDATE working_memory SET achievements = :achievements WHERE id = :id")
    suspend fun updateAchievements(id: String, achievements: String)
    
    @Query("UPDATE working_memory SET currentTopic = :topic WHERE id = :id")
    suspend fun updateWorkingMemoryCurrentTopic(id: String, topic: String?)
    
    @Query("UPDATE working_memory SET currentLessonId = :lessonId WHERE id = :id")
    suspend fun updateCurrentLessonId(id: String, lessonId: String?)
    
    @Query("UPDATE working_memory SET currentLevel = :level WHERE id = :id")
    suspend fun updateCurrentLevel(id: String, level: String?)
    
    @Query("UPDATE working_memory SET languageToLearn = :language WHERE id = :id")
    suspend fun updateLanguageToLearn(id: String, language: String?)
    
    @Query("UPDATE working_memory SET learningGoal = :goal WHERE id = :id")
    suspend fun updateLearningGoal(id: String, goal: String?)
    
    @Query("UPDATE working_memory SET lastPracticeDate = :date WHERE id = :id")
    suspend fun updateLastPracticeDate(id: String, date: Long)
    
    @Query("UPDATE working_memory SET streakDays = :days WHERE id = :id")
    suspend fun updateStreakDays(id: String, days: Int)
    
    @Query("DELETE FROM working_memory WHERE sessionId = :sessionId")
    suspend fun deleteWorkingMemory(sessionId: String)
    
    // ==================== Долговременная память ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLongTermMemory(memory: LongTermMemoryEntity)
    
    @Query("SELECT * FROM long_term_memory WHERE memoryKey = :key LIMIT 1")
    suspend fun getLongTermMemoryByKey(key: String): LongTermMemoryEntity?
    
    @Query("SELECT * FROM long_term_memory WHERE category = :category ORDER BY lastAccessed DESC")
    fun getLongTermMemoryByCategory(category: String): Flow<List<LongTermMemoryEntity>>
    
    @Query("SELECT * FROM long_term_memory ORDER BY lastAccessed DESC LIMIT :limit")
    fun getRecentLongTermMemory(limit: Int = 20): Flow<List<LongTermMemoryEntity>>
    
    @Query("SELECT * FROM long_term_memory WHERE category = :category ORDER BY accessCount DESC LIMIT :limit")
    fun getMostAccessedByCategory(category: String, limit: Int = 10): Flow<List<LongTermMemoryEntity>>
    
    @Query("UPDATE long_term_memory SET value = :value, lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE memoryKey = :key")
    suspend fun updateLongTermMemory(key: String, value: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE long_term_memory SET lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE memoryKey = :key")
    suspend fun accessLongTermMemory(key: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM long_term_memory WHERE memoryKey = :key")
    suspend fun deleteLongTermMemory(key: String)
    
    @Query("DELETE FROM long_term_memory WHERE category = :category")
    suspend fun deleteLongTermMemoryByCategory(category: String)
    
    @Query("DELETE FROM long_term_memory")
    suspend fun clearAllLongTermMemory()
    
    // ==================== Полезные запросы ====================
    
    @Query("SELECT * FROM working_memory WHERE languageToLearn IS NOT NULL ORDER BY lastPracticeDate DESC")
    fun getAllLanguageLearningSessions(): Flow<List<WorkingMemoryEntity>>
    
    @Query("SELECT * FROM long_term_memory WHERE category = 'LEARNING_GOALS'")
    fun getLearningGoals(): Flow<List<LongTermMemoryEntity>>
}
