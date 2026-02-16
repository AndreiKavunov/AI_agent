package com.example.aiagent.ui.screen

sealed interface ChatAction {
    data class SendMessage(val text: String) : ChatAction
    data class UpdateInput(val text: String) : ChatAction
    object ClearError : ChatAction
}