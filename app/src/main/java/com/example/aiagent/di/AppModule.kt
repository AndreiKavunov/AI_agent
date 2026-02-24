// di/AppModule.kt
package com.example.aiagent.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.agent.UniversalAgentImpl
import com.example.aiagent.ui.screen.ChatViewModel

object AppModule {

    private lateinit var appContext: Context

    // Инициализация модуля с контекстом приложения
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Репозитории (синглтоны)
    private val gigaChatRepository: GigaChatRepository by lazy {
        GigaChatRepository.getInstance()
    }

    private val huggingFaceRepository: HuggingFaceRepositoryImpl by lazy {
        HuggingFaceRepositoryImpl.getInstance()
    }

    // Локальный репозиторий для работы с БД
    private val messageLocalRepository: MessageLocalRepository by lazy {
        MessageLocalRepository.getInstance(appContext)
    }

    // УНИВЕРСАЛЬНЫЙ АГЕНТ - использует БД для хранения истории
    val universalAgent: UniversalAgent by lazy {
        UniversalAgentImpl(
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository,
            localRepository = messageLocalRepository
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
    suspend fun getCurrentRepositoryType() = universalAgent.getCurrentRepositoryType()

    suspend fun switchRepository(type: RepositoryType) {
        universalAgent.switchRepository(type)
    }

    suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        universalAgent.setHuggingFaceModel(modelType)
    }
}