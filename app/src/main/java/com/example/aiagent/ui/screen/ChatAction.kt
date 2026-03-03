package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UserSettings
import com.example.aiagent.domain.contextStrategy.ContextStrategy

sealed interface ChatAction {
    data class SendMessage(val text: String) : ChatAction
    data class UpdateInput(val text: String) : ChatAction
    data class UpdateTemperature(val temperature: Double) : ChatAction
    object ClearError : ChatAction
    object NewChat : ChatAction
    data class SwitchRepository(val repositoryType: RepositoryType) : ChatAction
    data class SelectHuggingFaceModel(val modelType: HuggingFaceModel) : ChatAction
    object ShowTokenDetails : ChatAction
    object HideTokenDetails : ChatAction

    // Новые действия для стратегий контекста
    data class SelectContextStrategy(val strategy: ContextStrategy) : ChatAction
    object ShowContextSettings : ChatAction
    object HideContextSettings : ChatAction
    data class CreateBranch(val checkpointMessageId: String, val branchName: String) : ChatAction
    data class SwitchBranch(val branchId: String) : ChatAction
    data class DeleteBranch(val branchId: String) : ChatAction
    data class UpdateSlidingWindowSize(val size: Int) : ChatAction

    // Действия для изучения языков
    data class UpdateLanguageToLearn(val language: String) : ChatAction
    data class UpdateLearningGoal(val goal: String) : ChatAction
    data class UpdateCurrentLevel(val level: String) : ChatAction
    object IncrementLessonsCompleted : ChatAction
    object IncrementExercisesCompleted : ChatAction
    data class AddCorrectAnswer(val memoryId: String) : ChatAction
    data class AddIncorrectAnswer(val memoryId: String) : ChatAction

    // Действия для настроек пользователя
    object ShowProfileDialog : ChatAction
    object HideProfileDialog : ChatAction
    data class UpdateUserSettings(val settings: UserSettings) : ChatAction
}