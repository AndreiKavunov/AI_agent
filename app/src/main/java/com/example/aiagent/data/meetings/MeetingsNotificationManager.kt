package com.example.aiagent.data.meetings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aiagent.MainActivity
import com.example.aiagent.R

class MeetingsNotificationManager(private val context: Context) {
    
    private val tag = "MeetingsNotification"

    companion object {
        private const val CHANNEL_ID = "meetings_notifications"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_NAME = "Meetings Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for meetings schedule"
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

    fun showMeetingsNotification(meetingsData: MeetingsData) {
        Log.d(tag, "========================================")
        Log.d(tag, "Showing meetings notification")
        Log.d(tag, "========================================")
        Log.d(tag, "Meetings data: ${meetingsData.meetings}")
        Log.d(tag, "Meeting count: ${meetingsData.meetingCount}")
        
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

        val contentText = buildNotificationText(meetingsData)
        val bigText = buildDetailedMeetingsText(meetingsData)
        
        Log.d(tag, "Notification title: Сегодняшние встречи")
        Log.d(tag, "Notification content: $contentText")
        Log.d(tag, "Notification big text: $bigText")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Сегодняшние встречи")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            Log.d(tag, "Attempting to show notification...")
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(tag, "✓ Notification shown successfully")
        } catch (e: SecurityException) {
            Log.e(tag, "✗ Notification permission not granted", e)
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(tag, "✗ Error showing notification", e)
            e.printStackTrace()
        }
    }

    private fun buildNotificationText(meetingsData: MeetingsData): String {
        return if (meetingsData.meetingCount != null) {
            "На сегодня запланировано ${meetingsData.meetingCount} встреч"
        } else {
            "Проверьте расписание встреч на сегодня"
        }
    }

    private fun buildDetailedMeetingsText(meetingsData: MeetingsData): String {
        return StringBuilder().apply {
            append("📅 Расписание встреч на сегодня\n\n")
            if (meetingsData.meetingCount != null) {
                append("📊 Всего встреч: ${meetingsData.meetingCount}\n\n")
            }
            append("📝 Список встреч:\n")
            append(meetingsData.meetings)
        }.toString()
    }
}
