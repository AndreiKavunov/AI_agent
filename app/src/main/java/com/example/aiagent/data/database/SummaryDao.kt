package com.example.aiagent.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SummaryDao {
    @Insert
    suspend fun insertSummary(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getSummariesForSession(sessionId: String): List<SummaryEntity>

    @Query("DELETE FROM summaries WHERE sessionId = :sessionId")
    suspend fun deleteAllSummaries(sessionId: String)

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId AND startMessageId <= :messageId AND endMessageId >= :messageId LIMIT 1")
    suspend fun getSummaryContainingMessage(sessionId: String, messageId: String): SummaryEntity?
}