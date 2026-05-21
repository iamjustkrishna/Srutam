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

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int
}
