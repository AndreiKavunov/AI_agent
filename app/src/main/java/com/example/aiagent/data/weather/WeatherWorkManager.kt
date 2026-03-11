package com.example.aiagent.data.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aiagent.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WeatherWorkManager(private val context: Context) {

    companion object {
        private const val MIN_INTERVAL_MINUTES = 15L
    }

    /**
     * Schedule periodic weather notifications
     * @param intervalMinutes Interval in minutes (minimum 15 minutes)
     */
    fun scheduleWeatherNotifications(intervalMinutes: Long): Boolean {
        val validInterval = maxOf(intervalMinutes, MIN_INTERVAL_MINUTES)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WeatherWorker>(
            validInterval, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        return true
    }

    /**
     * Cancel weather notifications and clear saved data
     */
    suspend fun cancelWeatherNotifications() {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherWorker.WORK_NAME)
        
        // Clear all saved weather data from database
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            database.weatherDao().deleteAll()
        }
    }

    /**
     * Check if weather notifications are scheduled
     */
    fun isWeatherNotificationsScheduled(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WeatherWorker.WORK_NAME)
            .get()
        
        return workInfos.isNotEmpty() && workInfos[0].state != androidx.work.WorkInfo.State.CANCELLED
    }
}
