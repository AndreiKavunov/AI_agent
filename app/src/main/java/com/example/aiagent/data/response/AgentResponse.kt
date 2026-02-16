package com.example.aiagent.data.response

import kotlinx.serialization.Serializable


@Serializable
data class AgentResponse(
    val text: String,
    val toolUsed: String? = null
)