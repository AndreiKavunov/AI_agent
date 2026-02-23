// di/AppModule.kt
package com.example.aiagent.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.agent.UniversalAgentImpl
import com.example.aiagent.ui.screen.ChatViewModel

object AppModule {

    // Репозитории (синглтоны)
    private val gigaChatRepository: GigaChatRepository by lazy {
        GigaChatRepository.getInstance()
    }

    private val huggingFaceRepository: HuggingFaceRepositoryImpl by lazy {
        HuggingFaceRepositoryImpl.getInstance()
    }

    // УНИВЕРСАЛЬНЫЙ АГЕНТ - единственное место для работы с чатом
    val universalAgent: UniversalAgent by lazy {
        UniversalAgentImpl(
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository
        )
    }

    // Фабрика ViewModel
    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(universalAgent) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // Вспомогательные методы для обратной совместимости
    fun getCurrentRepositoryType() = universalAgent.getCurrentRepositoryType()

    fun switchRepository(type: RepositoryType) {
        universalAgent.switchRepository(type)
    }

    fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        universalAgent.setHuggingFaceModel(modelType)
    }
}