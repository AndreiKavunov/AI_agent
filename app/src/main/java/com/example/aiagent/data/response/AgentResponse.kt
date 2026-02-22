package com.example.aiagent.data.response

import kotlinx.serialization.Serializable


@Serializable
data class AgentResponse(
    val text: String,
    val toolUsed: String? = null,
    val responseTimeMs: Long = 0,
    val tokenCount: Int = 0
)