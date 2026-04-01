package com.example.aiagent.ui.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.support.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val TAG = "SupportViewModel"

sealed class SupportUiState {
    object Idle : SupportUiState()
    object Loading : SupportUiState()
    data class Success(val response: AskQuestionResponse) : SupportUiState()
    data class Error(val message: String) : SupportUiState()
}

sealed class TicketsUiState {
    object Idle : TicketsUiState()
    object Loading : TicketsUiState()
    data class Success(val tickets: List<Ticket>) : TicketsUiState()
    data class Error(val message: String) : TicketsUiState()
}

sealed class FAQUiState {
    object Idle : FAQUiState()
    object Loading : FAQUiState()
    data class Success(val faq: List<FAQItem>) : FAQUiState()
    data class Error(val message: String) : FAQUiState()
}

class SupportViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    private val supportRepository = SupportRepository(client)

    private val _supportUiState = MutableStateFlow<SupportUiState>(SupportUiState.Idle)
    val supportUiState: StateFlow<SupportUiState> = _supportUiState.asStateFlow()

    private val _ticketsUiState = MutableStateFlow<TicketsUiState>(TicketsUiState.Idle)
    val ticketsUiState: StateFlow<TicketsUiState> = _ticketsUiState.asStateFlow()

    private val _faqUiState = MutableStateFlow<FAQUiState>(FAQUiState.Idle)
    val faqUiState: StateFlow<FAQUiState> = _faqUiState.asStateFlow()

    private val _selectedTicket = MutableStateFlow<Ticket?>(null)
    val selectedTicket: StateFlow<Ticket?> = _selectedTicket.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        Log.d(TAG, "🎬 SupportViewModel initialized")
    }

    fun setUserId(userId: String) {
        Log.d(TAG, "👤 setUserId called: $userId")
        _userId.value = userId
        Log.d(TAG, "   Current userId: ${_userId.value}")
    }

    fun selectTicket(ticket: Ticket?) {
        Log.d(TAG, "🎫 selectTicket called")
        if (ticket != null) {
            Log.d(TAG, "   Ticket ID: ${ticket.id}")
            Log.d(TAG, "   Ticket subject: ${ticket.subject}")
            Log.d(TAG, "   Ticket status: ${ticket.status}")
        } else {
            Log.d(TAG, "   Ticket: null (deselecting)")
        }
        _selectedTicket.value = ticket
    }

    fun askQuestion(question: String) {
        Log.d(TAG, "❓ askQuestion called")
        Log.d(TAG, "   Question length: ${question.length} characters")
        Log.d(TAG, "   Question preview: ${question.take(100)}${if (question.length > 100) "..." else ""}")
        Log.d(TAG, "   Current userId: ${_userId.value ?: "not set"}")
        Log.d(TAG, "   Selected ticket: ${_selectedTicket.value?.id ?: "none"}")
        
        viewModelScope.launch {
            Log.d(TAG, "   Setting state to Loading...")
            _supportUiState.value = SupportUiState.Loading
            
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "   Calling repository.askQuestion...")
            
            val result = supportRepository.askQuestion(
                question = question,
                userId = _userId.value,
                ticketId = _selectedTicket.value?.id
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "   Repository call completed in ${duration}ms")
            
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "   ✅ Repository call succeeded")
                    Log.d(TAG, "   Response success: ${response.success}")
                    Log.d(TAG, "   Response answer length: ${response.answer?.length ?: 0} characters")
                    Log.d(TAG, "   Response answer: ${response.answer}")
                    Log.d(TAG, "   Response user_context: ${response.user_context}")
                    Log.d(TAG, "   Response ticket_context: ${response.ticket_context}")
                    Log.d(TAG, "   Response rag_context count: ${response.rag_context.size}")
                    response.rag_context.forEachIndexed { index, context ->
                        Log.d(TAG, "   - RAG Context #$index: file=${context.file}, section=${context.section}, relevance=${context.relevance}")
                    }
                    
                    if (response.success) {
                        Log.d(TAG, "   Setting state to Success...")
                        _supportUiState.value = SupportUiState.Success(response)
                        Log.d(TAG, "   ✅ askQuestion completed successfully")
                    } else {
                        Log.w(TAG, "   ⚠️ Response success is false")
                        Log.d(TAG, "   Setting state to Error...")
                        _supportUiState.value = SupportUiState.Error("Failed to get response")
                        Log.w(TAG, "   ❌ askQuestion failed: Response success is false")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "   ❌ Repository call failed")
                    Log.e(TAG, "   Exception type: ${exception.javaClass.simpleName}")
                    Log.e(TAG, "   Exception message: ${exception.message}")
                    Log.e(TAG, "   Stack trace:", exception)
                    
                    val errorMessage = exception.message ?: "Unknown error occurred"
                    Log.d(TAG, "   Setting state to Error: $errorMessage")
                    _supportUiState.value = SupportUiState.Error(errorMessage)
                    Log.e(TAG, "   ❌ askQuestion failed: $errorMessage")
                }
            )
        }
    }

    fun loadUserTickets(userId: String) {
        Log.d(TAG, "📋 loadUserTickets called")
        Log.d(TAG, "   UserId: $userId")
        
        viewModelScope.launch {
            Log.d(TAG, "   Setting state to Loading...")
            _ticketsUiState.value = TicketsUiState.Loading
            
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "   Calling repository.getUserTickets...")
            
            val result = supportRepository.getUserTickets(userId)
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "   Repository call completed in ${duration}ms")
            
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "   ✅ Repository call succeeded")
                    Log.d(TAG, "   Response success: ${response.success}")
                    Log.d(TAG, "   Tickets count: ${response.tickets.size}")
                    response.tickets.forEach { ticket ->
                        Log.d(TAG, "   - Ticket #${ticket.id}: ${ticket.subject} (${ticket.status})")
                    }
                    
                    if (response.success) {
                        Log.d(TAG, "   Setting state to Success with ${response.tickets.size} tickets...")
                        _ticketsUiState.value = TicketsUiState.Success(response.tickets)
                        Log.d(TAG, "   ✅ loadUserTickets completed successfully")
                    } else {
                        Log.w(TAG, "   ⚠️ Response success is false")
                        Log.d(TAG, "   Setting state to Error...")
                        _ticketsUiState.value = TicketsUiState.Error("Failed to load tickets")
                        Log.w(TAG, "   ❌ loadUserTickets failed: Response success is false")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "   ❌ Repository call failed")
                    Log.e(TAG, "   Exception type: ${exception.javaClass.simpleName}")
                    Log.e(TAG, "   Exception message: ${exception.message}")
                    Log.e(TAG, "   Stack trace:", exception)
                    
                    val errorMessage = exception.message ?: "Unknown error occurred"
                    Log.d(TAG, "   Setting state to Error: $errorMessage")
                    _ticketsUiState.value = TicketsUiState.Error(errorMessage)
                    Log.e(TAG, "   ❌ loadUserTickets failed: $errorMessage")
                }
            )
        }
    }

    fun loadFAQ() {
        Log.d(TAG, "📚 loadFAQ called")
        
        viewModelScope.launch {
            Log.d(TAG, "   Setting state to Loading...")
            _faqUiState.value = FAQUiState.Loading
            
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "   Calling repository.getFAQ...")
            
            val result = supportRepository.getFAQ()
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "   Repository call completed in ${duration}ms")
            
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "   ✅ Repository call succeeded")
                    Log.d(TAG, "   Response success: ${response.success}")
                    Log.d(TAG, "   FAQ items count: ${response.faq.size}")
                    response.faq.forEachIndexed { index, item ->
                        Log.d(TAG, "   - FAQ #$index: ${item.question}")
                    }
                    
                    if (response.success) {
                        Log.d(TAG, "   Setting state to Success with ${response.faq.size} FAQ items...")
                        _faqUiState.value = FAQUiState.Success(response.faq)
                        Log.d(TAG, "   ✅ loadFAQ completed successfully")
                    } else {
                        Log.w(TAG, "   ⚠️ Response success is false")
                        Log.d(TAG, "   Setting state to Error...")
                        _faqUiState.value = FAQUiState.Error("Failed to load FAQ")
                        Log.w(TAG, "   ❌ loadFAQ failed: Response success is false")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "   ❌ Repository call failed")
                    Log.e(TAG, "   Exception type: ${exception.javaClass.simpleName}")
                    Log.e(TAG, "   Exception message: ${exception.message}")
                    Log.e(TAG, "   Stack trace:", exception)
                    
                    val errorMessage = exception.message ?: "Unknown error occurred"
                    Log.d(TAG, "   Setting state to Error: $errorMessage")
                    _faqUiState.value = FAQUiState.Error(errorMessage)
                    Log.e(TAG, "   ❌ loadFAQ failed: $errorMessage")
                }
            )
        }
    }

    fun resetSupportState() {
        Log.d(TAG, "🔄 resetSupportState called")
        Log.d(TAG, "   Previous state: ${_supportUiState.value}")
        _supportUiState.value = SupportUiState.Idle
        Log.d(TAG, "   New state: ${_supportUiState.value}")
        Log.d(TAG, "   ✅ State reset to Idle")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 SupportViewModel cleared, closing HTTP client...")
        client.close()
        Log.d(TAG, "   ✅ HTTP client closed")
    }
}
