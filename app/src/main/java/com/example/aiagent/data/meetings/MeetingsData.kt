package com.example.aiagent.data.meetings

import kotlinx.serialization.Serializable

@Serializable
data class MeetingsData(
    val meetings: String,
    val meetingCount: Int? = null
)

@Serializable
data class MeetingsResponse(
    val success: Boolean,
    val result: String? = null,
    val error: String? = null
)
