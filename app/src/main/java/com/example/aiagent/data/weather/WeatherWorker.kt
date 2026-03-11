package com.example.aiagent.data.weather

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aiagent.data.database.AppDatabase
import com.example.aiagent.data.database.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "WeatherWorker"
        const val WORK_NAME = "WeatherWork"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting weather fetch")
            
            val repository = WeatherRepository()
            val notificationManager = WeatherNotificationManager(applicationContext)
            val database = AppDatabase.getInstance(applicationContext)
            
            // Fetch weather data
            val result = repository.getWeather()
            
            result.onSuccess { weatherData ->
                Log.d(TAG, "Weather data fetched successfully: $weatherData")
                
                // Save weather data to database
                withContext(Dispatchers.IO) {
                    val weatherEntity = WeatherEntity(
                        location = weatherData.location,
                        temperature = weatherData.temperature,
                        condition = weatherData.condition,
                        description = weatherData.description
                    )
                    database.weatherDao().insert(weatherEntity)
                    Log.d(TAG, "Weather data saved to database")
                }
                
                // Send notification with average temperature
                notificationManager.showWeatherNotification(weatherData, database)
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch weather data", error)
                return Result.retry()
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in WeatherWorker", e)
            Result.retry()
        }
    }
}
