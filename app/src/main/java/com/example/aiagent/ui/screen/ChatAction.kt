package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType

sealed interface ChatAction {
    data class SendMessage(val text: String) : ChatAction
    data class UpdateInput(val text: String) : ChatAction
    data class UpdateTemperature(val temperature: Double) : ChatAction
    object ClearError : ChatAction
    object NewChat : ChatAction
    data class SwitchRepository(val repositoryType: RepositoryType) : ChatAction
    data class SelectHuggingFaceModel(val modelType: HuggingFaceModel) : ChatAction
}