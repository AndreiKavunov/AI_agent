package com.example.aiagent.di


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.domain.ChatRepository
import com.example.aiagent.domain.HuggingFaceRepository
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.ui.screen.ChatViewModel


object AppModule {
    private var repositoryInstance: ChatRepository? = null
    private var currentType = RepositoryType.GIGACHAT

    // Убираем backing field, всегда возвращаем актуальный экземпляр
    val chatRepository: ChatRepository
        get() {
            // Всегда возвращаем текущий репозиторий, создаем если null
            if (repositoryInstance == null) {
                repositoryInstance = createRepository(currentType)
            }
            return repositoryInstance!!
        }

    val huggingFaceRepository: HuggingFaceRepository?
        get() = repositoryInstance as? HuggingFaceRepository

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    // Передаем null, ViewModel будет получать репозиторий через AppModule
                    return ChatViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    fun switchRepository(type: RepositoryType): ChatRepository {
        currentType = type
        repositoryInstance = createRepository(type)
        return repositoryInstance!!
    }

    private fun createRepository(type: RepositoryType): ChatRepository {
        return when (type) {
            RepositoryType.GIGACHAT -> GigaChatRepository.getInstance()
            RepositoryType.HUGGINGFACE -> HuggingFaceRepositoryImpl.getInstance()
        }
    }

    fun getCurrentRepositoryType(): RepositoryType = currentType

    fun setTestRepository(repository: ChatRepository) {
        repositoryInstance = repository
    }
}