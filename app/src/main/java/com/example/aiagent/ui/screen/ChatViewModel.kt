// ui/screen/ChatViewModel.kt
package com.example.aiagent.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.huggingFace.ChatMessage
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val universalAgent: UniversalAgent
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
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
                messages = emptyList() // Явно устанавливаем пустой список сообщений для UI
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
        }
    }

    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
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
                    currentBranchId = null
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
                        else -> it.slidingWindowSize
                    }
                )
            }
            // Обновляем факты и ветки после смены стратегии
            updateFactsAndBranches()
            // Перезагружаем сообщения (для Branching может измениться набор)
            loadMessages()
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
                loadMessages()
            }
        }
    }

    private fun switchBranch(branchId: String) {
        viewModelScope.launch {
            universalAgent.switchBranch(branchId)
            // Загружаем сообщения выбранной ветки
            loadMessages()
            updateFactsAndBranches()
        }
    }

    private fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            universalAgent.deleteBranch(branchId)
            updateFactsAndBranches()
            loadMessages()
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

                // Перезагружаем сообщения, но фильтруем системные
                val allMessages = universalAgent.getMessages()
                val uiMessages = allMessages.mapNotNull { chatMessage ->
                    convertToUIMessage(chatMessage)
                }

                _state.update { currentState ->
                    currentState.copy(
                        messages = uiMessages, // Обновляем сразу все сообщения
                        isLoading = false,
                        lastResponse = LastResponseInfo(
                            timeMs = response.responseTimeMs,
                            tokenCount = response.tokenCount,
                            promptTokens = response.promptTokens,
                            tokensPerSecond = if (response.responseTimeMs > 0) {
                                response.tokenCount / (response.responseTimeMs / 1000.0)
                            } else null
                        )
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
}