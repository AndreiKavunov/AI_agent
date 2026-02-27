// di/AppModule.kt
package com.example.aiagent.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiagent.data.database.AppDatabase
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.database.SummaryDao
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.agent.UniversalAgent
import com.example.aiagent.domain.agent.UniversalAgentImpl
import com.example.aiagent.domain.contextStrategy.ContextStrategyManager
import com.example.aiagent.domain.contextStrategy.FactExtractor
import com.example.aiagent.ui.screen.ChatViewModel

object AppModule {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // База данных
    private val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    // DAO
    private val summaryDao: SummaryDao by lazy {
        appDatabase.summaryDao()
    }

    // Репозитории
    private val gigaChatRepository: GigaChatRepository by lazy {
        GigaChatRepository.getInstance()
    }

    private val huggingFaceRepository: HuggingFaceRepositoryImpl by lazy {
        HuggingFaceRepositoryImpl.getInstance()
    }

    // Локальный репозиторий
    private val messageLocalRepository: MessageLocalRepository by lazy {
        MessageLocalRepository.getInstance(appContext)
    }

    // Экстрактор фактов
    private val factExtractor: FactExtractor by lazy {
        FactExtractor(
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository
        )
    }

    // Менеджер стратегий контекста
    private val contextStrategyManager: ContextStrategyManager by lazy {
        ContextStrategyManager(
            localRepository = messageLocalRepository,
            factExtractor = factExtractor
        )
    }

    // УНИВЕРСАЛЬНЫЙ АГЕНТ
    val universalAgent: UniversalAgent by lazy {
        UniversalAgentImpl(
            gigaChatRepository = gigaChatRepository,
            huggingFaceRepository = huggingFaceRepository,
            localRepository = messageLocalRepository,
            summaryDao = summaryDao,
            contextStrategyManager = contextStrategyManager
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

    suspend fun getCurrentRepositoryType() = universalAgent.getCurrentRepositoryType()

    suspend fun switchRepository(type: RepositoryType) {
        universalAgent.switchRepository(type)
    }

    suspend fun setHuggingFaceModel(modelType: HuggingFaceModel) {
        universalAgent.setHuggingFaceModel(modelType)
    }
}