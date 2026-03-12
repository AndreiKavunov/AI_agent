package com.example.aiagent.data.meetings

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aiagent.data.giga.GigaChatRepository
import com.example.aiagent.data.giga.GigaMessage

class MeetingsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "MeetingsWorker"
        const val WORK_NAME = "MeetingsWork"
    }

    init {
        Log.d(TAG, "========================================")
        Log.d(TAG, "MeetingsWorker constructor called")
        Log.d(TAG, "========================================")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Starting meetings and todos fetch")
        Log.d(TAG, "========================================")
        
        return try {
            val repository = MeetingsRepository()
            val notificationManager = MeetingsNotificationManager(applicationContext)
            val gigaChatRepository = GigaChatRepository.getInstance()
            
            Log.d(TAG, "Repository and managers initialized")
            
            // Fetch meetings and todos data
            Log.d(TAG, "Fetching meetings and todos from repository...")
            val result = repository.getMeetingsAndTodos()
            
            result.onSuccess { meetingsData ->
                Log.d(TAG, "✓ Meetings and todos data fetched successfully")
                Log.d(TAG, "Meetings text: ${meetingsData.meetings}")
                Log.d(TAG, "Todos text: ${meetingsData.todos}")
                
                // Send meetings and todos data to GigaChat to generate summary
                val prompt = """Проанализируй следующую информацию о встречах и задачах на сегодня и составь краткое саммари на русском языке.
Встречи: ${meetingsData.meetings}
Задачи: ${meetingsData.todos}

Формат саммари должен быть таким: "сегодня у вас X встреч, и Y задач, Z из которых вы уже сделали. Сегодня у вас был продуктивный день" или в зависимости от ситуации "Сегодня у вас был спокойный день" или "Сегодня у вас был насыщенный день".
Ответь только саммари, без дополнительного текста."""
                Log.d(TAG, "Sending to GigaChat to generate summary...")
                
                try {
                    val response = gigaChatRepository.sendMessage(prompt, 0.3)
                    
                    Log.d(TAG, "✓ GigaChat response received: ${response.text}")
                    
                    // Extract summary from response
                    val summary = extractSummary(response.text)
                    Log.d(TAG, "Extracted summary: $summary")
                    
                    // Update meetings data with summary
                    val updatedMeetingsData = meetingsData.copy(summary = summary)
                    
                    // Send notification with summary
                    Log.d(TAG, "Showing meetings and todos notification...")
                    notificationManager.showMeetingsNotification(updatedMeetingsData)
                    Log.d(TAG, "✓ Meetings and todos notification shown successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to get summary from GigaChat", e)
                    // Show notification without summary if GigaChat fails
                    Log.d(TAG, "Showing meetings and todos notification without summary...")
                    notificationManager.showMeetingsNotification(meetingsData)
                    Log.d(TAG, "✓ Meetings and todos notification shown (without summary)")
                }
            }.onFailure { error ->
                Log.e(TAG, "✗ Failed to fetch meetings and todos data", error)
                Log.e(TAG, "Error message: ${error.message}")
                return Result.retry()
            }
            
            Log.d(TAG, "✓ MeetingsWorker completed successfully")
            Log.d(TAG, "========================================")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error in MeetingsWorker", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.d(TAG, "========================================")
            Result.retry()
        }
    }
    
    /**
     * Извлекает саммари из ответа GigaChat
     */
    private fun extractSummary(response: String): String {
        // Убираем лишние пробелы и переносы строк
        return response.trim()
    }
}
