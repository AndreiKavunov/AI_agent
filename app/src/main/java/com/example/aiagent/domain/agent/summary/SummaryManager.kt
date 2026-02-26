// domain/summary/SummaryManager.kt
package com.example.aiagent.domain.agent.summary

import android.util.Log
import com.example.aiagent.data.database.MessageEntity
import com.example.aiagent.data.database.MessageLocalRepository
import com.example.aiagent.data.database.SummaryDao
import com.example.aiagent.data.database.SummaryEntity
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage
import com.example.aiagent.data.huggingFace.HuggingFaceRepositoryImpl
import com.example.aiagent.data.response.AgentResponse
import com.example.aiagent.domain.RepositoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "SummaryManager"

class SummaryManager(
    private val localRepository: MessageLocalRepository,
    private val summaryDao: SummaryDao,
    private val gigaChatRepository: GigaChatRepository,
    private val huggingFaceRepository: HuggingFaceRepositoryImpl
) {

    companion object {
        const val MAX_FRESH_MESSAGES = 10
        const val SUMMARY_INTERVAL = 5
    }

    /**
     * Получает историю с учетом суммаризаций для отправки в API
     * Возвращает список MessageEntity в правильном порядке
     */
    suspend fun getMessagesForApi(): List<MessageEntity> {
        val allMessages = localRepository.getAllMessages()
        val sessionId = localRepository.provideSessionId()
        val summaries = summaryDao.getSummariesForSession(sessionId)

        // Отделяем системные сообщения
        val systemMessages = allMessages.filter { it.role == "system" }
        val nonSystemMessages = allMessages.filter { it.role != "system" }

        // Определяем, какие сообщения уже суммаризованы
        val summarizedMessageIds = getSummarizedMessageIds(nonSystemMessages, summaries)

        // Свежие сообщения = последние MAX_FRESH_MESSAGES несуммаризованных
        val freshNonSystemMessages = nonSystemMessages
            .filterNot { summarizedMessageIds.contains(it.id) }
            .takeLast(MAX_FRESH_MESSAGES)

        // Получаем оригинальный системный промпт
        val systemPrompt = systemMessages.firstOrNull()?.content ?: "Ты полезный ассистент. Отвечай кратко и по делу на русском языке."

        // Объединяем все суммаризации в один текст
        val combinedSummary = if (summaries.isNotEmpty()) {
            buildCombinedSummary(summaries)
        } else {
            null
        }

        // Формируем финальное system сообщение
        val finalSystemContent = if (combinedSummary != null) {
            """
                $systemPrompt
                
                Краткое содержание предыдущего диалога:
                $combinedSummary
            """.trimIndent()
        } else {
            systemPrompt
        }

        // Строим финальный список сообщений
        val finalMessages = buildList {
            // Добавляем объединенное system сообщение
            add(
                MessageEntity(
                    id = "system_combined_${UUID.randomUUID()}",
                    sessionId = sessionId,
                    role = "system",
                    content = finalSystemContent,
                    timestamp = System.currentTimeMillis(),
                    repositoryType = "system",
                    modelName = null,
                    realTokenCount = summaries.sumOf { it.tokenCount } // Примерный подсчет
                )
            )

            // Добавляем все свежие сообщения
            addAll(freshNonSystemMessages)
        }

        // Логирование
        Log.d(TAG, "📊 Контекст для API:")
        Log.d(TAG, "   ├─ Системный промпт (объединен${if (summaries.isNotEmpty()) " + ${summaries.size} суммаризаций" else ""})")
        Log.d(TAG, "   ├─ Свежих сообщений: ${freshNonSystemMessages.size}")
        Log.d(TAG, "   ├─ Всего элементов: ${finalMessages.size}")

        if (summaries.isNotEmpty()) {
            Log.d(TAG, "   └─ Суммаризации покрывают ${summaries.sumOf { it.messageCount }} старых сообщений")
        }

        return finalMessages
    }

    /**
     * Конвертирует MessageEntity в GigaMessage для отправки в API
     */
    fun toGigaMessages(messages: List<MessageEntity>): List<GigaMessage> {
        return messages.map { entity ->
            GigaMessage(
                role = entity.role,
                content = entity.content
            )
        }
    }

    /**
     * Объединяет все суммаризации в один текст
     */
    private fun buildCombinedSummary(summaries: List<SummaryEntity>): String {
        return summaries.joinToString("\n\n") { summary ->
            "• ${summary.summary}"
        }
    }

    /**
     * Проверяет необходимость суммаризации и запускает её
     */
    fun checkAndSummarizeIfNeeded(currentType: RepositoryType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!shouldSummarize()) return@launch

                val batch = getNextBatchForSummary() ?: return@launch

                Log.d(TAG, "📝 Начинаем суммаризацию ${batch.size} сообщений")
                batch.forEachIndexed { index, msg ->
                    Log.d(TAG, "   📄 [${index + 1}] ${msg.role}: ${msg.content.take(50)}...")
                }

                // Формируем промпт для суммаризации
                val summaryPrompt = buildSummaryPrompt(batch)

                // Отправляем запрос на суммаризацию
                val summaryResponse = when (currentType) {
                    RepositoryType.GIGACHAT -> {
                        gigaChatRepository.sendMessage(
                            message = summaryPrompt,
                            temperature = 0.3
                        )
                    }
                    RepositoryType.HUGGINGFACE -> {
                        huggingFaceRepository.sendMessage(
                            message = summaryPrompt,
                            temperature = 0.3
                        )
                    }
                }

                // Сохраняем суммаризацию
                saveSummary(batch, summaryResponse)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при суммаризации: ${e.message}")
            }
        }
    }

    private suspend fun shouldSummarize(): Boolean {
        val allMessages = localRepository.getAllMessages()
        val nonSystemMessages = allMessages.filter { it.role != "system" }

        val sessionId = localRepository.provideSessionId()
        val summaries = summaryDao.getSummariesForSession(sessionId)
        val summarizedCount = summaries.sumOf { it.messageCount }
        val remainingMessages = nonSystemMessages.size - summarizedCount

        val needSummary = remainingMessages > MAX_FRESH_MESSAGES &&
                (remainingMessages - MAX_FRESH_MESSAGES) >= SUMMARY_INTERVAL

        if (needSummary) {
            Log.d(TAG, "🔍 Проверка суммаризации: нужно создать")
            Log.d(TAG, "   ├─ Всего сообщений: ${nonSystemMessages.size}")
            Log.d(TAG, "   ├─ Суммаризовано: $summarizedCount")
            Log.d(TAG, "   ├─ Осталось: $remainingMessages")
            Log.d(TAG, "   ├─ MAX_FRESH_MESSAGES: $MAX_FRESH_MESSAGES")
            Log.d(TAG, "   └─ Будет суммаризовано: ${remainingMessages - MAX_FRESH_MESSAGES}")
        }

        return needSummary
    }

    private suspend fun getNextBatchForSummary(): List<MessageEntity>? {
        val allMessages = localRepository.getAllMessages()
        val nonSystemMessages = allMessages.filter { it.role != "system" }

        val sessionId = localRepository.provideSessionId()
        val summaries = summaryDao.getSummariesForSession(sessionId)
        val summarizedIds = getSummarizedMessageIds(nonSystemMessages, summaries)

        val batch = nonSystemMessages
            .filterNot { summarizedIds.contains(it.id) }
            .take(SUMMARY_INTERVAL)

        return if (batch.size == SUMMARY_INTERVAL) {
            Log.d(TAG, "📦 Найдена партия для суммаризации: ${batch.size} сообщений")
            batch
        } else {
            null
        }
    }

    private suspend fun saveSummary(messages: List<MessageEntity>, response: AgentResponse) {
        val summaryTokens = response.tokenCount ?: 0

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            sessionId = localRepository.provideSessionId(),
            summary = response.text,
            messageCount = messages.size,
            startMessageId = messages.first().id,
            endMessageId = messages.last().id,
            tokenCount = summaryTokens
        )

        summaryDao.insertSummary(summary)

        Log.d(TAG, "✅ Суммаризация сохранена:")
        Log.d(TAG, "   ├─ ID: ${summary.id}")
        Log.d(TAG, "   ├─ Текст: ${response.text.take(100)}...")
        Log.d(TAG, "   ├─ Токенов: $summaryTokens")
        Log.d(TAG, "   └─ Покрывает сообщений: ${messages.size}")
    }

    private fun getSummarizedMessageIds(
        nonSystemMessages: List<MessageEntity>,
        summaries: List<SummaryEntity>
    ): Set<String> {
        val summarizedIds = mutableSetOf<String>()

        summaries.forEach { summary ->
            val startIndex = nonSystemMessages.indexOfFirst { it.id == summary.startMessageId }
            val endIndex = nonSystemMessages.indexOfFirst { it.id == summary.endMessageId }

            if (startIndex >= 0 && endIndex >= 0) {
                for (i in startIndex..endIndex) {
                    if (i < nonSystemMessages.size) {
                        summarizedIds.add(nonSystemMessages[i].id)
                    }
                }
            }
        }

        return summarizedIds
    }

    private fun buildSummaryPrompt(messages: List<MessageEntity>): String {
        val conversation = messages.joinToString("\n") {
            "${it.role}: ${it.content}"
        }

        return """
            Ты - ассистент, который создает краткие суммаризации диалогов.
            
            Кратко суммируй следующий диалог на русском языке (2-3 предложения), 
            сохраняя ключевые факты и контекст:
            
            $conversation
            
            Суммаризация:
        """.trimIndent()
    }
}