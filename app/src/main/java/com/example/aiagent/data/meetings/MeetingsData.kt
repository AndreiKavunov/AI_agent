package com.example.aiagent.data.meetings

import kotlinx.serialization.Serializable

@Serializable
data class MeetingsData(
    val meetings: String,
    val meetingCount: Int? = null,
    val todos: String? = null,
    val todoCount: Int? = null,
    val completedTodosCount: Int? = null,
    val summary: String? = null
)

@Serializable
data class MeetingsResponse(
    val success: Boolean,
    val result: String? = null,
    val error: String? = null
)
