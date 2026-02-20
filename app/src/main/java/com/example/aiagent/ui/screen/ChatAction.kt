package com.example.aiagent.ui.screen

sealed interface ChatAction {
    data class SendMessage(val text: String) : ChatAction
    data class UpdateInput(val text: String) : ChatAction
    data class UpdateTemperature(val temperature: Double) : ChatAction // Добавлено новое действие
    object ClearError : ChatAction
    object NewChat : ChatAction
}