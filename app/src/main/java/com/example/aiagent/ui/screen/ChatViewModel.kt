// ui/screen/ChatViewModel.kt
package com.example.aiagent.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.agent.UniversalAgentImpl
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
        // НЕ загружаем историю сообщений из БД в UI
        // Загружаем только настройки и статистику
        _state.update {
            it.copy(
                currentRepositoryType = universalAgent.getCurrentRepositoryType(),
                huggingFaceModel = universalAgent.getCurrentHuggingFaceModel() ?: HuggingFaceModel.MEDIUM
            )
        }

        // Загружаем статистику токенов (опционально, для отображения в UI)
        refreshTokenStats()
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
        }
    }

    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun updateTemperature(temperature: Double) {
        _state.update { it.copy(temperature = temperature) }
    }

    private fun newChat() {
        viewModelScope.launch {
            universalAgent.clearHistory()
            _state.update {
                it.copy(
                    messages = emptyList(), // Очищаем UI сообщения
                    error = null,
                    lastResponse = null,
                    tokenStats = TokenStats(),
                    showTokenDetails = false
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
                    messages = emptyList(), // Очищаем UI сообщения
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
                    messages = emptyList(), // Очищаем UI сообщения
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

                // Обновляем статистику токенов
                refreshTokenStats()

                val agentMessage = Message.AgentMessage(
                    id = System.currentTimeMillis().toString(),
                    content = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount,
                    promptTokens = response.promptTokens,
                    totalHistoryTokens = response.totalHistoryTokens
                )

                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + agentMessage,
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