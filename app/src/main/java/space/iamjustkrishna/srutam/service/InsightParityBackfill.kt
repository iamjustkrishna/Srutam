package space.iamjustkrishna.srutam.service

import android.content.Context
import android.util.Log
import space.iamjustkrishna.srutam.cloud.CloudSyncManager
import space.iamjustkrishna.srutam.data.RecordingDao
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * Re-uploads notes stranded by the pre-parity sync behaviour. Runs at most once.
 *
 * Two bugs combined to leave cloud data permanently incomplete:
 *
 *  1. `uploadNote()` discarded the response of every child upload, so a missing
 *     table or column returned 404/400 in silence and the note was still marked
 *     SYNCED - with none of its action items, insights or reminders attached.
 *  2. Only `getPendingSyncRecordings()` is ever uploaded, and nothing marked an
 *     already-SYNCED note dirty again, so those notes were never retried.
 *
 * Fixing both only helps notes changed from now on. This backfill is what
 * recovers the existing ones: it marks every SYNCED note dirty a single time so
 * the normal worker re-sends it through the idempotent upserts keyed by
 * `client_insight_id` / `client_reminder_id`. Re-sending is safe precisely
 * because those upserts preserve server-side state such as an agent's
 * completion of an action item.
 *
 * Deliberately NOT a Room migration: this changes rows, not schema, and Room was
 * only just bumped to v8 to recover from a version collision that crashed every
 * existing install. That history does not need another version added to it.
 */
object InsightParityBackfill {

    private const val TAG = "InsightParityBackfill"

    /**
     * Marks already-synced notes for re-upload, once per install.
     *
     * No-ops when the user is signed out: there is nothing to re-upload, and
     * running it then would burn the one-shot flag before it could do any good.
     * It is therefore left unset so the backfill still happens after sign-in.
     */
    suspend fun runIfNeeded(context: Context, recordingDao: RecordingDao) {
        if (AppPreferences.isInsightParityBackfillDone(context)) return
        if (!AppPreferences.isCloudSignedIn(context)) return

        val marked = runCatching { recordingDao.markSyncedForReupload() }
            .onFailure { Log.w(TAG, "Parity backfill could not mark notes for re-upload", it) }
            .getOrNull() ?: return

        AppPreferences.setInsightParityBackfillDone(context, true)
        Log.i(TAG, "Parity backfill marked $marked note(s) for re-upload")

        if (marked > 0) {
            // The worker drains PENDING notes in passes and reschedules itself on
            // failure, so a large library spreads over several runs rather than
            // firing one request per note in a single burst.
            CloudSyncManager.enqueueSync(context)
        }
    }
}
