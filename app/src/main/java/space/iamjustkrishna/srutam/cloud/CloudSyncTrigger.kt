package space.iamjustkrishna.srutam.cloud

import android.content.Context

/**
 * Wakes the cloud sync worker after a local insight or reminder change.
 *
 * Until this existed, only [space.iamjustkrishna.srutam.service.AiProcessingWorker]
 * ever marked a note as needing upload, and only when AI processing finished a
 * *newly recorded* note. Every later edit - creating a next step, converting an
 * idea, confirming or rescheduling a reminder, archiving an insight - changed
 * local Room rows that `getPendingSyncRecordings()` would never look at again,
 * so the change stayed on the device permanently.
 *
 * Marking the owning note dirty happens inside the repository's own transaction
 * (so it is atomic with the mutation); this interface only wakes the worker
 * afterwards. It is an interface rather than a direct CloudSyncManager call so
 * InsightsRepository stays constructible without a Context in unit tests.
 */
fun interface CloudSyncTrigger {

    /**
     * Requests a sync pass. Deliberately takes no id: CloudSyncWorker scans every
     * note left PENDING, so one wake-up drains all of them. WorkManager's
     * unique-work REPLACE policy coalesces a burst of edits into a single run.
     */
    fun requestSync()

    companion object {
        /** No-op, for tests and for callers that have no Context. */
        val None = CloudSyncTrigger { }
    }
}

/** Production trigger: hands off to [CloudSyncManager], which no-ops when signed out. */
class WorkManagerSyncTrigger(context: Context) : CloudSyncTrigger {
    private val appContext = context.applicationContext
    override fun requestSync() = CloudSyncManager.enqueueSync(appContext)
}
