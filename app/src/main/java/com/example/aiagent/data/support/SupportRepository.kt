package com.example.aiagent.data.support

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

private const val TAG = "SupportRepository"

class SupportRepository(
    private val client: HttpClient,
    private val baseUrl: String = "http://192.168.0.82:8000"
) {
    init {
        Log.d(TAG, "🔧 SupportRepository initialized with baseUrl: $baseUrl")
    }

    suspend fun askQuestion(
        question: String,
        userId: String? = null,
        ticketId: String? = null
    ): Result<AskQuestionResponse> {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📤 askQuestion called")
        Log.d(TAG, "   Question: ${question.take(100)}${if (question.length > 100) "..." else ""}")
        Log.d(TAG, "   UserId: ${userId ?: "null"}")
        Log.d(TAG, "   TicketId: ${ticketId ?: "null"}")
        
        return try {
            val request = AskQuestionRequest(question, userId, ticketId)
            Log.d(TAG, "   Request URL: $baseUrl/support/ask")
            Log.d(TAG, "   Request body: question=${question.take(50)}..., userId=$userId, ticketId=$ticketId")
            
            val response = client.post("$baseUrl/support/ask") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            val responseBody = response.body<AskQuestionResponse>()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ askQuestion succeeded in ${duration}ms")
            Log.d(TAG, "   Response success: ${responseBody.success}")
            Log.d(TAG, "   Response answer: ${responseBody.answer?.take(100)}${if (responseBody.answer?.length ?: 0 > 100) "..." else ""}")
//            Log.d(TAG, "   Response ticketId: ${responseBody.ticketId}")
//            Log.d(TAG, "   Response sources: ${responseBody.sources?.size ?: 0}")
            
            Result.success(responseBody)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = when {
                e.message?.contains("404") == true -> "Endpoint not found: $baseUrl/support/ask"
                e.message?.contains("Connection refused") == true -> "Server unavailable at $baseUrl"
                e.message?.contains("timeout") == true -> "Request timeout"
                e.message?.contains("JSON") == true -> "JSON parsing error"
                else -> e.message ?: "Unknown error"
            }
            
            Log.e(TAG, "❌ askQuestion failed after ${duration}ms: $errorMsg")
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            Log.e(TAG, "   Stack trace:", e)
            
            Result.failure(e)
        }
    }

    suspend fun getUserTickets(userId: String): Result<UserTicketsResponse> {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📤 getUserTickets called")
        Log.d(TAG, "   UserId: $userId")
        
        return try {
            val url = "$baseUrl/support/tickets/$userId"
            Log.d(TAG, "   Request URL: $url")
            
            val response = client.get(url) {
                contentType(ContentType.Application.Json)
            }
            
            val responseBody = response.body<UserTicketsResponse>()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ getUserTickets succeeded in ${duration}ms")
            Log.d(TAG, "   Response success: ${responseBody.success}")
            Log.d(TAG, "   Tickets count: ${responseBody.tickets.size}")
            responseBody.tickets.forEach { ticket ->
                Log.d(TAG, "   Ticket #${ticket.id}: ${ticket.subject} (status: ${ticket.status})")
            }
            
            Result.success(responseBody)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = when {
                e.message?.contains("404") == true -> "Endpoint not found: $baseUrl/support/tickets/$userId"
                e.message?.contains("Connection refused") == true -> "Server unavailable at $baseUrl"
                e.message?.contains("timeout") == true -> "Request timeout"
                e.message?.contains("JSON") == true -> "JSON parsing error"
                else -> e.message ?: "Unknown error"
            }
            
            Log.e(TAG, "❌ getUserTickets failed after ${duration}ms: $errorMsg")
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            Log.e(TAG, "   Stack trace:", e)
            
            Result.failure(e)
        }
    }

    suspend fun getTicket(ticketId: String): Result<TicketResponse> {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📤 getTicket called")
        Log.d(TAG, "   TicketId: $ticketId")
        
        return try {
            val url = "$baseUrl/support/ticket/$ticketId"
            Log.d(TAG, "   Request URL: $url")
            
            val response = client.get(url) {
                contentType(ContentType.Application.Json)
            }
            
            val responseBody = response.body<TicketResponse>()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ getTicket succeeded in ${duration}ms")
            Log.d(TAG, "   Response success: ${responseBody.success}")
            Log.d(TAG, "   Ticket subject: ${responseBody.ticket?.subject}")
            Log.d(TAG, "   Ticket status: ${responseBody.ticket?.status}")
//            Log.d(TAG, "   Messages count: ${responseBody.ticket?.messages?.size ?: 0}")
            
            Result.success(responseBody)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = when {
                e.message?.contains("404") == true -> "Ticket not found: $ticketId"
                e.message?.contains("Connection refused") == true -> "Server unavailable at $baseUrl"
                e.message?.contains("timeout") == true -> "Request timeout"
                e.message?.contains("JSON") == true -> "JSON parsing error"
                else -> e.message ?: "Unknown error"
            }
            
            Log.e(TAG, "❌ getTicket failed after ${duration}ms: $errorMsg")
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            Log.e(TAG, "   Stack trace:", e)
            
            Result.failure(e)
        }
    }

    suspend fun getFAQ(): Result<FAQResponse> {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📤 getFAQ called")
        
        return try {
            val url = "$baseUrl/support/faq"
            Log.d(TAG, "   Request URL: $url")
            
            val response = client.get(url) {
                contentType(ContentType.Application.Json)
            }
            
            val responseBody = response.body<FAQResponse>()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "✅ getFAQ succeeded in ${duration}ms")
            Log.d(TAG, "   Response success: ${responseBody.success}")
            Log.d(TAG, "   FAQ items count: ${responseBody.faq.size}")
            responseBody.faq.forEachIndexed { index, item ->
                Log.d(TAG, "   FAQ #$index: ${item.question}")
            }
            
            Result.success(responseBody)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = when {
                e.message?.contains("404") == true -> "FAQ endpoint not found: $baseUrl/support/faq"
                e.message?.contains("Connection refused") == true -> "Server unavailable at $baseUrl"
                e.message?.contains("timeout") == true -> "Request timeout"
                e.message?.contains("JSON") == true -> "JSON parsing error"
                else -> e.message ?: "Unknown error"
            }
            
            Log.e(TAG, "❌ getFAQ failed after ${duration}ms: $errorMsg")
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            Log.e(TAG, "   Stack trace:", e)
            
            Result.failure(e)
        }
    }
}
