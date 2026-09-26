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

    /** Recent completed/dismissed reminders for history view. */
    @Query("SELECT * FROM reminders WHERE status IN ('COMPLETED', 'DISMISSED') ORDER BY eventTimeMs DESC LIMIT 20")
    fun getPastRemindersFlow(): Flow<List<ReminderEntity>>

    /** Auto-dismiss overdue ACTIVE reminders whose event time has passed + grace period. */
    @Query("UPDATE reminders SET status = 'DISMISSED' WHERE status = 'ACTIVE' AND eventTimeMs < :cutoffMs")
    suspend fun autoDismissOverdue(cutoffMs: Long)

    /** Get all ACTIVE reminders (non-flow, for rescheduling on boot). */
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND eventTimeMs > :nowMs")
    suspend fun getFutureActiveReminders(nowMs: Long): List<ReminderEntity>

    /** Hard-delete dismissed/completed reminders older than [cutoffMs]. */
    @Query("DELETE FROM reminders WHERE status IN ('COMPLETED', 'DISMISSED') AND eventTimeMs < :cutoffMs")
    suspend fun purgeOldReminders(cutoffMs: Long)
}
