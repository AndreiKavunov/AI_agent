// domain/agent/UniversalAgent.kt
package com.example.aiagent.domain.agent

import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.memory.LearningStats
import com.example.aiagent.domain.agent.memory.UserProfile
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.domain.contextStrategy.DialogBranch
import com.example.aiagent.domain.contextStrategy.DialogFact

interface UniversalAgent {
    // Основные методы
    suspend fun processMessage(message: String, temperature: Double): AgentResponse
    suspend fun clearHistory()
    suspend fun clearAll()
    suspend fun setTemperature(temperature: Double)
    suspend fun setSystemPrompt(prompt: String)
    suspend fun getSystemPrompt(): String?
    fun getCurrentAgentInfo(): String
    suspend fun switchRepository(type: RepositoryType)
    fun getCurrentRepositoryType(): RepositoryType
    suspend fun setHuggingFaceModel(modelType: HuggingFaceModel)
    fun getCurrentHuggingFaceModel(): HuggingFaceModel?

    // Методы для подсчёта токенов
    suspend fun getCurrentQueryTokens(): Int
    suspend fun getTotalHistoryTokens(): DetailedTokenCount
    suspend fun getLastResponseTokens(): Int?
    fun getTokenCounter(): TokenCounter

    // ========== Методы для работы со стратегиями контекста ==========

    /**
     * Устанавливает стратегию управления контекстом
     */
    suspend fun setContextStrategy(strategy: ContextStrategy)

    /**
     * Возвращает текущую стратегию управления контекстом
     */
    fun getCurrentContextStrategy(): ContextStrategy

    /**
     * Возвращает текущие факты, извлеченные из диалога
     */
    fun getCurrentFacts(): Map<String, DialogFact>

    /**
     * Возвращает список всех веток диалога
     */
    fun getBranches(): List<DialogBranch>

    /**
     * Возвращает ID текущей ветки (если используется стратегия Branching)
     */
    fun getCurrentBranchId(): String?

    /**
     * Создает новую ветку от указанного сообщения
     * @return true если ветка создана успешно
     */
    suspend fun createBranch(checkpointMessageId: String, branchName: String): Boolean

    /**
     * Переключается на указанную ветку
     * @return true если переключение успешно
     */
    suspend fun switchBranch(branchId: String): Boolean

    /**
     * Удаляет указанную ветку
     * @return true если ветка удалена успешно
     */
    suspend fun deleteBranch(branchId: String): Boolean

    /**
     * Возвращает сообщения для текущего контекста
     */
    suspend fun getMessages(): List<ChatMessage>

    // ========== Методы для изучения языков ==========

    /**
     * Сохраняет язык для изучения
     */
    fun saveLanguageToLearn(language: String?)

    /**
     * Возвращает язык для изучения
     */
    fun getLanguageToLearn(): String?

    /**
     * Сохраняет цель обучения
     */
    fun saveLearningGoal(goal: String?)

    /**
     * Возвращает цель обучения
     */
    fun getLearningGoal(): String?

    /**
     * Сохраняет текущий уровень
     */
    fun saveCurrentLevel(level: String?)

    /**
     * Возвращает текущий уровень
     */
    fun getCurrentLevel(): String?

    /**
     * Сохраняет количество пройденных уроков
     */
    fun saveLessonsCompleted(count: Int)

    /**
     * Возвращает количество пройденных уроков
     */
    fun getLessonsCompleted(): Int

    /**
     * Сохраняет количество выполненных упражнений
     */
    fun saveExercisesCompleted(count: Int)

    /**
     * Возвращает количество выполненных упражнений
     */
    fun getExercisesCompleted(): Int

    /**
     * Сохраняет количество правильных ответов
     */
    fun saveCorrectAnswers(count: Int)

    /**
     * Возвращает количество правильных ответов
     */
    fun getCorrectAnswers(): Int

    /**
     * Сохраняет общее количество ответов
     */
    fun saveTotalAnswers(count: Int)

    /**
     * Возвращает общее количество ответов
     */
    fun getTotalAnswers(): Int

    /**
     * Сохраняет количество дней подряд
     */
    fun saveStreakDays(days: Int)

    /**
     * Возвращает количество дней подряд
     */
    fun getStreakDays(): Int

    /**
     * Очищает все данные об изучении языков
     */
    fun clearLanguageLearningData()

    // ========== Методы для работы с LanguageMemoryManager ==========

    /**
     * Получает профиль пользователя из долговременной памяти
     */
    suspend fun getUserProfile(): UserProfile?

    /**
     * Сохраняет профиль пользователя в долговременную память
     */
    suspend fun saveUserProfile(profile: UserProfile)

    /**
     * Получает статистику обучения
     */
    suspend fun getLearningStats(): LearningStats?

    /**
     * Строит промпт с контекстом из всех уровней памяти
     */
    suspend fun buildMemoryPrompt(basePrompt: String): String

    /**
     * Очищает краткосрочную память
     */
    suspend fun clearShortTermMemory()

    /**
     * Очищает рабочую память
     */
    suspend fun clearWorkingMemory()
}