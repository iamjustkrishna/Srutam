package space.iamjustkrishna.srutam.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "extraction_suppression", primaryKeys = ["recordingId", "kind", "fingerprint"])
data class ExtractionSuppression(val recordingId: Long, val kind: String, val fingerprint: String)

@Dao
interface ExtractionSuppressionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: ExtractionSuppression)

    @Query("SELECT EXISTS(SELECT 1 FROM extraction_suppression WHERE recordingId = :recordingId AND kind = :kind AND fingerprint = :fingerprint)")
    suspend fun contains(recordingId: Long, kind: String, fingerprint: String): Boolean

    @Query("DELETE FROM extraction_suppression WHERE recordingId = :recordingId")
    suspend fun deleteForRecording(recordingId: Long)
}
