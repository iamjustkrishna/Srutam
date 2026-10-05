package space.iamjustkrishna.srutam.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("UPDATE recordings SET insightsImported = 1 WHERE id = :id")
    suspend fun markInsightsImported(id: Long)

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :recordingId")
    suspend fun getRecordingById(recordingId: Long): Recording?

    @Query("SELECT * FROM recordings WHERE id = :recordingId")
    fun getRecordingByIdFlow(recordingId: Long): Flow<Recording?>

    @Query("SELECT audioFilePath FROM recordings")
    suspend fun getAllRecordingPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)

    @Query("DELETE FROM recordings WHERE id = :recordingId")
    suspend fun deleteRecordingById(recordingId: Long)

    @Query("SELECT * FROM recordings WHERE audioFilePath = :audioFilePath")
    suspend fun getRecordingByPath(audioFilePath: String): Recording?

    /** Sets the transcript only if there is none yet; atomic, so it never overwrites a concurrent update. */
    @Query("UPDATE recordings SET transcript = :text WHERE id = :id AND (transcript IS NULL OR transcript = '')")
    suspend fun setTranscriptIfBlank(id: Long, text: String): Int

    /** Points a note at its renamed audio file. Returns how many notes were moved (0 or 1). */
    @Query("UPDATE recordings SET audioFilePath = :newPath, name = :name WHERE audioFilePath = :oldPath")
    suspend fun movePath(oldPath: String, newPath: String, name: String): Int

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int

    @Query("SELECT * FROM recordings WHERE syncStatus != 'SYNCED' AND aiStatus = 'READY'")
    suspend fun getPendingSyncRecordings(): List<Recording>

    @Query("UPDATE recordings SET syncStatus = :status, cloudId = :cloudId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, cloudId: String?, syncedAt: Long)

    @Query("UPDATE recordings SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun updatePrivacy(id: Long, isPrivate: Boolean)

    @Query("UPDATE recordings SET syncStatus = 'PENDING' WHERE id = :id")
    suspend fun markForSync(id: Long)

    @Query("UPDATE recordings SET syncStatus = 'PENDING' WHERE syncStatus != 'SYNCED' AND aiStatus = 'READY'")
    suspend fun markAllUnsyncedForSync()

    /**
     * Marks ALREADY-SYNCED notes dirty again. Unlike [markAllUnsyncedForSync] this
     * deliberately targets `syncStatus = 'SYNCED'`, because those are exactly the
     * notes stranded by the old behaviour: their insights and reminders either
     * never uploaded or uploaded without the fields migration 08 added, and
     * nothing would ever re-send them. Used once by the parity backfill.
     */
    @Query("UPDATE recordings SET syncStatus = 'PENDING' WHERE syncStatus = 'SYNCED' AND aiStatus = 'READY'")
    suspend fun markSyncedForReupload(): Int
}
