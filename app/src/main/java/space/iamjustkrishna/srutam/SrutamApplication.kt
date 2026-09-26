package space.iamjustkrishna.srutam

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.service.InsightLifecycleManager
import space.iamjustkrishna.srutam.utils.AudioFileReader

class SrutamApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        AudioFileReader.init(this)

        // Run lifecycle cleanup: auto-archive stale tasks, dismiss overdue reminders, purge old data
        appScope.launch {
            InsightLifecycleManager.runStartupCleanup(
                insightDao = database.insightDao(),
                reminderDao = database.reminderDao(),
                context = this@SrutamApplication
            )
        }
    }

    companion object {
        @Volatile
        private var instance: SrutamApplication? = null

        fun getInstance(): SrutamApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
