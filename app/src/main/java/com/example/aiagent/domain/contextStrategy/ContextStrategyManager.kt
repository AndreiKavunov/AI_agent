// domain/contextStrategy/ContextStrategyManager.kt
package com.example.aiagent.domain.contextStrategy

import android.util.Log
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.ui.screen.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.UUID

private const val TAG = "ContextStrategyManager"

class ContextStrategyManager(
    private val localRepository: MessageLocalRepository,
    private val factExtractor: FactExtractor  // Добавляем экстрактор
) {

    private val _currentStrategy = MutableStateFlow<ContextStrategy>(ContextStrategy.SlidingWindow())
    val currentStrategy: StateFlow<ContextStrategy> = _currentStrategy.asStateFlow()

    private val _facts = MutableStateFlow<Map<String, DialogFact>>(emptyMap())
    val facts: StateFlow<Map<String, DialogFact>> = _facts.asStateFlow()

    private val _branches = MutableStateFlow<List<DialogBranch>>(emptyList())
    val branches: StateFlow<List<DialogBranch>> = _branches.asStateFlow()

    private val _currentBranchId = MutableStateFlow<String?>(null)
    val currentBranchId: StateFlow<String?> = _currentBranchId.asStateFlow()

    // Хранилище сообщений по веткам
    private val branchMessages = mutableMapOf<String, MutableList<ChatMessage>>()

    // Кэш последних извлеченных фактов для избежания дублирования
    private val lastExtractedHash = mutableMapOf<String, Int>()

    fun setStrategy(strategy: ContextStrategy) {
        _currentStrategy.value = strategy
        Log.d(TAG, "Стратегия изменена на: ${strategy.name}")
    }

    fun getCurrentStrategy(): ContextStrategy = _currentStrategy.value

    /**
     * Подготавливает сообщения для отправки в API в соответствии с выбранной стратегией
     */
    suspend fun prepareMessagesForApi(messages: List<ChatMessage>): List<ChatMessage> {
        return when (val strategy = _currentStrategy.value) {
            is ContextStrategy.SlidingWindow -> prepareSlidingWindow(messages, strategy)
            is ContextStrategy.StickyFacts -> prepareStickyFacts(messages, strategy)
            is ContextStrategy.Branching -> prepareBranching(messages, strategy)
            is ContextStrategy.LanguageLearning -> prepareLanguageLearning(messages, strategy)
            is ContextStrategy.Workflow -> prepareWorkflow(messages, strategy)
        }
    }

    /**
     * Стратегия 1: Sliding Window
     */
    private suspend fun prepareSlidingWindow(
        messages: List<ChatMessage>,
        strategy: ContextStrategy.SlidingWindow
    ): List<ChatMessage> {
        val systemMessage = messages.firstOrNull { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }
        val lastMessages = nonSystemMessages.takeLast(strategy.maxMessages)

        Log.d(TAG, "Sliding Window: системное + ${lastMessages.size} последних сообщений из ${nonSystemMessages.size}")

        return buildList {
            systemMessage?.let { add(it) }
            addAll(lastMessages)
        }
    }

    /**
     * Стратегия 2: Sticky Facts с использованием LLM для извлечения фактов
     */
    private suspend fun prepareStickyFacts(
        messages: List<ChatMessage>,
        strategy: ContextStrategy.StickyFacts
    ): List<ChatMessage> {
        val systemMessage = messages.firstOrNull { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }
        val lastMessages = nonSystemMessages.takeLast(strategy.maxMessages)

        // Форматируем факты для системного промпта
        val factsText = if (_facts.value.isNotEmpty()) {
            buildString {
                appendLine("\n=== ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ ===")
                _facts.value.values
                    .distinctBy { it.key.replace(Regex("_\\d+$"), "") } // Группируем по базовому ключу
                    .forEach { fact ->
                        val key = fact.key.replace(Regex("_\\d+$"), "").replace("_", " ").capitalize()
                        appendLine("• $key: ${fact.value}")
                    }
            }
        } else {
            ""
        }

        // Создаем расширенное системное сообщение
        val enhancedSystemPrompt = buildString {
            systemMessage?.let { appendLine(it.content) }
            append(factsText)
        }

        val enhancedSystemMessage = ChatMessage(
            id = systemMessage?.id ?: "system_${System.currentTimeMillis()}",
            role = "system",
            content = enhancedSystemPrompt
        )

        Log.d(TAG, "Sticky Facts: системное сообщение с ${_facts.value.size} фактами + ${lastMessages.size} последних сообщений")

        return buildList {
            add(enhancedSystemMessage)
            addAll(lastMessages)
        }
    }

    /**
     * Стратегия 3: Branching
     */
// domain/contextStrategy/ContextStrategyManager.kt
// Исправленный метод prepareBranching

    /**
     * Стратегия 3: Branching
     * Использует ВСЕ сообщения из текущей ветки без ограничений
     */
    private suspend fun prepareBranching(
        messages: List<ChatMessage>,
        strategy: ContextStrategy.Branching
    ): List<ChatMessage> {
        val currentBranchId = _currentBranchId.value

        return if (currentBranchId != null) {
            // Берем сообщения из текущей ветки
            val branchMsg = branchMessages[currentBranchId]

            if (branchMsg != null) {
                Log.d(TAG, "Branching: используем ветку '$currentBranchId' со ВСЕМИ ${branchMsg.size} сообщениями (без ограничений)")
                // Убеждаемся, что системное сообщение первое
                val systemMessage = branchMsg.firstOrNull { it.role == "system" }
                val nonSystemMessages = branchMsg.filter { it.role != "system" }

                buildList {
                    systemMessage?.let { add(it) }
                    addAll(nonSystemMessages) // Все сообщения ветки, без takeLast
                }
            } else {
                Log.d(TAG, "Branching: ветка '$currentBranchId' не найдена в branchMessages, используем все сообщения")
                messages
            }
        } else {
            Log.d(TAG, "Branching: ветка не выбрана, используем все сообщения (${messages.size})")
            messages
        }
    }

    /**
     * Стратегия 4: Language Learning
     * Добавляет информацию об изучении языка в системный промпт
     */
    private suspend fun prepareLanguageLearning(
        messages: List<ChatMessage>,
        strategy: ContextStrategy.LanguageLearning
    ): List<ChatMessage> {
        val systemMessage = messages.firstOrNull { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }
        val lastMessages = nonSystemMessages.takeLast(strategy.maxMessages)

        // Получаем данные об изучении языка
        val languageToLearn = localRepository.getLanguageToLearn()
        val learningGoal = localRepository.getLearningGoal()
        val currentLevel = localRepository.getCurrentLevel()
        val lessonsCompleted = localRepository.getLessonsCompleted()
        val exercisesCompleted = localRepository.getExercisesCompleted()
        val correctAnswers = localRepository.getCorrectAnswers()
        val totalAnswers = localRepository.getTotalAnswers()

        // Проверяем, нужно ли спрашивать о языке и цели
        val needsSetup = languageToLearn == null || learningGoal == null

        // Формируем контекст обучения языка
        val learningContext = buildString {
            appendLine("\n=== КОНТЕКСТ ИЗУЧЕНИЯ ЯЗЫКА ===")
            appendLine("Режим: Обучение иностранному языку")

            if (needsSetup) {
                // Если язык или цель не заданы, просим пользователя ввести их
                appendLine("\n⚠️ ВАЖНО: Сначала нужно узнать цели пользователя!")
                appendLine("Если пользователь только начал изучение языка:")
                appendLine("1. Поздоровайся и представься как помощник по изучению языков")
                appendLine("2. Спроси: \"Какой язык вы хотите изучить?\"")
                appendLine("3. После ответа спроси: \"Какова ваша цель обучения? (например: разговор, перевод, чтение, грамматика, подготовка к экзамену)\"")
                appendLine("4. После получения обоих ответов, начни обучение")
                appendLine("5. НЕ начинай давать задания, пока не узнаешь язык и цель!")
            } else {
                // Если язык и цель заданы, добавляем данные в контекст
                appendLine("\n📊 ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:")
                appendLine("Изучаемый язык: $languageToLearn")
                appendLine("Цель обучения: $learningGoal")
                if (currentLevel != null) {
                    appendLine("Текущий уровень: $currentLevel")
                }
                if (lessonsCompleted > 0) {
                    appendLine("Пройдено уроков: $lessonsCompleted")
                }
                if (exercisesCompleted > 0) {
                    appendLine("Выполнено упражнений: $exercisesCompleted")
                }
                if (totalAnswers > 0) {
                    val accuracy = if (correctAnswers > 0) {
                        String.format("%.1f%%", (correctAnswers.toDouble() / totalAnswers) * 100)
                    } else {
                        "0%"
                    }
                    appendLine("Точность ответов: $accuracy ($correctAnswers из $totalAnswers)")
                }

                appendLine("\n📋 ИНСТРУКЦИИ ДЛЯ АССИСТЕНТА:")
                appendLine("1. Адаптируй ответы под уровень пользователя")
                appendLine("2. Исправляй ошибки грамматики и произношения")
                appendLine("3. Предлагай новые слова и выражения")
                appendLine("4. Практикуй диалог на изучаемом языке ($languageToLearn)")
                appendLine("5. Объясняй грамматические правила простым языком")
                appendLine("6. Отмечай прогресс и достижения")
                appendLine("7. Используй изучаемый язык ($languageToLearn) в примерах и упражнениях")
                appendLine("8. Давай практические задания, соответствующие цели: $learningGoal")
                appendLine("9. Если цель - перевод: давай тексты для перевода и проверяй результат")
                appendLine("10. Если цель - разговор: практикуй диалоговые ситуации")
                appendLine("11. Если цель - грамматика: объясняй правила и давай упражнения")
                appendLine("12. Если цель - чтение: предлагай тексты для чтения и обсуждения")
            }
        }

        // Создаем расширенное системное сообщение
        val enhancedSystemPrompt = buildString {
            systemMessage?.let { appendLine(it.content) }
            append(learningContext)
        }

        val enhancedSystemMessage = ChatMessage(
            id = systemMessage?.id ?: "system_${System.currentTimeMillis()}",
            role = "system",
            content = enhancedSystemPrompt
        )

        Log.d(TAG, "Language Learning: системное сообщение с контекстом обучения (язык: $languageToLearn, цель: $learningGoal, уровень: $currentLevel, нужно настройку: $needsSetup) + ${lastMessages.size} последних сообщений")

        return buildList {
            add(enhancedSystemMessage)
            addAll(lastMessages)
        }
    }

    /**
     * Стратегия 5: Workflow (пошаговый режим)
     */
    private suspend fun prepareWorkflow(
        messages: List<ChatMessage>,
        strategy: ContextStrategy.Workflow
    ): List<ChatMessage> {
        val systemMessage = messages.firstOrNull { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }
        val lastMessages = nonSystemMessages.takeLast(strategy.maxMessages)

        Log.d(TAG, "Workflow: системное + ${lastMessages.size} последних сообщений из ${nonSystemMessages.size}")

        return buildList {
            systemMessage?.let { add(it) }
            addAll(lastMessages)
        }
    }

    /**
     * Обновляет факты с использованием LLM
     */
    suspend fun updateFactsWithLLM(
        message: String,
        repositoryType: RepositoryType,
        modelName: String? = null
    ) {
        // Проверяем, не обрабатывали ли мы уже похожее сообщение
        val messageHash = message.hashCode()
        if (lastExtractedHash[message] == messageHash) {
            Log.d(TAG, "⏭️ Сообщение уже обработано, пропускаем")
            return
        }

        Log.d(TAG, "🔍 Извлечение фактов через LLM из: $message")

        val extractedFacts = factExtractor.extractFacts(message, repositoryType, modelName)

        if (!extractedFacts.isEmpty()) {
            val newFacts = extractedFacts.toDialogFacts()
            _facts.update { currentFacts ->
                val updated = currentFacts + newFacts
                Log.d(TAG, "📊 Добавлено ${newFacts.size} новых фактов, всего: ${updated.size}")
                updated
            }
            lastExtractedHash[message] = messageHash
        } else {
            Log.d(TAG, "❌ Фактов не найдено")
        }
    }

    /**
     * Старый метод для обратной совместимости (можно удалить позже)
     */
    suspend fun updateFactsFromMessage(message: String, isUser: Boolean) {
        // Оставляем пустым или удаляем
    }

    // ... остальные методы (createBranch, switchBranch, deleteBranch и т.д.) остаются без изменений

// domain/contextStrategy/ContextStrategyManager.kt
// Убедимся, что createBranch правильно сохраняет историю

    /**
     * Создает новую ветку от указанного сообщения
     */
    suspend fun createBranch(checkpointMessageId: String, branchName: String): Boolean {
        // Проверяем лимит веток
        val maxBranches = if (_currentStrategy.value is ContextStrategy.Branching) {
            (_currentStrategy.value as ContextStrategy.Branching).maxBranches
        } else {
            5
        }

        if (_branches.value.size >= maxBranches) {
            Log.e(TAG, "Достигнут лимит веток ($maxBranches)")
            return false
        }

        val branchId = UUID.randomUUID().toString()
        val allMessages = localRepository.getMessageHistory()

        // Находим индекс сообщения-чекпоинта
        val checkpointIndex = allMessages.indexOfFirst { it.id == checkpointMessageId }
        if (checkpointIndex == -1) {
            Log.e(TAG, "Сообщение-чекпоинт не найдено: $checkpointMessageId")
            return false
        }

        // Копируем ВСЕ сообщения до чекпоинта (включая его)
        val checkpointMessages = allMessages.take(checkpointIndex + 1).toMutableList()
        branchMessages[branchId] = checkpointMessages

        Log.d(TAG, "Branching: создана ветка '$branchName' с ${checkpointMessages.size} сообщениями от чекпоинта")

        val branch = DialogBranch(
            id = branchId,
            name = branchName,
            checkpointMessageId = checkpointMessageId,
            messages = checkpointMessages.map { msg ->
                // Конвертируем в UI Message
                when (msg.role) {
                    "user" -> Message.UserMessage(
                        id = msg.id,
                        content = msg.content,
                        timestamp = System.currentTimeMillis()
                    )
                    "assistant" -> Message.AgentMessage(
                        id = msg.id,
                        content = msg.content,
                        tokenCount = msg.realTokenCount ?: 0,
                        timestamp = System.currentTimeMillis()
                    )
                    else -> Message.SystemMessage(
                        id = msg.id,
                        content = msg.content,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        )

        _branches.update { branches -> branches + branch }

        // Автоматически переключаемся на новую ветку
        _currentBranchId.value = branchId

        Log.d(TAG, "✅ Создана ветка: $branchName (id: $branchId) от сообщения $checkpointMessageId")
        return true
    }

    suspend fun switchBranch(branchId: String): Boolean {
        return if (_branches.value.any { it.id == branchId }) {
            _currentBranchId.value = branchId
            Log.d(TAG, "Переключено на ветку: $branchId")
            true
        } else {
            Log.e(TAG, "Ветка не найдена: $branchId")
            false
        }
    }

    suspend fun deleteBranch(branchId: String): Boolean {
        val exists = _branches.value.any { it.id == branchId }
        if (exists) {
            _branches.update { branches -> branches.filter { it.id != branchId } }
            branchMessages.remove(branchId)

            if (_currentBranchId.value == branchId) {
                _currentBranchId.value = _branches.value.firstOrNull()?.id
            }

            Log.d(TAG, "Удалена ветка: $branchId")
        }
        return exists
    }

    fun getBranchMessages(branchId: String): List<ChatMessage> {
        return branchMessages[branchId] ?: emptyList()
    }

    suspend fun addMessageToCurrentBranch(message: ChatMessage) {
        val currentId = _currentBranchId.value
        if (currentId != null) {
            // Получаем или создаем список сообщений для ветки
            val branchList = branchMessages.getOrPut(currentId) { mutableListOf() }
            branchList.add(message)

            Log.d(TAG, "Branching: сообщение добавлено в ветку '$currentId'. Всего в ветке: ${branchList.size} сообщений")

            // Обновляем список сообщений в ветке для UI
            _branches.update { branches ->
                branches.map { branch ->
                    if (branch.id == currentId) {
                        branch.copy(
                            messages = branch.messages + Message.AgentMessage(
                                id = message.id,
                                content = message.content,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        branch
                    }
                }
            }
        }
    }

    fun getCurrentFacts(): Map<String, DialogFact> = _facts.value
    fun getBranches(): List<DialogBranch> = _branches.value
    fun getCurrentBranchId(): String? = _currentBranchId.value

    fun clear() {
        _facts.value = emptyMap()
        _branches.value = emptyList()
        branchMessages.clear()
        _currentBranchId.value = null
        lastExtractedHash.clear()
        Log.d(TAG, "Менеджер стратегий очищен")
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}