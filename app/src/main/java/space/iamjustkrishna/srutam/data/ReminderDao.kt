package space.iamjustkrishna.srutam.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND eventTimeMs >= :nowMs ORDER BY eventTimeMs ASC")
    fun getUpcomingRemindersFlow(nowMs: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY eventTimeMs ASC")
    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE recordingId = :recordingId ORDER BY eventTimeMs ASC")
    fun getRemindersByRecordingIdFlow(recordingId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE recordingId = :recordingId")
    suspend fun getRemindersByRecordingId(recordingId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateReminderStatus(id: String, status: String)

    @Query("DELETE FROM reminders WHERE recordingId = :recordingId")
    suspend fun deleteRemindersByRecordingId(recordingId: Long)
}
