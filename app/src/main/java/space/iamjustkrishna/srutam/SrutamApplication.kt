package space.iamjustkrishna.srutam

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.service.InsightLifecycleManager
import space.iamjustkrishna.srutam.service.InsightParityBackfill
import space.iamjustkrishna.srutam.utils.AudioFileReader

class SrutamApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        AudioFileReader.init(this)

        // Auto-archive completed tasks and reconcile alarms while retaining saved history.
        appScope.launch {
            try {
                InsightLifecycleManager.runStartupCleanup(
                    insightDao = database.insightDao(),
                    reminderDao = database.reminderDao(),
                    context = this@SrutamApplication
                )
                // Recovers notes whose insights and reminders were stranded in the
                // cloud by the old silent-failure sync. Runs at most once.
                InsightParityBackfill.runIfNeeded(
                    context = this@SrutamApplication,
                    recordingDao = database.recordingDao()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // Opening Insights retries reconciliation and displays any persistent error.
                Log.w("SrutamApplication", "Insights reconciliation will retry when opened", failure)
            }
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
