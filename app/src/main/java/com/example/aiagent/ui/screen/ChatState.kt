// ui/screen/ChatState.kt
package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UserSettings
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.domain.contextStrategy.DialogBranch
import com.example.aiagent.domain.contextStrategy.DialogFact
import com.example.aiagent.domain.workflow.WorkflowState

sealed interface Message {
    val id: String
    val content: String
    val timestamp: Long

    data class UserMessage(
        override val id: String,
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message

    data class AgentMessage(
        override val id: String,
        override val content: String,
        val toolUsed: String? = null,
        val responseTimeMs: Long = 0,
        val tokenCount: Int = 0,
        val promptTokens: Int? = null,
        val totalHistoryTokens: Int? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message

    data class SystemMessage(
        override val id: String,
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message
}

data class TokenStats(
    val totalTokens: Int = 0,
    val systemTokens: Int = 0,
    val userTokens: Int = 0,
    val assistantTokens: Int = 0,
    val lastResponseTokens: Int? = null,
    val currentQueryTokens: Int = 0
) {
    fun getFormattedString(): String = buildString {
        appendLine("📊 Статистика токенов:")
        appendLine("├─ Всего: $totalTokens")
        appendLine("├─ Системные: $systemTokens")
        appendLine("├─ Пользователь: $userTokens")
        appendLine("├─ Ассистент: $assistantTokens")
        lastResponseTokens?.let {
            appendLine("├─ Последний ответ: $it")
        }
        append("└─ Текущий запрос: $currentQueryTokens")
    }
}

data class LastResponseInfo(
    val timeMs: Long,
    val tokenCount: Int,
    val promptTokens: Int?,
    val tokensPerSecond: Double?
)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val temperature: Double = 0.7,
    val currentRepositoryType: RepositoryType = RepositoryType.GIGACHAT,
    val huggingFaceModel: HuggingFaceModel = HuggingFaceModel.MEDIUM,
    val lastResponse: LastResponseInfo? = null,
    val tokenStats: TokenStats = TokenStats(),
    val showTokenDetails: Boolean = false,
    val toastMessage: String? = null,

    // Поля для workflow (рабочего процесса)
    val workflowState: WorkflowState = WorkflowState(),

    // Новые поля для стратегий контекста
    val currentContextStrategy: ContextStrategy = ContextStrategy.SlidingWindow(),
    val showContextSettings: Boolean = false,
    val facts: Map<String, DialogFact> = emptyMap(),
    val branches: List<DialogBranch> = emptyList(),
    val currentBranchId: String? = null,
    val slidingWindowSize: Int = 10,
    val showBranchDialog: Boolean = false,
    val selectedMessageForBranch: String? = null,
    val branchNameInput: String = "",

    // Поля для изучения языков
    val languageToLearn: String? = null,
    val learningGoal: String? = null,
    val currentLevel: String? = null,
    val lessonsCompleted: Int = 0,
    val exercisesCompleted: Int = 0,
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val achievements: List<String> = emptyList(),
    val streakDays: Int = 0,

    // Поля для настроек пользователя
    val userSettings: UserSettings = UserSettings.createDefault(),
    val showProfileDialog: Boolean = false
)