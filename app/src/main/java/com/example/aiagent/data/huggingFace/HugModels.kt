package com.example.aiagent.data.huggingFace

import kotlinx.serialization.Serializable

@Serializable
data class ClassificationResponse(
    val label: String,
    val score: Double
)

@Serializable
data class TranslationResponse(
    val translation_text: String
)

@Serializable
data class SummarizationResponse(
    val summary_text: String
)

@Serializable
data class GenerationResponse(
    val generated_text: String
)

@Serializable
data class HuggingFaceMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ChatMessage
)






