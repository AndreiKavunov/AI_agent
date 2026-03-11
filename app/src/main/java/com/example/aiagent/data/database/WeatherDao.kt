package com.example.aiagent.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeatherDao {
    @Insert
    suspend fun insert(weather: WeatherEntity): Long

    @Query("SELECT * FROM weather_data ORDER BY timestamp DESC")
    suspend fun getAllWeatherData(): List<WeatherEntity>

    @Query("SELECT AVG(temperature) FROM weather_data")
    suspend fun getAverageTemperature(): Double?

    @Query("DELETE FROM weather_data")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM weather_data")
    suspend fun getCount(): Int
}
