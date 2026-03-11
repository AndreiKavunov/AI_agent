package com.example.aiagent.data.weather

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WeatherRepository {
    private val client = HttpClient()
    private val baseUrl = "http://192.168.0.82:8000"
    
    suspend fun getWeather(): Result<WeatherData> {
        return try {
            // Try to call a weather endpoint on the MCP server
            val response = client.get("$baseUrl/weather") {
                timeout {
                    requestTimeoutMillis = 10000
                }
            }
            val responseBody = response.body<String>()
            
            // Parse the response
            val json = Json.parseToJsonElement(responseBody).jsonObject
            
            val data = json["data"]?.jsonObject
            if (data != null) {
                val weatherData = WeatherData(
                    location = data["location"]?.jsonPrimitive?.content ?: "Unknown",
                    temperature = data["temperature"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    condition = data["condition"]?.jsonPrimitive?.content ?: "Unknown",
                    description = data["description"]?.jsonPrimitive?.content ?: "No description",
                    humidity = data["humidity"]?.jsonPrimitive?.content?.toIntOrNull(),
                    windSpeed = data["windSpeed"]?.jsonPrimitive?.content?.toDoubleOrNull()
                )
                Result.success(weatherData)
            } else {
                // If no data field, try to parse as direct weather object
                val weatherData = WeatherData(
                    location = json["location"]?.jsonPrimitive?.content ?: "Unknown",
                    temperature = json["temperature"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    condition = json["condition"]?.jsonPrimitive?.content ?: "Unknown",
                    description = json["description"]?.jsonPrimitive?.content ?: "No description",
                    humidity = json["humidity"]?.jsonPrimitive?.content?.toIntOrNull(),
                    windSpeed = json["windSpeed"]?.jsonPrimitive?.content?.toDoubleOrNull()
                )
                Result.success(weatherData)
            }
        } catch (e: Exception) {
            // Return mock data if server is not available
            Result.success(getMockWeatherData())
        }
    }
    
    private fun getMockWeatherData(): WeatherData {
        return WeatherData(
            location = "Moscow",
            temperature = 15.0,
            condition = "Cloudy",
            description = "Partly cloudy with occasional sunshine",
            humidity = 65,
            windSpeed = 12.5
        )
    }
}
