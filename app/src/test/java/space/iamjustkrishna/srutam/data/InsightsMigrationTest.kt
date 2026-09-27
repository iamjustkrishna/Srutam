package space.iamjustkrishna.srutam.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class InsightsMigrationTest {
    @Test fun populatedVersionSixMigratesWithoutLosingUserData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "insights-migration-fixture"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val schemaFile = listOf(
            File("schemas/space.iamjustkrishna.srutam.data.AppDatabase/6.json"),
            File("app/schemas/space.iamjustkrishna.srutam.data.AppDatabase/6.json")
        ).first { it.exists() }
        val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (i in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table (id,identity_hash) VALUES(42,?)", arrayOf(schema.getString("identityHash")))
            old.insertOrThrow("recordings", null, ContentValues().apply {
                put("id", 17L); put("timestamp", 1000L); put("audioFilePath", "preserve.m4a")
                put("duration", 900L); put("name", "Preserved note"); put("aiStatus", "READY"); put("isProcessing", 0)
                put("transcript", "Original transcript"); put("summary", "Original summary")
            })
            for ((id, status) in listOf("task" to "COMPLETED", "archive" to "ARCHIVED", "idea" to "OPEN")) {
                old.insertOrThrow("insight_items", null, ContentValues().apply {
                    put("id", id); put("recordingId", 17L); put("recordingName", "Preserved note")
                    put("kind", if (id == "idea") "IDEA" else "ACTION"); put("text", "Preserve $id")
                    put("status", status); put("createdAt", 1000L); put("sourceOrder", 0)
                    put("completedAt", 2000L); put("archivedAt", 3000L)
                })
            }
            for ((id, status) in listOf("active" to "ACTIVE", "history" to "COMPLETED")) {
                old.insertOrThrow("reminders", null, ContentValues().apply {
                    put("id", id); put("recordingId", 17L); put("recordingName", "Preserved note")
                    put("title", "Preserve $id"); put("eventTimeMs", 9000L); put("originalText", "May 20")
                    put("type", "REMINDER"); put("status", status); put("createdAt", 1000L)
                })
            }
            old.version = 6
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(INSIGHTS_MIGRATION_6_7).allowMainThreadQueries().build()
        try {
            // Opening performs Room's complete schema validation against version 7.
            assertEquals(7, upgraded.openHelper.writableDatabase.version)
            val note = upgraded.recordingDao().getRecordingById(17)!!
            assertEquals("Original transcript", note.transcript)
            assertEquals("preserve.m4a", note.audioFilePath)
            assertNull(note.recordedZoneId)
            assertEquals(3, upgraded.insightDao().getInsightsByRecordingId(17).size)
            assertEquals("ARCHIVED", upgraded.insightDao().getById("archive")!!.status)
            assertEquals(3000L, upgraded.insightDao().getById("archive")!!.archivedAt)
            val reminder = upgraded.reminderDao().getReminderById("active")!!
            assertEquals(9000L, reminder.eventTimeMs)
            assertEquals("May 20", reminder.originalText)
            assertFalse(reminder.notificationEnabled)
            assertTrue(reminder.needsReview && reminder.legacyReview)
            assertNull(reminder.confirmedAt)
            assertEquals("COMPLETED", upgraded.reminderDao().getReminderById("history")!!.status)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
