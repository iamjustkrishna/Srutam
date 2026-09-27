package space.iamjustkrishna.srutam.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC, id")
    fun getAllFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

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

    /** Recent completed/dismissed reminders for history view. */
    @Query("SELECT * FROM reminders WHERE status IN ('COMPLETED', 'DISMISSED', 'EXPIRED') ORDER BY eventTimeMs DESC")
    fun getPastRemindersFlow(): Flow<List<ReminderEntity>>

    /** Get all ACTIVE reminders (non-flow, for rescheduling on boot). */
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND eventTimeMs > :nowMs")
    suspend fun getFutureActiveReminders(nowMs: Long): List<ReminderEntity>

}
