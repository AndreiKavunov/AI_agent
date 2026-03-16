// ui/screen/ChatViewModel.kt
package com.example.aiagent.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.rag.RagStrategy
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.agent.UserSettings
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.domain.workflow.WorkflowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class ChatViewModel(
    private val universalAgent: UniversalAgent
) : ViewModel() {

    val TAG = "ChatViewModel"

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val workflowManager get() = (universalAgent as? com.example.aiagent.domain.agent.UniversalAgentImpl)?.getWorkflowManager() ?: WorkflowManager()

    init {
        // Устанавливаем callback для уведомления о повторной попытке
        (universalAgent as? com.example.aiagent.domain.agent.UniversalAgentImpl)?.onResponseRetry = { currentLength, maxLength ->
            _state.update {
                it.copy(
                    toastMessage = "Ответ слишком длинный ($currentLength символов). Повторная попытка сократить до $maxLength символов..."
                )
            }
        }
        
        viewModelScope.launch {
            loadInitialData()
        }
    }


    private suspend fun loadInitialData() {
        // Загружаем настройки из агента
        _state.update {
            it.copy(
                currentRepositoryType = universalAgent.getCurrentRepositoryType(),
                huggingFaceModel = universalAgent.getCurrentHuggingFaceModel() ?: HuggingFaceModel.MEDIUM,
                currentContextStrategy = universalAgent.getCurrentContextStrategy(),
                slidingWindowSize = when (val strategy = universalAgent.getCurrentContextStrategy()) {
                    is ContextStrategy.SlidingWindow -> strategy.maxMessages
                    is ContextStrategy.StickyFacts -> strategy.maxMessages
                    else -> 10
                },
                facts = universalAgent.getCurrentFacts(),
                branches = universalAgent.getBranches(),
                currentBranchId = universalAgent.getCurrentBranchId(),
                messages = emptyList(), // Явно устанавливаем пустой список сообщений для UI
                // Загружаем данные для изучения языков
                languageToLearn = universalAgent.getLanguageToLearn(),
                learningGoal = universalAgent.getLearningGoal(),
                currentLevel = universalAgent.getCurrentLevel(),
                lessonsCompleted = universalAgent.getLessonsCompleted(),
                exercisesCompleted = universalAgent.getExercisesCompleted(),
                correctAnswers = universalAgent.getCorrectAnswers(),
                totalAnswers = universalAgent.getTotalAnswers(),
                streakDays = universalAgent.getStreakDays(),
                // Загружаем настройки пользователя
                userSettings = (universalAgent as? com.example.aiagent.domain.agent.UniversalAgentImpl)?.getUserSettings() ?: UserSettings.createDefault(),
                // Загружаем состояние workflow
                workflowState = workflowManager.getCurrentWorkflow(),
                // Загружаем RAG настройки
                ragEnabled = universalAgent.isRagEnabled(),
                ragStrategy = universalAgent.getRagStrategy(),
                lastRagContext = universalAgent.getLastRagContext()
            )
        }

        // НЕ загружаем историю сообщений из БД в UI
        // loadMessages() - удаляем эту строку

        // Загружаем только статистику токенов (опционально)
        refreshTokenStats()
    }

    /**
     * Конвертирует ChatMessage из data слоя в Message для UI слоя
     * Системные сообщения (role = "system") не отображаются в UI
     */
    private fun convertToUIMessage(chatMessage: ChatMessage): Message? {
        return when (chatMessage.role) {
            "user" -> Message.UserMessage(
                id = chatMessage.id,
                content = chatMessage.content
            )
            "assistant" -> Message.AgentMessage(
                id = chatMessage.id,
                content = chatMessage.content,
                tokenCount = chatMessage.realTokenCount ?: 0
            )
            "system" -> null // Системные сообщения не показываем в UI
            else -> null
        }
    }

    private suspend fun loadMessages() {
        val chatMessages = universalAgent.getMessages()
        val uiMessages = chatMessages.mapNotNull { chatMessage ->
            convertToUIMessage(chatMessage)
        }
        _state.update { it.copy(messages = uiMessages) }
    }

    private suspend fun refreshTokenStats() {
        val tokenStats = universalAgent.getTotalHistoryTokens()
        val lastResponseTokens = universalAgent.getLastResponseTokens()
        val currentQueryTokens = universalAgent.getCurrentQueryTokens()

        _state.update {
            it.copy(
                tokenStats = TokenStats(
                    totalTokens = tokenStats.totalTokens,
                    systemTokens = tokenStats.systemTokens,
                    userTokens = tokenStats.userTokens,
                    assistantTokens = tokenStats.assistantTokens,
                    lastResponseTokens = lastResponseTokens,
                    currentQueryTokens = currentQueryTokens
                )
            )
        }
    }

    fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendMessage -> sendMessage(action.text)
            is ChatAction.UpdateInput -> updateInput(action.text)
            is ChatAction.UpdateTemperature -> updateTemperature(action.temperature)
            is ChatAction.ClearError -> clearError()
            is ChatAction.NewChat -> newChat()
            is ChatAction.SwitchRepository -> switchRepository(action.repositoryType)
            is ChatAction.SelectHuggingFaceModel -> selectHuggingFaceModel(action.modelType)
            is ChatAction.ShowTokenDetails -> showTokenDetails()
            is ChatAction.HideTokenDetails -> hideTokenDetails()

            // Действия для стратегий контекста
            is ChatAction.SelectContextStrategy -> selectContextStrategy(action.strategy)
            is ChatAction.ShowContextSettings -> showContextSettings()
            is ChatAction.HideContextSettings -> hideContextSettings()
            is ChatAction.CreateBranch -> createBranch(action.checkpointMessageId, action.branchName)
            is ChatAction.SwitchBranch -> switchBranch(action.branchId)
            is ChatAction.DeleteBranch -> deleteBranch(action.branchId)
            is ChatAction.UpdateSlidingWindowSize -> updateSlidingWindowSize(action.size)

            // Действия для изучения языков
            is ChatAction.UpdateLanguageToLearn -> updateLanguageToLearn(action.language)
            is ChatAction.UpdateLearningGoal -> updateLearningGoal(action.goal)
            is ChatAction.UpdateCurrentLevel -> updateCurrentLevel(action.level)
            is ChatAction.IncrementLessonsCompleted -> incrementLessonsCompleted()
            is ChatAction.IncrementExercisesCompleted -> incrementExercisesCompleted()
            is ChatAction.AddCorrectAnswer -> addCorrectAnswer(action.memoryId)
            is ChatAction.AddIncorrectAnswer -> addIncorrectAnswer(action.memoryId)

            // Действия для настроек пользователя
            is ChatAction.ShowProfileDialog -> showProfileDialog()
            is ChatAction.HideProfileDialog -> hideProfileDialog()
            is ChatAction.UpdateUserSettings -> updateUserSettings(action.settings)
            is ChatAction.ClearToast -> clearToast()

            // Действия для workflow (рабочего процесса)
            is ChatAction.StartWorkflow -> startWorkflow()
            is ChatAction.AdvanceWorkflow -> advanceWorkflow()
            is ChatAction.RetreatWorkflow -> retreatWorkflow()
            is ChatAction.ResetWorkflow -> resetWorkflow()
        }
    }
    
    // Методы для изучения языков
    private fun updateLanguageToLearn(language: String) {
        universalAgent.saveLanguageToLearn(language.ifBlank { null })
        _state.update { it.copy(languageToLearn = language.ifBlank { null }) }
    }
    
    private fun updateLearningGoal(goal: String) {
        universalAgent.saveLearningGoal(goal.ifBlank { null })
        _state.update { it.copy(learningGoal = goal.ifBlank { null }) }
    }
    
    private fun updateCurrentLevel(level: String) {
        universalAgent.saveCurrentLevel(level.ifBlank { null })
        _state.update { it.copy(currentLevel = level.ifBlank { null }) }
    }

    private fun incrementLessonsCompleted() {
        viewModelScope.launch {
            val newCount = _state.value.lessonsCompleted + 1
            universalAgent.saveLessonsCompleted(newCount)
            _state.update { it.copy(lessonsCompleted = newCount) }
        }
    }
    
    private fun incrementExercisesCompleted() {
        viewModelScope.launch {
            val newCount = _state.value.exercisesCompleted + 1
            universalAgent.saveExercisesCompleted(newCount)
            _state.update { it.copy(exercisesCompleted = newCount) }
        }
    }
    
    private fun addCorrectAnswer(memoryId: String) {
        viewModelScope.launch {
            val newCorrect = _state.value.correctAnswers + 1
            val newTotal = _state.value.totalAnswers + 1
            universalAgent.saveCorrectAnswers(newCorrect)
            universalAgent.saveTotalAnswers(newTotal)
            _state.update { 
                it.copy(
                    correctAnswers = newCorrect,
                    totalAnswers = newTotal
                )
            }
        }
    }
    
    private fun addIncorrectAnswer(memoryId: String) {
        viewModelScope.launch {
            val newTotal = _state.value.totalAnswers + 1
            universalAgent.saveTotalAnswers(newTotal)
            _state.update {
                it.copy(
                    totalAnswers = newTotal
                )
            }
        }
    }

    // Методы для настроек пользователя
    private fun showProfileDialog() {
        _state.update { it.copy(showProfileDialog = true) }
    }

    private fun hideProfileDialog() {
        _state.update { it.copy(showProfileDialog = false) }
    }

    private fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            (universalAgent as? com.example.aiagent.domain.agent.UniversalAgentImpl)?.saveUserSettings(settings)
            _state.update {
                it.copy(
                    userSettings = settings,
                    showProfileDialog = false
                )
            }
        }
    }

    // ========== Методы для workflow (рабочего процесса) ==========

    private fun startWorkflow() {
        val lastUserMessage = _state.value.messages.lastOrNull { it is Message.UserMessage }
        
        if (lastUserMessage == null) {
            // Если нет сообщений, запускаем workflow без запроса - он будет ждать первого сообщения
            val newWorkflow = workflowManager.startWorkflow("")
            _state.update { it.copy(workflowState = newWorkflow) }
            Log.d(TAG, "🚀 Workflow запущен (ожидание первого сообщения): ${newWorkflow.currentStage.displayName}")
        } else {
            val newWorkflow = workflowManager.startWorkflow(lastUserMessage.content)
            _state.update { it.copy(workflowState = newWorkflow) }
            Log.d(TAG, "🚀 Workflow запущен: ${newWorkflow.currentStage.displayName}")
        }
    }

    private fun advanceWorkflow() {
        val nextWorkflow = workflowManager.advanceToNextStage()
        if (nextWorkflow != null) {
            _state.update { it.copy(workflowState = nextWorkflow) }
            Log.d(TAG, "➡️ Переход к этапу: ${nextWorkflow.currentStage.displayName}")
        } else {
            Log.w(TAG, "⚠️ Не удалось перейти к следующему этапу")
        }
    }

    private fun retreatWorkflow() {
        val previousWorkflow = workflowManager.retreatToPreviousStage()
        if (previousWorkflow != null) {
            _state.update { it.copy(workflowState = previousWorkflow) }
            Log.d(TAG, "⬅️ Возврат к этапу: ${previousWorkflow.currentStage.displayName}")
        } else {
            Log.w(TAG, "⚠️ Не удалось вернуться к предыдущему этапу")
        }
    }

    private fun resetWorkflow() {
        val newWorkflow = workflowManager.resetWorkflow()
        _state.update { it.copy(workflowState = newWorkflow) }
        Log.d(TAG, "🔄 Workflow сброшен")
    }

    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    private fun updateTemperature(temperature: Double) {
        viewModelScope.launch {
            universalAgent.setTemperature(temperature)
            _state.update { it.copy(temperature = temperature) }
        }
    }

    private fun newChat() {
        viewModelScope.launch {
            universalAgent.clearHistory() // Очищает историю в БД
            universalAgent.clearLanguageLearningData() // Очищаем данные об изучении языков
            resetWorkflow() // Сбрасываем workflow
            _state.update {
                it.copy(
                    messages = emptyList(), // Очищаем UI сообщения
                    error = null,
                    lastResponse = null,
                    tokenStats = TokenStats(),
                    showTokenDetails = false,
                    showContextSettings = false,
                    facts = emptyMap(),
                    branches = emptyList(),
                    currentBranchId = null,
                    // Очищаем данные об изучении языков в UI
                    languageToLearn = null,
                    learningGoal = null,
                    currentLevel = null,
                    lessonsCompleted = 0,
                    exercisesCompleted = 0,
                    correctAnswers = 0,
                    totalAnswers = 0,
                    streakDays = 0
                )
            }
        }
    }

    private fun switchRepository(repositoryType: RepositoryType) {
        viewModelScope.launch {
            universalAgent.switchRepository(repositoryType)

            _state.update {
                it.copy(
                    currentRepositoryType = repositoryType,
                    messages = emptyList(),
                    error = null,
                    lastResponse = null,
                    tokenStats = TokenStats(),
                    showTokenDetails = false,
                    huggingFaceModel = if (repositoryType == RepositoryType.HUGGINGFACE) {
                        universalAgent.getCurrentHuggingFaceModel() ?: HuggingFaceModel.MEDIUM
                    } else {
                        it.huggingFaceModel
                    }
                )
            }
        }
    }

    private fun selectHuggingFaceModel(modelType: HuggingFaceModel) {
        viewModelScope.launch {
            universalAgent.setHuggingFaceModel(modelType)

            _state.update {
                it.copy(
                    huggingFaceModel = modelType,
                    messages = emptyList(),
                    lastResponse = null,
                    tokenStats = TokenStats(),
                    showTokenDetails = false
                )
            }
        }
    }

    private fun showTokenDetails() {
        viewModelScope.launch {
            refreshTokenStats()
            _state.update { it.copy(showTokenDetails = true) }
        }
    }

    private fun hideTokenDetails() {
        _state.update { it.copy(showTokenDetails = false) }
    }

    private fun selectContextStrategy(strategy: ContextStrategy) {
        viewModelScope.launch {
            universalAgent.setContextStrategy(strategy)
            _state.update {
                it.copy(
                    currentContextStrategy = strategy,
                    showContextSettings = false,
                    slidingWindowSize = when (strategy) {
                        is ContextStrategy.SlidingWindow -> strategy.maxMessages
                        is ContextStrategy.StickyFacts -> strategy.maxMessages
                        is ContextStrategy.Workflow -> strategy.maxMessages
                        else -> it.slidingWindowSize
                    }
                )
            }
            // Обновляем факты и ветки после смены стратегии
            updateFactsAndBranches()
            // Если выбрана стратегия Workflow, запускаем workflow
            if (strategy is ContextStrategy.Workflow) {
                startWorkflow()
            }
            // НЕ перезагружаем сообщения для UI - показываем только сообщения текущей сессии
        }
    }

    private fun showContextSettings() {
        _state.update { it.copy(showContextSettings = true) }
    }

    private fun hideContextSettings() {
        _state.update { it.copy(showContextSettings = false) }
    }

    private fun updateSlidingWindowSize(size: Int) {
        if (size in 1..50) {
            viewModelScope.launch {
                val currentStrategy = _state.value.currentContextStrategy
                val newStrategy = when (currentStrategy) {
                    is ContextStrategy.SlidingWindow -> currentStrategy.copy(maxMessages = size)
                    is ContextStrategy.StickyFacts -> currentStrategy.copy(maxMessages = size)
                    else -> currentStrategy
                }
                universalAgent.setContextStrategy(newStrategy)
                _state.update {
                    it.copy(
                        currentContextStrategy = newStrategy,
                        slidingWindowSize = size
                    )
                }
            }
        }
    }

    private fun createBranch(checkpointMessageId: String, branchName: String) {
        if (branchName.isBlank()) return

        viewModelScope.launch {
            val success = universalAgent.createBranch(checkpointMessageId, branchName)
            if (success) {
                // Обновляем список веток и переключаемся на новую ветку
                updateFactsAndBranches()
                // НЕ загружаем сообщения для UI - показываем только сообщения текущей сессии
            }
        }
    }

    private fun switchBranch(branchId: String) {
        viewModelScope.launch {
            universalAgent.switchBranch(branchId)
            // НЕ загружаем сообщения для UI - показываем только сообщения текущей сессии
            updateFactsAndBranches()
        }
    }

    private fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            universalAgent.deleteBranch(branchId)
            updateFactsAndBranches()
            // НЕ загружаем сообщения для UI - показываем только сообщения текущей сессии
        }
    }

    private suspend fun updateFactsAndBranches() {
        _state.update {
            it.copy(
                facts = universalAgent.getCurrentFacts(),
                branches = universalAgent.getBranches(),
                currentBranchId = universalAgent.getCurrentBranchId()
            )
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = Message.UserMessage(
            id = System.currentTimeMillis().toString(),
            content = text.trim()
        )

        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null,
                showTokenDetails = false
            )
        }

        viewModelScope.launch {
            try {
                val response = universalAgent.processMessage(
                    message = text.trim(),
                    temperature = _state.value.temperature
                )

                refreshTokenStats()
                updateFactsAndBranches()

                // Обновляем RAG контекст
                val ragContext = universalAgent.getLastRagContext()

                // Создаем сообщение ассистента для UI (только текущий ответ)
                val assistantMessage = Message.AgentMessage(
                    id = System.currentTimeMillis().toString(),
                    content = response.text,
                    tokenCount = response.tokenCount,
                    promptTokens = response.promptTokens,
                    responseTimeMs = response.responseTimeMs
                )

                // Сохраняем ответ в workflow, если он активен
                val updatedWorkflow = if (workflowManager.isWorkflowActive()) {
                    // Если оригинальный запрос пустой, обновляем его первым сообщением пользователя
                    if (_state.value.workflowState.originalRequest.isEmpty()) {
                        workflowManager.updateWorkflowRequest(text.trim())
                    } else {
                        workflowManager.saveStageResponse(response.text)
                    }
                } else {
                    _state.value.workflowState
                }

                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + assistantMessage, // Добавляем только новый ответ
                        isLoading = false,
                        lastResponse = LastResponseInfo(
                            timeMs = response.responseTimeMs,
                            tokenCount = response.tokenCount,
                            promptTokens = response.promptTokens,
                            tokensPerSecond = if (response.responseTimeMs > 0) {
                                response.tokenCount / (response.responseTimeMs / 1000.0)
                            } else null
                        ),
                        workflowState = updatedWorkflow,
                        lastRagContext = ragContext
                    )
                }
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(
                        error = "Ошибка (${universalAgent.getCurrentAgentInfo()}): ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    // ========== Методы для работы с RAG ==========

    /**
     * Переключает RAG
     */
    fun toggleRag() {
        viewModelScope.launch {
            val newState = !_state.value.ragEnabled
            universalAgent.setRagEnabled(newState)
            _state.update { it.copy(ragEnabled = newState) }
            Log.d(TAG, "📚 RAG ${if (newState) "включен" else "выключен"}")
        }
    }

    /**
     * Устанавливает стратегию RAG
     */
    fun setRagStrategy(strategy: RagStrategy) {
        viewModelScope.launch {
            universalAgent.setRagStrategy(strategy)
            _state.update { it.copy(ragStrategy = strategy) }
            Log.d(TAG, "📚 Установлена RAG стратегия: ${strategy.name}")
        }
    }

    /**
     * Показывает/скрывает настройки RAG
     */
    fun toggleRagSettings() {
        _state.update { it.copy(showRagSettings = !it.showRagSettings) }
    }

    /**
     * Строит RAG индекс с указанной стратегией
     */
    fun buildRagIndex(strategy: RagStrategy) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    isBuildingRagIndex = true,
                    ragIndexStatus = "Построение индекса..."
                )
            }
            
            val result = universalAgent.buildRagIndex(strategy)
            
            result.fold(
                onSuccess = { message ->
                    _state.update {
                        it.copy(
                            isBuildingRagIndex = false,
                            ragIndexStatus = message,
                            toastMessage = "✅ $message"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isBuildingRagIndex = false,
                            ragIndexStatus = "Ошибка: ${error.message}",
                            toastMessage = "❌ Ошибка: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Очищает статус RAG индекса
     */
    fun clearRagIndexStatus() {
        _state.update { it.copy(ragIndexStatus = null) }
    }
}