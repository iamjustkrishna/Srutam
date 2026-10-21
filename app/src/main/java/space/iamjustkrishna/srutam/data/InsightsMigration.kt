package space.iamjustkrishna.srutam.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val INSIGHTS_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recordings ADD COLUMN recordedZoneId TEXT")
        db.execSQL("ALTER TABLE recordings ADD COLUMN insightsImported INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE insight_items ADD COLUMN sourceInsightId TEXT")
        db.execSQL("ALTER TABLE insight_items ADD COLUMN sourceReminderId TEXT")
        db.execSQL("ALTER TABLE insight_items ADD COLUMN extractionFingerprint TEXT")
        // Rebuild only to make eventTimeMs nullable. Preserve every old row and ID.
        db.execSQL("""
            CREATE TABLE reminders_v7 (
                id TEXT NOT NULL PRIMARY KEY, recordingId INTEGER NOT NULL,
                recordingName TEXT NOT NULL, title TEXT NOT NULL, eventTimeMs INTEGER,
                originalText TEXT NOT NULL, person TEXT, location TEXT, type TEXT NOT NULL,
                status TEXT NOT NULL, createdAt INTEGER NOT NULL,
                timePrecision TEXT NOT NULL DEFAULT 'UNKNOWN', localDate TEXT, localTime TEXT,
                zoneId TEXT, zoneInferred INTEGER NOT NULL DEFAULT 1,
                notificationEnabled INTEGER NOT NULL DEFAULT 0, confirmedAt INTEGER,
                needsReview INTEGER NOT NULL DEFAULT 1, advanceNotification INTEGER NOT NULL DEFAULT 0,
                scheduleRevision INTEGER NOT NULL DEFAULT 0, legacyReview INTEGER NOT NULL DEFAULT 0,
                extractionFingerprint TEXT, linkedTaskId TEXT, scheduleError TEXT
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO reminders_v7
            (id, recordingId, recordingName, title, eventTimeMs, originalText, person,
             location, type, status, createdAt, needsReview, legacyReview)
            SELECT id, recordingId, recordingName, title, eventTimeMs, originalText, person,
             location, type, status, createdAt,
             CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END,
             CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END FROM reminders
        """.trimIndent())
        db.execSQL("DROP TABLE reminders")
        db.execSQL("ALTER TABLE reminders_v7 RENAME TO reminders")
        db.execSQL("CREATE INDEX index_reminders_recordingId ON reminders(recordingId)")
        db.execSQL("CREATE INDEX index_reminders_eventTimeMs ON reminders(eventTimeMs)")
        db.execSQL("CREATE INDEX index_reminders_status ON reminders(status)")
        db.execSQL("""
            CREATE TABLE extraction_suppression (
                recordingId INTEGER NOT NULL, kind TEXT NOT NULL, fingerprint TEXT NOT NULL,
                PRIMARY KEY(recordingId, kind, fingerprint)
            )
        """.trimIndent())
    }
}

/** True when [table] already has a column named [column]. */
private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        if (nameIndex < 0) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}

/**
 * 8 -> 9: notes get a stable [Recording.noteUuid] (the autoincrement id restarts after a
 * reinstall, so it can't match a note to its cloud copy or its audio file), plus the audio
 * hash, AI topics, a last-updated time and where the content came from.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("recordings", "noteUuid", "noteUuid TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("recordings", "audioHash", "audioHash TEXT")
        db.addColumnIfMissing("recordings", "topics", "topics TEXT")
        db.addColumnIfMissing("recordings", "updatedAt", "updatedAt INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("recordings", "contentSource", "contentSource TEXT NOT NULL DEFAULT 'AI'")
        // A random version-4 UUID per existing note, in the usual 8-4-4-4-12 form.
        db.execSQL(
            """
            UPDATE recordings SET noteUuid =
                lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
                substr(lower(hex(randomblob(2))), 2) || '-' ||
                substr('89ab', 1 + (abs(random()) % 4), 1) ||
                substr(lower(hex(randomblob(2))), 2) || '-' || lower(hex(randomblob(6)))
            WHERE noteUuid = ''
            """.trimIndent()
        )
        db.execSQL("UPDATE recordings SET updatedAt = timestamp WHERE updatedAt = 0")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recordings_noteUuid ON recordings(noteUuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recordings_audioHash ON recordings(audioHash)")
    }
}

private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
    if (!hasColumn(table, column)) {
        execSQL("ALTER TABLE `$table` ADD COLUMN $definition")
    }
}

/**
 * v8 adds the cloud-sync columns to `recordings`.
 *
 * These columns were briefly added to [INSIGHTS_MIGRATION_6_7] while the schema version stayed at 7.
 * That would have left every user upgrading from the released 2.3.1 (already on v7, without the
 * columns) with a database Room refuses to open, because no migration runs when the version is
 * unchanged and the identity hash no longer matches. 6->7 is therefore back to exactly what 2.3.1
 * shipped, and the new columns live here.
 *
 * The adds are idempotent so databases created by the interim branch builds - which already carry
 * these columns at v7 - also upgrade cleanly instead of failing on "duplicate column name".
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("recordings", "syncStatus", "syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'")
        db.addColumnIfMissing("recordings", "isPrivate", "isPrivate INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("recordings", "cloudId", "cloudId TEXT")
        db.addColumnIfMissing("recordings", "lastSyncedAt", "lastSyncedAt INTEGER")
    }
}
