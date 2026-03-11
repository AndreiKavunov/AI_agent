package com.example.aiagent.data.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aiagent.MainActivity
import com.example.aiagent.R
import com.example.aiagent.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "weather_notifications"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "Weather Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for weather forecasts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun showWeatherNotification(weatherData: WeatherData, database: AppDatabase) {
        // Calculate average temperature from all saved data
        val averageTemperature = withContext(Dispatchers.IO) {
            database.weatherDao().getAverageTemperature()
        }
        
        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Weather Forecast")
            .setContentText(buildNotificationText(weatherData, averageTemperature))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(buildDetailedWeatherText(weatherData, averageTemperature)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
            e.printStackTrace()
        }
    }

    private fun buildNotificationText(weatherData: WeatherData, averageTemperature: Double?): String {
        return if (averageTemperature != null) {
            "Current: ${weatherData.temperature}°C | Average: ${String.format("%.1f", averageTemperature)}°C"
        } else {
            weatherData.description
        }
    }

    private fun buildDetailedWeatherText(weatherData: WeatherData, averageTemperature: Double?): String {
        return StringBuilder().apply {
            append("Location: ${weatherData.location}\n")
            append("Current Temperature: ${weatherData.temperature}°C\n")
            if (averageTemperature != null) {
                append("Average Temperature: ${String.format("%.1f", averageTemperature)}°C\n")
            }
            append("Condition: ${weatherData.condition}\n")
            append("Description: ${weatherData.description}\n")
            if (weatherData.humidity != null) {
                append("Humidity: ${weatherData.humidity}%\n")
            }
            if (weatherData.windSpeed != null) {
                append("Wind Speed: ${weatherData.windSpeed} km/h\n")
            }
        }.toString()
    }
}
