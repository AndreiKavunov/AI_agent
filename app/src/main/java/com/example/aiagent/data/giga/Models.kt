package com.example.aiagent.data.giga


import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val access_token: String? = null,
    val expires_at: Long? = null,
    val expires_in: Long? = null,
    val scope: String? = null
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<GigaMessage>,
    val temperature: Double,
    val max_tokens: Int,
    val stream: Boolean = false
)

@Serializable
data class GigaMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<GigaChoice>,
    val created: Long,
    val model: String? = null
)

@Serializable
data class GigaChoice(
    val message: GigaMessage,
    val index: Int = 0,
    val finish_reason: String? = null
)