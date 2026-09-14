package space.iamjustkrishna.srutam.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderEntityTest {

    private lateinit var database: AppDatabase
    private lateinit var reminderDao: ReminderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderDao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryUpcomingReminders() = runBlocking {
        val now = System.currentTimeMillis()
        val reminders = listOf(
            ReminderEntity(
                id = "rem_future_1",
                recordingId = 100L,
                recordingName = "Sprint Planning",
                title = "Meet with mobile team",
                eventTimeMs = now + 1000 * 60 * 60, // 1 hour future
                originalText = "In 1 hour",
                person = "Alice",
                location = "Room 402",
                type = ReminderType.MEETING
            ),
            ReminderEntity(
                id = "rem_future_2",
                recordingId = 100L,
                recordingName = "Sprint Planning",
                title = "Submit release notes",
                eventTimeMs = now + 1000 * 60 * 60 * 24, // 24 hours future
                originalText = "Tomorrow",
                type = ReminderType.DEADLINE
            )
        )

        reminderDao.insertReminders(reminders)

        val upcoming = reminderDao.getUpcomingRemindersFlow(now).first()
        assertEquals(2, upcoming.size)
        assertEquals("rem_future_1", upcoming[0].id)
        assertEquals("Meet with mobile team", upcoming[0].title)
        assertEquals("Alice", upcoming[0].person)
    }

    @Test
    fun updateReminderStatus_marksCompleted() = runBlocking {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = "rem_to_complete",
            recordingId = 200L,
            recordingName = "Client Call",
            title = "Follow up with client",
            eventTimeMs = now + 1000 * 60 * 30,
            originalText = "In 30 mins"
        )

        reminderDao.insertReminders(listOf(reminder))

        val initial = reminderDao.getReminderById("rem_to_complete")
        assertNotNull(initial)
        assertEquals(ReminderStatus.ACTIVE, initial?.status)

        reminderDao.updateReminderStatus("rem_to_complete", ReminderStatus.COMPLETED)

        val updated = reminderDao.getReminderById("rem_to_complete")
        assertEquals(ReminderStatus.COMPLETED, updated?.status)

        // Completed reminder should no longer appear in active upcoming flow
        val activeUpcoming = reminderDao.getUpcomingRemindersFlow(now).first()
        assertTrue(activeUpcoming.none { it.id == "rem_to_complete" })
    }

    @Test
    fun deleteRemindersByRecordingId_cleansUpReminders() = runBlocking {
        val now = System.currentTimeMillis()
        val reminders = listOf(
            ReminderEntity(
                id = "rem_note_1",
                recordingId = 300L,
                recordingName = "Note 300",
                title = "Task from note 300",
                eventTimeMs = now + 50000,
                originalText = "Soon"
            ),
            ReminderEntity(
                id = "rem_note_2",
                recordingId = 301L,
                recordingName = "Note 301",
                title = "Task from note 301",
                eventTimeMs = now + 60000,
                originalText = "Later"
            )
        )

        reminderDao.insertReminders(reminders)
        assertEquals(2, reminderDao.getAllActiveRemindersFlow().first().size)

        reminderDao.deleteRemindersByRecordingId(300L)

        val remaining = reminderDao.getAllActiveRemindersFlow().first()
        assertEquals(1, remaining.size)
        assertEquals("rem_note_2", remaining[0].id)
    }
}
