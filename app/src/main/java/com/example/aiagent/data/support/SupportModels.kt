package com.example.aiagent.data.support

import kotlinx.serialization.Serializable

@Serializable
data class AskQuestionRequest(
    val question: String,
    val user_id: String? = null,
    val ticket_id: String? = null
)

@Serializable
data class RagContext(
    val file: String,
    val chunk_id: Int,
    val section: String,
    val relevance: Double
)

@Serializable
data class AskQuestionResponse(
    val success: Boolean,
    val answer: String = "",
    val user_context: String? = null,
    val ticket_context: String? = null,
    val rag_context: List<RagContext> = emptyList()
)

@Serializable
data class Ticket(
    val id: String,
    val user_id: String,
    val status: String,
    val priority: String,
    val subject: String,
    val description: String,
    val created_at: String,
    val last_updated: String? = null
)

@Serializable
data class UserTicketsResponse(
    val success: Boolean,
    val user_id: String,
    val count: Int,
    val tickets: List<Ticket> = emptyList()
)

@Serializable
data class TicketResponse(
    val success: Boolean,
    val ticket: Ticket? = null
)

@Serializable
data class FAQItem(
    val id: String,
    val question: String,
    val answer: String
)

@Serializable
data class FAQResponse(
    val success: Boolean,
    val count: Int,
    val faq: List<FAQItem> = emptyList()
)
