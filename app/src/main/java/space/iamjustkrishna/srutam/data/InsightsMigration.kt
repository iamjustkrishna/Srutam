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
