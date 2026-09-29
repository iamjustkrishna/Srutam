package space.iamjustkrishna.srutam.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insight_items WHERE id = :id")
    suspend fun getById(id: String): InsightEntity?

    @Query("SELECT * FROM insight_items WHERE sourceInsightId = :id OR sourceReminderId = :id LIMIT 1")
    suspend fun getDerivedTask(id: String): InsightEntity?

    @Query("SELECT * FROM insight_items WHERE sourceInsightId = :id ORDER BY createdAt ASC")
    suspend fun getDerivedTasksForIdea(id: String): List<InsightEntity>

    @Query("SELECT * FROM insight_items ORDER BY createdAt DESC")
    fun getAllInsightsFlow(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE kind = :kind ORDER BY createdAt DESC")
    fun getInsightsByKindFlow(kind: String): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE kind = 'ACTION' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun getActiveActionsFlow(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE kind = 'DECISION' ORDER BY createdAt DESC")
    fun getDecisionsFlow(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE kind = 'IDEA' ORDER BY createdAt DESC")
    fun getIdeasFlow(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE recordingId = :recordingId ORDER BY sourceOrder ASC")
    fun getInsightsByRecordingIdFlow(recordingId: Long): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insight_items WHERE recordingId = :recordingId ORDER BY sourceOrder ASC")
    suspend fun getInsightsByRecordingId(recordingId: Long): List<InsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<InsightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: InsightEntity)

    @Update
    suspend fun updateInsight(insight: InsightEntity)

    @Delete
    suspend fun deleteInsight(insight: InsightEntity)

    @Query("DELETE FROM insight_items WHERE recordingId = :recordingId")
    suspend fun deleteInsightsByRecordingId(recordingId: Long)

    @Query("UPDATE insight_items SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateActionStatus(id: String, status: String, completedAt: Long?)

    @Query("UPDATE insight_items SET status = 'ARCHIVED', archivedAt = :archivedAt WHERE kind = 'ACTION' AND status = 'COMPLETED'")
    suspend fun archiveCompletedActions(archivedAt: Long = System.currentTimeMillis())

    @Query("UPDATE insight_items SET status = 'COMPLETED', archivedAt = null, completedAt = :now WHERE kind = 'ACTION' AND status = 'ARCHIVED'")
    suspend fun unarchiveAllActions(now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM insight_items WHERE kind = 'ACTION' AND status = 'ARCHIVED'")
    fun getArchivedActionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM insight_items")
    suspend fun getInsightCount(): Int

    /** Auto-archive completed actions older than [cutoffMs]. Called on app startup. */
    @Query("UPDATE insight_items SET status = 'ARCHIVED', archivedAt = :now WHERE kind = 'ACTION' AND status = 'COMPLETED' AND completedAt IS NOT NULL AND completedAt < :cutoffMs")
    suspend fun autoArchiveStaleCompleted(cutoffMs: Long, now: Long = System.currentTimeMillis())

}
