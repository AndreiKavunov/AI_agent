// ui/screen/ChatViewModel.kt
package com.example.aiagent.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val universalAgent: UniversalAgent  // Получаем агента через конструктор
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                currentRepositoryType = universalAgent.getCurrentRepositoryType(),
                huggingFaceModel = universalAgent.getCurrentHuggingFaceModel() ?: HuggingFaceModel.MEDIUM
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
        universalAgent.clearHistory()
        _state.update {
            it.copy(
                messages = emptyList(),
                error = null,
                lastResponseTime = null,
                lastTokenCount = null
            )
        }
    }

    private fun switchRepository(repositoryType: RepositoryType) {
        universalAgent.switchRepository(repositoryType)

        _state.update {
            it.copy(
                currentRepositoryType = repositoryType,
                messages = emptyList(),
                error = null,
                lastResponseTime = null,
                lastTokenCount = null,
                huggingFaceModel = if (repositoryType == RepositoryType.HUGGINGFACE) {
                    universalAgent.getCurrentHuggingFaceModel() ?: HuggingFaceModel.MEDIUM
                } else {
                    it.huggingFaceModel
                }
            )
        }
    }

    private fun selectHuggingFaceModel(modelType: HuggingFaceModel) {
        universalAgent.setHuggingFaceModel(modelType)

        _state.update {
            it.copy(
                huggingFaceModel = modelType,
                messages = emptyList(), // Очищаем UI историю при смене модели
                lastResponseTime = null,
                lastTokenCount = null
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
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val response = universalAgent.processMessage(
                    message = text.trim(),
                    temperature = _state.value.temperature
                )

                val agentMessage = Message.AgentMessage(
                    id = System.currentTimeMillis().toString(),
                    content = response.text,
                    toolUsed = response.toolUsed,
                    responseTimeMs = response.responseTimeMs,
                    tokenCount = response.tokenCount
                )

                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + agentMessage,
                        isLoading = false,
                        lastResponseTime = response.responseTimeMs,
                        lastTokenCount = response.tokenCount,
                        lastTokensPerSecond = if (response.responseTimeMs > 0) {
                            response.tokenCount / (response.responseTimeMs / 1000.0)
                        } else null
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