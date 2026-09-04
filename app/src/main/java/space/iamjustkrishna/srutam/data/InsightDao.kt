package space.iamjustkrishna.srutam.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
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

    @Query("UPDATE insight_items SET status = 'COMPLETED', archivedAt = null WHERE kind = 'ACTION' AND status = 'ARCHIVED'")
    suspend fun unarchiveAllActions()

    @Query("SELECT COUNT(*) FROM insight_items WHERE kind = 'ACTION' AND status = 'ARCHIVED'")
    fun getArchivedActionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM insight_items")
    suspend fun getInsightCount(): Int
}
