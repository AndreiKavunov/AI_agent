// data/database/AppDatabase.kt
package com.example.aiagent.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        MessageEntity::class,
        SummaryEntity::class,
        com.example.aiagent.data.database.memory.ShortTermMemoryEntity::class,
        com.example.aiagent.data.database.memory.WorkingMemoryEntity::class,
        com.example.aiagent.data.database.memory.LongTermMemoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun summaryDao(): SummaryDao
    abstract fun memoryDao(): com.example.aiagent.data.database.memory.MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_agent_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}