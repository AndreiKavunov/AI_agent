package com.example.aiagent.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_data")
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val temperature: Double,
    val condition: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
