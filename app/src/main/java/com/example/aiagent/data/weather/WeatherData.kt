package com.example.aiagent.data.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val location: String,
    val temperature: Double,
    val condition: String,
    val description: String,
    val humidity: Int? = null,
    val windSpeed: Double? = null
)

@Serializable
data class WeatherResponse(
    val success: Boolean,
    val data: WeatherData? = null,
    val error: String? = null
)
