package com.example.aiagent.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.domain.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendMessage -> sendMessage(action.text)
            is ChatAction.UpdateInput -> updateInput(action.text)
            is ChatAction.ClearError -> clearError()
        }
    }

    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
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
                error = null // Очищаем предыдущую ошибку
            )
        }

        viewModelScope.launch {
            try {
                // Получаем ответ от агента
                val response = chatRepository.sendMessage(text.trim())

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
                        error = "Ошибка: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}