// data/database/MessageLocalRepository.kt
package com.example.aiagent.data.database

import android.content.Context
import android.content.SharedPreferences
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.domain.RepositoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MessageLocalRepository private constructor(
    private val messageDao: MessageDao,
    private val context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ai_agent_prefs", Context.MODE_PRIVATE)

    // Убираем lazy property и делаем функцию для получения sessionId
    private fun getCurrentSessionIdInternal(): String {
        var sessionId = prefs.getString("session_id", null)
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            prefs.edit().putString("session_id", sessionId).apply()
        }
        return sessionId
    }

    // ========== Базовые операции с сообщениями ==========

    suspend fun saveMessage(
        id: String,
        role: String,
        content: String,
        repositoryType: RepositoryType,
        modelName: String? = null,
        realTokenCount: Int? = null
    ) {
        val entity = MessageEntity(
            id = id,
            sessionId = getCurrentSessionIdInternal(),
            role = role,
            content = content,
            repositoryType = repositoryType.name,
            modelName = modelName,
            realTokenCount = realTokenCount
        )
        messageDao.insertMessage(entity)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    suspend fun getAllMessages(): List<MessageEntity> {
        return messageDao.getMessagesForSession(getCurrentSessionIdInternal())
    }

    suspend fun getMessageHistory(): List<ChatMessage> {
        return messageDao.getMessagesForSession(getCurrentSessionIdInternal())
            .map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    realTokenCount = entity.realTokenCount
                )
            }
    }

    fun getMessageHistoryFlow(): Flow<List<ChatMessage>> {
        return messageDao.getMessagesFlow(getCurrentSessionIdInternal())
            .map { entities ->
                entities.map {
                    ChatMessage(
                        id = it.id,
                        role = it.role,
                        content = it.content,
                        realTokenCount = it.realTokenCount
                    )
                }
            }
    }

    // ========== Управление историей ==========

    suspend fun clearHistory(keepSystemPrompt: Boolean) {
        val sessionId = getCurrentSessionIdInternal()
        if (keepSystemPrompt) {
            messageDao.deleteNonSystemMessages(sessionId)
        } else {
            messageDao.deleteAllMessages(sessionId)
        }
    }

    suspend fun removeLastMessage() {
        val sessionId = getCurrentSessionIdInternal()
        val messages = messageDao.getMessagesForSession(sessionId)
        if (messages.isNotEmpty()) {
            val lastMessage = messages.last()
            messageDao.deleteMessageById(lastMessage.id)
        }
    }

    suspend fun resetSession() {
        val oldSessionId = getCurrentSessionIdInternal()
        messageDao.deleteAllMessages(oldSessionId)
        val newSessionId = UUID.randomUUID().toString()
        prefs.edit().putString("session_id", newSessionId).apply()
    }

    // ========== Системный промпт ==========

    suspend fun setSystemPrompt(prompt: String) {
        val sessionId = getCurrentSessionIdInternal()
        val existingSystem = messageDao.getSystemMessage(sessionId)

        if (existingSystem == null) {
            messageDao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "system",
                    content = prompt,
                    repositoryType = "system",
                    realTokenCount = null
                )
            )
        } else {
            messageDao.updateSystemMessage(sessionId, prompt)
        }
    }

    suspend fun getSystemPrompt(): String? {
        val sessionId = getCurrentSessionIdInternal()
        return messageDao.getSystemMessage(sessionId)?.content
    }

    // ========== Геттеры ==========

    fun getMessageDao(): MessageDao = messageDao

    // Переименовываем метод, чтобы избежать конфликта
    fun provideSessionId(): String = getCurrentSessionIdInternal()

    // ========== Изучение языков ==========

    fun saveLanguageToLearn(language: String?) {
        prefs.edit().putString("language_to_learn", language).apply()
    }

    fun getLanguageToLearn(): String? {
        return prefs.getString("language_to_learn", null)
    }

    fun saveLearningGoal(goal: String?) {
        prefs.edit().putString("learning_goal", goal).apply()
    }

    fun getLearningGoal(): String? {
        return prefs.getString("learning_goal", null)
    }

    fun saveCurrentLevel(level: String?) {
        prefs.edit().putString("current_level", level).apply()
    }

    fun getCurrentLevel(): String? {
        return prefs.getString("current_level", null)
    }

    fun saveLessonsCompleted(count: Int) {
        prefs.edit().putInt("lessons_completed", count).apply()
    }

    fun getLessonsCompleted(): Int {
        return prefs.getInt("lessons_completed", 0)
    }

    fun saveExercisesCompleted(count: Int) {
        prefs.edit().putInt("exercises_completed", count).apply()
    }

    fun getExercisesCompleted(): Int {
        return prefs.getInt("exercises_completed", 0)
    }

    fun saveCorrectAnswers(count: Int) {
        prefs.edit().putInt("correct_answers", count).apply()
    }

    fun getCorrectAnswers(): Int {
        return prefs.getInt("correct_answers", 0)
    }

    fun saveTotalAnswers(count: Int) {
        prefs.edit().putInt("total_answers", count).apply()
    }

    fun getTotalAnswers(): Int {
        return prefs.getInt("total_answers", 0)
    }

    fun saveStreakDays(days: Int) {
        prefs.edit().putInt("streak_days", days).apply()
    }

    fun getStreakDays(): Int {
        return prefs.getInt("streak_days", 0)
    }

    // ========== Настройки пользователя ==========

    fun saveUserSettingsStyle(style: String) {
        prefs.edit().putString("user_settings_style", style).apply()
    }

    fun getUserSettingsStyle(): String? {
        return prefs.getString("user_settings_style", null)
    }

    fun saveUserSettingsFormat(format: String) {
        prefs.edit().putString("user_settings_format", format).apply()
    }

    fun getUserSettingsFormat(): String? {
        return prefs.getString("user_settings_format", null)
    }

    fun saveUserSettingsConstraints(constraints: String) {
        prefs.edit().putString("user_settings_constraints", constraints).apply()
    }

    fun getUserSettingsConstraints(): String? {
        return prefs.getString("user_settings_constraints", null)
    }

    fun saveUserSettingsMaxResponseLength(maxLength: Int) {
        prefs.edit().putInt("user_settings_max_response_length", maxLength).apply()
    }

    fun getUserSettingsMaxResponseLength(): Int {
        return prefs.getInt("user_settings_max_response_length", 0)
    }

    // ========== RAG настройки ==========

    fun saveRagEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("rag_enabled", enabled).apply()
    }

    fun isRagEnabled(): Boolean {
        return prefs.getBoolean("rag_enabled", true)
    }

    fun saveRagStrategy(strategy: String) {
        prefs.edit().putString("rag_strategy", strategy).apply()
    }

    fun getRagStrategy(): String? {
        return prefs.getString("rag_strategy", null)
    }

    companion object {
        @Volatile
        private var INSTANCE: MessageLocalRepository? = null

        fun getInstance(context: Context): MessageLocalRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getInstance(context)
                MessageLocalRepository(
                    database.messageDao(),
                    context.applicationContext
                ).also {
                    INSTANCE = it
                }
            }
        }
    }
}