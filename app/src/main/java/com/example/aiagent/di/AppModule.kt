package com.example.aiagent.di


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.domain.ChatRepository
import com.example.aiagent.ui.screen.ChatViewModel


// di/AppModule.kt
object AppModule {
    private var repositoryInstance: ChatRepository? = null

    val chatRepository: ChatRepository
        get() {
            if (repositoryInstance == null) {
                repositoryInstance = GigaChatRepository()
            }
            return repositoryInstance!!
        }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(chatRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    fun setTestRepository(repository: ChatRepository) {
        repositoryInstance = repository
    }
}