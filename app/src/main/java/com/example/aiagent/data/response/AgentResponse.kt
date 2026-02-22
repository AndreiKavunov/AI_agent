package com.example.aiagent.data.response

import kotlinx.serialization.Serializable


@Serializable
data class AgentResponse(
    val text: String,
    val toolUsed: String? = null,
    val modelUsed: String? = null, // Добавим поле для отслеживания модели
)