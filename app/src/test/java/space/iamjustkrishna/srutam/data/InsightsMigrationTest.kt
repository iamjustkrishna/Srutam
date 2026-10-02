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

    /** Reads an exported schema, so fixtures are the real shipped schemas rather than hand-written SQL. */
    private fun schema(version: Int): JSONObject {
        val file = listOf(
            File("schemas/space.iamjustkrishna.srutam.data.AppDatabase/$version.json"),
            File("app/schemas/space.iamjustkrishna.srutam.data.AppDatabase/$version.json")
        ).first { it.exists() }
        return JSONObject(file.readText()).getJSONObject("database")
    }

    /** Recreates a database exactly as the given shipped version left it, including Room's identity hash. */
    private fun createLegacyDatabase(context: Context, name: String, version: Int): File {
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val db = schema(version)
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = db.getJSONArray("entities")
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
            old.execSQL(
                "INSERT INTO room_master_table (id,identity_hash) VALUES(42,?)",
                arrayOf(db.getString("identityHash"))
            )
            old.version = version
        }
        return path
    }

    private fun openWithMigrations(context: Context, name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(INSIGHTS_MIGRATION_6_7, MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

    @Test fun populatedVersionSixMigratesWithoutLosingUserData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "insights-migration-fixture"
        val path = createLegacyDatabase(context, name, 6)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
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
        val upgraded = openWithMigrations(context, name)
        try {
            // Opening performs Room's complete schema validation against the current version.
            assertEquals(8, upgraded.openHelper.writableDatabase.version)
            val note = upgraded.recordingDao().getRecordingById(17)!!
            assertEquals("Original transcript", note.transcript)
            assertEquals("preserve.m4a", note.audioFilePath)
            assertNull(note.recordedZoneId)
            assertEquals("NOT_SYNCED", note.syncStatus)
            assertFalse(note.isPrivate)
            assertNull(note.cloudId)
            assertNull(note.lastSyncedAt)
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

    /**
     * The upgrade path every already-released 2.3.1 user takes.
     *
     * 2.3.1 shipped schema version 7 without the cloud-sync columns. Those columns were briefly added
     * to the 6->7 migration while the version stayed at 7, which would have made Room refuse to open
     * an existing database (same version, different identity hash) and crash the app on launch for
     * every upgrading user. This test pins the fix: v7-as-shipped must migrate to v8 with the new
     * columns added and every existing row intact.
     */
    @Test fun releasedVersionSevenMigratesToVersionEightKeepingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "cloud-sync-migration-fixture"
        val path = createLegacyDatabase(context, name, 7)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
            assertFalse(
                "fixture must be the v7 that 2.3.1 shipped, i.e. without the sync columns",
                old.rawQuery("PRAGMA table_info(recordings)", null).use { cursor ->
                    generateSequence { if (cursor.moveToNext()) cursor.getString(1) else null }
                        .contains("syncStatus")
                }
            )
            old.insertOrThrow("recordings", null, ContentValues().apply {
                put("id", 21L); put("timestamp", 1700000000000L); put("audioFilePath", "board.m4a")
                put("duration", 5000L); put("name", "Board meeting"); put("aiStatus", "COMPLETED")
                put("isProcessing", 0); put("transcript", "Confidential transcript")
                put("summary", "Original summary"); put("insightsImported", 0)
            })
            old.version = 7
        }
        val upgraded = openWithMigrations(context, name)
        try {
            assertEquals(8, upgraded.openHelper.writableDatabase.version)
            val note = upgraded.recordingDao().getRecordingById(21)!!
            assertEquals("Confidential transcript", note.transcript)
            assertEquals("Board meeting", note.name)
            assertEquals("board.m4a", note.audioFilePath)
            // New columns arrive with sane defaults, so nothing is silently treated as already synced.
            assertEquals("NOT_SYNCED", note.syncStatus)
            assertFalse(note.isPrivate)
            assertNull(note.cloudId)
            assertNull(note.lastSyncedAt)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * Devices that ran an interim cloud-sync branch build already have the sync columns at version 7.
     * The migration must skip those adds instead of failing on "duplicate column name".
     */
    @Test fun versionSevenThatAlreadyHasSyncColumnsStillMigrates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "interim-build-migration-fixture"
        val path = createLegacyDatabase(context, name, 7)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
            old.execSQL("ALTER TABLE recordings ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'")
            old.execSQL("ALTER TABLE recordings ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
            old.execSQL("ALTER TABLE recordings ADD COLUMN cloudId TEXT")
            old.execSQL("ALTER TABLE recordings ADD COLUMN lastSyncedAt INTEGER")
            old.insertOrThrow("recordings", null, ContentValues().apply {
                put("id", 33L); put("timestamp", 1700000002000L); put("audioFilePath", "interim.m4a")
                put("duration", 1500L); put("name", "Interim build note"); put("aiStatus", "COMPLETED")
                put("isProcessing", 0); put("insightsImported", 0)
                put("syncStatus", "SYNCED"); put("cloudId", "cloud-abc")
            })
            old.version = 7
        }
        val upgraded = openWithMigrations(context, name)
        try {
            assertEquals(8, upgraded.openHelper.writableDatabase.version)
            val note = upgraded.recordingDao().getRecordingById(33)!!
            assertEquals("SYNCED", note.syncStatus)
            assertEquals("cloud-abc", note.cloudId)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
