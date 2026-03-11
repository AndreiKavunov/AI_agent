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
        Log.d(TAG, "Starting meetings fetch")
        Log.d(TAG, "========================================")
        
        return try {
            val repository = MeetingsRepository()
            val notificationManager = MeetingsNotificationManager(applicationContext)
            val gigaChatRepository = GigaChatRepository.getInstance()
            
            Log.d(TAG, "Repository and managers initialized")
            
            // Fetch meetings data
            Log.d(TAG, "Fetching meetings from repository...")
            val result = repository.getMeetings()
            
            result.onSuccess { meetingsData ->
                Log.d(TAG, "✓ Meetings data fetched successfully")
                Log.d(TAG, "Meetings text: ${meetingsData.meetings}")
                
                // Send meetings data to GigaChat to count meetings
                val prompt = "Посчитай количество встреч в следующем тексте и ответь только числом (например: 3): ${meetingsData.meetings}"
                Log.d(TAG, "Sending to GigaChat to count meetings...")
                
                try {
                    val response = gigaChatRepository.sendMessage(prompt, 0.3)
                    
                    Log.d(TAG, "✓ GigaChat response received: ${response.text}")
                    
                    // Parse the count from response
                    val meetingCount = extractMeetingCount(response.text)
                    Log.d(TAG, "Extracted meeting count: $meetingCount")
                    
                    // Update meetings data with count
                    val updatedMeetingsData = meetingsData.copy(meetingCount = meetingCount)
                    
                    // Send notification with meeting count
                    Log.d(TAG, "Showing meetings notification...")
                    notificationManager.showMeetingsNotification(updatedMeetingsData)
                    Log.d(TAG, "✓ Meetings notification shown successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to get meeting count from GigaChat", e)
                    // Show notification without count if GigaChat fails
                    Log.d(TAG, "Showing meetings notification without count...")
                    notificationManager.showMeetingsNotification(meetingsData)
                    Log.d(TAG, "✓ Meetings notification shown (without count)")
                }
            }.onFailure { error ->
                Log.e(TAG, "✗ Failed to fetch meetings data", error)
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
     * Извлекает количество встреч из ответа GigaChat
     */
    private fun extractMeetingCount(response: String): Int? {
        // Ищем первое число в ответе
        val numberPattern = "\\d+".toRegex()
        val match = numberPattern.find(response)
        return match?.value?.toIntOrNull()
    }
}
