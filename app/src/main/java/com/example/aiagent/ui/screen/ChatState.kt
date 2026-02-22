package com.example.aiagent.ui.screen

import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType

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
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message
}

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val temperature: Double = 0.7,
    val currentRepositoryType: RepositoryType = RepositoryType.GIGACHAT,
    val huggingFaceModel: HuggingFaceModel = HuggingFaceModel.MEDIUM
)