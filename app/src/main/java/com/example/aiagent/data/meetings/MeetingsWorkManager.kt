package com.example.aiagent.data.meetings

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MeetingsWorkManager(private val context: Context) {
    
    private val tag = "MeetingsWorkManager"

    companion object {
        private const val MIN_INTERVAL_MINUTES = 15L
    }

    /**
     * Schedule periodic meetings notifications
     * @param intervalMinutes Interval in minutes (minimum 15 minutes)
     */
    fun scheduleMeetingsNotifications(intervalMinutes: Long): Boolean {
        Log.d(tag, "========================================")
        Log.d(tag, "Scheduling meetings notifications")
        Log.d(tag, "========================================")
        Log.d(tag, "Requested interval: $intervalMinutes minutes")
        
        val validInterval = maxOf(intervalMinutes, MIN_INTERVAL_MINUTES)
        Log.d(tag, "Validated interval: $validInterval minutes")
        
        // Remove network constraints temporarily to ensure worker runs
        val workRequest = PeriodicWorkRequestBuilder<MeetingsWorker>(
            validInterval, TimeUnit.MINUTES
        )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MeetingsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        
        Log.d(tag, "✓ Meetings notifications scheduled successfully")
        Log.d(tag, "========================================")
        return true
    }

    /**
     * Cancel meetings notifications
     */
    fun cancelMeetingsNotifications() {
        Log.d(tag, "========================================")
        Log.d(tag, "Cancelling meetings notifications")
        Log.d(tag, "========================================")
        WorkManager.getInstance(context).cancelUniqueWork(MeetingsWorker.WORK_NAME)
        Log.d(tag, "✓ Meetings notifications cancelled")
        Log.d(tag, "========================================")
    }

    /**
     * Check if meetings notifications are scheduled
     */
    fun isMeetingsNotificationsScheduled(): Boolean {
        Log.d(tag, "Checking if meetings notifications are scheduled...")
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MeetingsWorker.WORK_NAME)
            .get()
        
        val isScheduled = workInfos.isNotEmpty() && workInfos[0].state != androidx.work.WorkInfo.State.CANCELLED
        Log.d(tag, "Work infos size: ${workInfos.size}")
        if (workInfos.isNotEmpty()) {
            Log.d(tag, "Work state: ${workInfos[0].state}")
        }
        Log.d(tag, "Is scheduled: $isScheduled")
        return isScheduled
    }
}
