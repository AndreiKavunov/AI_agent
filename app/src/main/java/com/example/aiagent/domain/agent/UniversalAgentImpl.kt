// domain/agent/UniversalAgentImpl.kt
package com.example.aiagent.domain.agent

import android.util.Log
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.database.SummaryDao
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.summary.SummaryManager
import com.example.aiagent.domain.agent.memory.LanguageMemoryManager
import com.example.aiagent.domain.agent.memory.UserProfile
import com.example.aiagent.domain.agent.memory.LearningStats
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.domain.contextStrategy.ContextStrategyManager
import com.example.aiagent.domain.contextStrategy.DialogFact
import com.example.aiagent.domain.contextStrategy.DialogBranch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "UniversalAgent"

class UniversalAgentImpl(
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl,
    private val localRepository: MessageLocalRepository,
    private val summaryDao: SummaryDao,
    private val contextStrategyManager: ContextStrategyManager,
    private val languageMemoryManager: LanguageMemoryManager
) : UniversalAgent {

    private var currentType = RepositoryType.GIGACHAT
    private var currentHuggingFaceModel: HuggingFaceModel? = null
    private val tokenCounter = TokenCounter()
    private var currentTemperature: Double = 0.7

    // Инициализируем SummaryManager
    private val summaryManager by lazy {
        SummaryManager(
            localRepository = localRepository,
            summaryDao = summaryDao,
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository
        )
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeSystemPrompt()
        }
    }

    private suspend fun initializeSystemPrompt() {
        try {
            val existingPrompt = localRepository.getSystemPrompt()
            if (existingPrompt == null) {
                setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при инициализации системного промпта: ${e.message}")
        }
    }

// domain/agent/UniversalAgentImpl.kt
// Полный метод processMessage

// domain/agent/UniversalAgentImpl.kt
// Полный метод processMessage с улучшенной обработкой фактов

    override suspend fun processMessage(message: String, temperature: Double): AgentResponse {
        Log.d(TAG, "🚀 UniversalAgent обрабатывает сообщение через ${currentType}")
        Log.d(TAG, "📝 Текст сообщения: $message")

        // Сохраняем сообщение пользователя
        val userMessageId = UUID.randomUUID().toString()
        val userChatMessage = ChatMessage(
            id = userMessageId,
            role = "user",
            content = message
        )

        localRepository.saveMessage(
            id = userMessageId,
            role = "user",
            content = message,
            repositoryType = currentType,
            modelName = currentHuggingFaceModel?.displayName
        )
        Log.d(TAG, "💾 Сообщение пользователя сохранено в БД: $userMessageId")

        // Обрабатываем сообщение через LanguageMemoryManager
        withContext(Dispatchers.IO) {
            launch(Dispatchers.IO) {
            try {
                val sessionId = localRepository.provideSessionId()
                languageMemoryManager.processMessage(
                    sessionId = sessionId,
                    message = userChatMessage,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при обработке сообщения в LanguageMemoryManager: ${e.message}")
            }
            }
        }

        // Добавляем сообщение в текущую ветку, если используется ветвление
        if (contextStrategyManager.getCurrentStrategy() is ContextStrategy.Branching) {
            contextStrategyManager.addMessageToCurrentBranch(userChatMessage)
            Log.d(TAG, "🌿 Сообщение добавлено в текущую ветку")
        }

        return withContext(Dispatchers.IO) {
            try {
                // Получаем все сообщения из БД
                val allMessages = localRepository.getMessageHistory()
                Log.d(TAG, "📚 Загружено ${allMessages.size} сообщений из БД")

                // Получаем системный промпт с настройками пользователя
                val systemPromptWithSettings = getSystemPromptWithSettings()

                // Временно обновляем системный промпт в БД для текущего запроса
                val currentSystemPrompt = localRepository.getSystemPrompt()
                localRepository.setSystemPrompt(systemPromptWithSettings)

                // Перезагружаем сообщения с обновленным системным промптом
                val messagesWithUpdatedPrompt = localRepository.getMessageHistory()

                // Восстанавливаем базовый системный промпт
                localRepository.setSystemPrompt(currentSystemPrompt ?: "Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")

                // Применяем текущую стратегию контекста для подготовки сообщений к API
                val messagesForApi = contextStrategyManager.prepareMessagesForApi(messagesWithUpdatedPrompt)
                Log.d(TAG, "🎯 Применена стратегия: ${contextStrategyManager.getCurrentStrategy().name}")
                Log.d(TAG, "📤 Подготовлено ${messagesForApi.size} сообщений для API")

                // Конвертируем в GigaMessage
                val apiHistory = toGigaMessages(messagesForApi)

                // Детальное логирование
                Log.d(TAG, "📤 Отправка в API (${apiHistory.size} сообщений):")
                apiHistory.forEachIndexed { index, msg ->
                    val role = msg.role
                    val content = if (role == "system") {
                        msg.content.take(200) + if (msg.content.length > 200) "..." else ""
                    } else {
                        msg.content.take(100) + if (msg.content.length > 100) "..." else ""
                    }
                    Log.d(TAG, "   [$index] ${role}: $content")
                }

                // Проверяем, что system сообщение только одно
                val systemCount = apiHistory.count { it.role == "system" }
                if (systemCount != 1) {
                    Log.w(TAG, "⚠️ ВНИМАНИЕ: в истории ${systemCount} system сообщений!")
                }

                // Отправляем основной запрос
                Log.d(TAG, "🌡️ Температура: $temperature")
                val response = when (currentType) {
                    RepositoryType.GIGACHAT -> {
                        gigaChatRepository.sendMessageWithHistory(
                            history = apiHistory,
                            temperature = temperature
                        )
                    }
                    RepositoryType.HUGGINGFACE -> {
                        huggingFaceRepository.sendMessageWithHistory(
                            history = apiHistory,
                            temperature = temperature,
                        )
                    }
                }

                Log.d(TAG, "✅ Основной ответ получен за ${response.responseTimeMs}ms")
                Log.d(TAG, "📊 Токены: ${response.tokenCount}")

                // Сохраняем ответ
                val assistantMessageId = UUID.randomUUID().toString()
                val assistantChatMessage = ChatMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = response.text,
                    realTokenCount = response.tokenCount
                )

                localRepository.saveMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = response.text,
                    repositoryType = currentType,
                    modelName = currentHuggingFaceModel?.displayName,
                    realTokenCount = response.tokenCount
                )
                Log.d(TAG, "💾 Ответ ассистента сохранен в БД: $assistantMessageId")

                // Обрабатываем ответ ассистента через LanguageMemoryManager
                launch(Dispatchers.IO) {
                    try {
                        val sessionId = localRepository.provideSessionId()
                        languageMemoryManager.processMessage(
                            sessionId = sessionId,
                            message = assistantChatMessage,
                            repositoryType = currentType,
                            modelName = currentHuggingFaceModel?.displayName
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Ошибка при обработке ответа в LanguageMemoryManager: ${e.message}")
                    }
                }

                // Добавляем ответ в текущую ветку, если используется ветвление
                if (contextStrategyManager.getCurrentStrategy() is ContextStrategy.Branching) {
                    contextStrategyManager.addMessageToCurrentBranch(assistantChatMessage)
                    Log.d(TAG, "🌿 Ответ добавлен в текущую ветку")
                }

                // Запускаем проверку суммаризации
                summaryManager.checkAndSummarizeIfNeeded(currentType)

                // Извлекаем факты ТОЛЬКО если это стратегия Sticky Facts
                // и делаем это после основного ответа, чтобы не блокировать пользователя
                if (contextStrategyManager.getCurrentStrategy() is ContextStrategy.StickyFacts) {
                    Log.d(TAG, "🔍 Запускаем извлечение фактов из сообщения пользователя")

                    // Запускаем в фоне, не блокируя возврат ответа
                    launch(Dispatchers.IO) {
                        try {
                            val startTime = System.currentTimeMillis()
                            contextStrategyManager.updateFactsWithLLM(
                                message = message,
                                repositoryType = currentType,
                                modelName = currentHuggingFaceModel?.displayName
                            )
                            val duration = System.currentTimeMillis() - startTime
                            Log.d(TAG, "⏱️ Извлечение фактов заняло: ${duration}ms")

                            // Логируем текущие факты
                            val currentFacts = contextStrategyManager.getCurrentFacts()
                            if (currentFacts.isNotEmpty()) {
                                Log.d(TAG, "📊 Текущие факты (${currentFacts.size}):")
                                currentFacts.values.forEach { fact ->
                                    Log.d(TAG, "   • ${fact.key}: ${fact.value} (уверенность: ${fact.confidence})")
                                }
                            } else {
                                Log.d(TAG, "📊 Фактов пока нет")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Ошибка при извлечении фактов: ${e.message}")
                            if (e.message?.contains("429") == true) {
                                Log.w(TAG, "⚠️ Превышен лимит запросов, пропускаем извлечение фактов")
                            }
                        }
                    }
                }

                AgentResponse(
                    text = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount ?: 0,
                    promptTokens = response.promptTokens ?: 0,
                    totalHistoryTokens = response.totalHistoryTokens ?: 0
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Ошибка: ${e.message}")
                e.printStackTrace()

                // Логируем детали ошибки
                if (e.message?.contains("429") == true) {
                    Log.e(TAG, "⚠️ Слишком много запросов к API. Нужно уменьшить частоту или добавить задержки")
                }

                // Удаляем сообщение пользователя в случае ошибки
                try {
                    localRepository.deleteMessage(userMessageId)
                    Log.d(TAG, "🗑️ Удалено сообщение пользователя из-за ошибки: $userMessageId")
                } catch (_: Exception) { }

                throw e
            }
        }
    }

    /**
     * Конвертирует ChatMessage в GigaMessage для отправки в API
     */
    private fun toGigaMessages(messages: List<ChatMessage>): List<GigaMessage> {
        return messages.map { chatMessage ->
            GigaMessage(
                role = chatMessage.role,
                content = chatMessage.content
            )
        }
    }

    override suspend fun setTemperature(temperature: Double) {
        currentTemperature = temperature
    }

    // Конвертация ChatMessage в GigaMessage

    override suspend fun getTotalHistoryTokens(): DetailedTokenCount {
        val history = localRepository.getMessageHistory()
        val allRealTokens = mutableMapOf<String, Int>()
        history.forEach { msg ->
            msg.realTokenCount?.let { allRealTokens[msg.id] = it }
        }
        return tokenCounter.detailedCount(history, allRealTokens)
    }

    override suspend fun getLastResponseTokens(): Int? {
        val history = localRepository.getMessageHistory()
        val lastAssistant = history.lastOrNull { it.role == "assistant" }
        return lastAssistant?.realTokenCount ?: lastAssistant?.let {
            tokenCounter.estimateTokens(it.content)
        }
    }

    override suspend fun getCurrentQueryTokens(): Int {
        val history = localRepository.getMessageHistory()
        val lastUser = history.lastOrNull { it.role == "user" }
        return lastUser?.let {
            it.realTokenCount ?: tokenCounter.estimateTokens(it.content)
        } ?: 0
    }

    override suspend fun clearHistory() {
        localRepository.clearHistory(keepSystemPrompt = true)
        summaryDao.deleteAllSummaries(localRepository.provideSessionId())
        contextStrategyManager.clear()
        Log.d(TAG, "🧹 История очищена (системный промпт сохранен)")
    }

    override suspend fun clearAll() {
        localRepository.clearHistory(keepSystemPrompt = false)
        summaryDao.deleteAllSummaries(localRepository.provideSessionId())
        contextStrategyManager.clear()
        setSystemPrompt("Ты полезный ассистент. Отвечай кратко и по делу на русском языке.")
        Log.d(TAG, "🧹 Полная очистка выполнена")
    }

    override suspend fun setSystemPrompt(prompt: String) {
        localRepository.setSystemPrompt(prompt)
        Log.d(TAG, "⚙️ Системный промпт обновлен: $prompt")
    }

    override suspend fun getSystemPrompt(): String? = localRepository.getSystemPrompt()

    // ========== Методы для работы с настройками пользователя ==========

    /**
     * Получает настройки пользователя
     */
    suspend fun getUserSettings(): UserSettings? {
        val style = localRepository.getUserSettingsStyle()
        val format = localRepository.getUserSettingsFormat()
        val constraints = localRepository.getUserSettingsConstraints()

        return if (style != null || format != null || constraints != null) {
            UserSettings(
                style = style ?: "",
                responseFormat = format ?: "",
                constraints = constraints ?: ""
            )
        } else {
            null
        }
    }

    /**
     * Сохраняет настройки пользователя
     */
    suspend fun saveUserSettings(settings: UserSettings) {
        localRepository.saveUserSettingsStyle(settings.style)
        localRepository.saveUserSettingsFormat(settings.responseFormat)
        localRepository.saveUserSettingsConstraints(settings.constraints)
        Log.d(TAG, "💾 Настройки пользователя сохранены: стиль=${settings.style}, формат=${settings.responseFormat}, ограничения=${settings.constraints}")
    }

    /**
     * Получает базовый системный промпт без настроек пользователя
     */
    private suspend fun getBaseSystemPrompt(): String {
        val currentPrompt = localRepository.getSystemPrompt() ?: "Ты полезный ассистент. Отвечай кратко и по делу на русском языке."
        // Удаляем старые настройки из промпта, если они есть
        val settingsMarker = "\n=== ПЕРСОНАЛИЗАЦИЯ ОТВЕТОВ ==="
        return if (currentPrompt.contains(settingsMarker)) {
            currentPrompt.substringBefore(settingsMarker)
        } else {
            currentPrompt
        }
    }

    /**
     * Получает системный промпт с добавленными настройками пользователя
     */
    private suspend fun getSystemPromptWithSettings(): String {
        val basePrompt = getBaseSystemPrompt()
        val settings = getUserSettings()

        return if (settings != null && settings.isFilled()) {
            basePrompt + settings.toSystemPromptString()
        } else {
            basePrompt
        }
    }

    override fun getCurrentAgentInfo(): String = currentType.name

    override fun getCurrentRepositoryType(): RepositoryType = currentType

    override suspend fun switchRepository(type: RepositoryType) {
        Log.d(TAG, "🔄 Смена репозитория: ${currentType} -> $type")
        currentType = type
        clearAll()
    }

    override suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        currentHuggingFaceModel = modelType
        if (currentType == RepositoryType.HUGGINGFACE) {
            clearAll()
        }
        Log.d(TAG, "🤗 Модель HuggingFace: ${modelType.displayName}")
    }

    override fun getCurrentHuggingFaceModel(): HuggingFaceModel? = currentHuggingFaceModel

    override fun getTokenCounter(): TokenCounter = tokenCounter

    // ========== Методы для работы со стратегиями контекста ==========

    override suspend fun setContextStrategy(strategy: ContextStrategy) {
        contextStrategyManager.setStrategy(strategy)
        Log.d(TAG, "🎯 Стратегия контекста изменена: ${strategy.name}")
    }

    override fun getCurrentContextStrategy(): ContextStrategy {
        return contextStrategyManager.getCurrentStrategy()
    }

    override fun getCurrentFacts(): Map<String, DialogFact> {
        return contextStrategyManager.getCurrentFacts()
    }

    override fun getBranches(): List<DialogBranch> {
        return contextStrategyManager.getBranches()
    }

    override fun getCurrentBranchId(): String? {
        return contextStrategyManager.getCurrentBranchId()
    }

    override suspend fun createBranch(checkpointMessageId: String, branchName: String): Boolean {
        return contextStrategyManager.createBranch(checkpointMessageId, branchName)
    }

    override suspend fun switchBranch(branchId: String): Boolean {
        return contextStrategyManager.switchBranch(branchId)
    }

    override suspend fun deleteBranch(branchId: String): Boolean {
        return contextStrategyManager.deleteBranch(branchId)
    }

    override suspend fun getMessages(): List<ChatMessage> {
        // Возвращаем сообщения в зависимости от стратегии
        return when (val strategy = contextStrategyManager.getCurrentStrategy()) {
            is ContextStrategy.Branching -> {
                val branchId = contextStrategyManager.getCurrentBranchId()
                if (branchId != null) {
                    contextStrategyManager.getBranchMessages(branchId)
                } else {
                    localRepository.getMessageHistory()
                }
            }
            else -> localRepository.getMessageHistory()
        }
    }

    // ========== Методы для изучения языков ==========

    override fun saveLanguageToLearn(language: String?) {
        localRepository.saveLanguageToLearn(language)
        Log.d(TAG, "💾 Сохранен язык для изучения: $language")
    }

    override fun getLanguageToLearn(): String? {
        return localRepository.getLanguageToLearn()
    }

    override fun saveLearningGoal(goal: String?) {
        localRepository.saveLearningGoal(goal)
        Log.d(TAG, "💾 Сохранена цель обучения: $goal")
    }

    override fun getLearningGoal(): String? {
        return localRepository.getLearningGoal()
    }

    override fun saveCurrentLevel(level: String?) {
        localRepository.saveCurrentLevel(level)
        Log.d(TAG, "💾 Сохранен текущий уровень: $level")
    }

    override fun getCurrentLevel(): String? {
        return localRepository.getCurrentLevel()
    }

    override fun saveLessonsCompleted(count: Int) {
        localRepository.saveLessonsCompleted(count)
        Log.d(TAG, "💾 Сохранено количество уроков: $count")
    }

    override fun getLessonsCompleted(): Int {
        return localRepository.getLessonsCompleted()
    }

    override fun saveExercisesCompleted(count: Int) {
        localRepository.saveExercisesCompleted(count)
        Log.d(TAG, "💾 Сохранено количество упражнений: $count")
    }

    override fun getExercisesCompleted(): Int {
        return localRepository.getExercisesCompleted()
    }

    override fun saveCorrectAnswers(count: Int) {
        localRepository.saveCorrectAnswers(count)
        Log.d(TAG, "💾 Сохранено количество правильных ответов: $count")
    }

    override fun getCorrectAnswers(): Int {
        return localRepository.getCorrectAnswers()
    }

    override fun saveTotalAnswers(count: Int) {
        localRepository.saveTotalAnswers(count)
        Log.d(TAG, "💾 Сохранено общее количество ответов: $count")
    }

    override fun getTotalAnswers(): Int {
        return localRepository.getTotalAnswers()
    }

    override fun saveStreakDays(days: Int) {
        localRepository.saveStreakDays(days)
        Log.d(TAG, "💾 Сохранено количество дней подряд: $days")
    }

    override fun getStreakDays(): Int {
        return localRepository.getStreakDays()
    }

    override fun clearLanguageLearningData() {
        localRepository.saveLanguageToLearn(null)
        localRepository.saveLearningGoal(null)
        localRepository.saveCurrentLevel(null)
        localRepository.saveLessonsCompleted(0)
        localRepository.saveExercisesCompleted(0)
        localRepository.saveCorrectAnswers(0)
        localRepository.saveTotalAnswers(0)
        localRepository.saveStreakDays(0)
        Log.d(TAG, "🗑️ Данные об изучении языков очищены")
    }

    // ========== Методы для работы с LanguageMemoryManager ==========

    override suspend fun getUserProfile(): UserProfile? {
        return languageMemoryManager.getUserProfile()
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        languageMemoryManager.saveUserProfile(profile)
        Log.d(TAG, "💾 Профиль пользователя сохранен")
    }

    override suspend fun getLearningStats(): LearningStats? {
        val sessionId = localRepository.provideSessionId()
        return languageMemoryManager.getLearningStats(sessionId)
    }

    override suspend fun buildMemoryPrompt(basePrompt: String): String {
        val sessionId = localRepository.provideSessionId()
        return languageMemoryManager.buildPrompt(sessionId, basePrompt)
    }

    override suspend fun clearShortTermMemory() {
        val sessionId = localRepository.provideSessionId()
        languageMemoryManager.clearShortTermMemory(sessionId)
    }

    override suspend fun clearWorkingMemory() {
        val sessionId = localRepository.provideSessionId()
        languageMemoryManager.clearWorkingMemory(sessionId)
    }
}