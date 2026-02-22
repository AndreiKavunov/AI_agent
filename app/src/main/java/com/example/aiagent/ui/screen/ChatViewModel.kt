package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.di.AppModule
import com.example.aiagent.domain.ChatRepository
import com.example.aiagent.domain.RepositoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    // Убираем параметр chatRepository из конструктора
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    // Получаем репозиторий через AppModule каждый раз при обращении
    private val chatRepository: ChatRepository
        get() = AppModule.chatRepository

    init {
        _state.update {
            it.copy(
                currentRepositoryType = AppModule.getCurrentRepositoryType()
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
        viewModelScope.launch {
            chatRepository.clearChatHistory()
            _state.update { it.copy(messages = emptyList(), error = null) }
        }
    }

    private fun switchRepository(repositoryType: RepositoryType) {
        // Переключаем репозиторий через AppModule
        AppModule.switchRepository(repositoryType)

        _state.update {
            it.copy(
                currentRepositoryType = repositoryType,
                messages = emptyList(),
                error = null
            )
        }
    }

    private fun selectHuggingFaceModel(modelType: HuggingFaceModel) {
        AppModule.huggingFaceRepository?.setHuggingFaceModel(modelType)

        _state.update {
            it.copy(
                huggingFaceModel = modelType
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
                // Используем актуальный репозиторий
                val response = chatRepository.sendMessage(
                    message = text.trim(),
                    temperature = _state.value.temperature
                )

                val agentMessage = Message.AgentMessage(
                    id = System.currentTimeMillis().toString(),
                    content = response.text,
                    toolUsed = response.toolUsed
                )

                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + agentMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(
                        error = "Ошибка (${_state.value.currentRepositoryType}): ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}