package space.iamjustkrishna.srutam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Recording::class, InsightEntity::class, ReminderEntity::class, AiQueryCache::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun insightDao(): InsightDao
    abstract fun reminderDao(): ReminderDao
    abstract fun aiQueryCacheDao(): AiQueryCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "srutam_database"
                )
                    .fallbackToDestructiveMigration() // Allow database reset during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
