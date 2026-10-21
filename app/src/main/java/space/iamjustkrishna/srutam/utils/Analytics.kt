package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper over Firebase Analytics. It only does anything when the app is actually wired to
 * Firebase, that is when `app/google-services.json` is present at build time so FirebaseApp
 * initializes. Without it, every call here is a safe no-op, so the app builds and runs for anyone
 * who does not have the config file (CI, a fresh clone).
 *
 * Event and parameter names follow GA4 rules: snake_case, event names up to 40 characters,
 * parameter names up to 40 characters, string values up to 100 characters.
 *
 * Privacy: only product-usage signals are logged here, never transcript text, note content,
 * file paths, or anything a user said. If you add an event, keep it to counts and short enums.
 */
object Analytics {
    private const val TAG = "Analytics"

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    @Volatile
    private var tried = false

    /** Call once from Application.onCreate. Honors the user's opt-out preference. */
    fun init(context: Context) {
        val fa = instance(context) ?: return
        runCatching { fa.setAnalyticsCollectionEnabled(AppPreferences.isAnalyticsEnabled(context)) }
    }

    /** Lets Settings turn collection on or off at runtime. */
    fun setEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setAnalyticsEnabled(context, enabled)
        runCatching { instance(context)?.setAnalyticsCollectionEnabled(enabled) }
    }

    private fun instance(context: Context): FirebaseAnalytics? {
        analytics?.let { return it }
        if (tried) return analytics
        synchronized(this) {
            if (tried) return analytics
            tried = true
            analytics = runCatching {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAnalytics.getInstance(context.applicationContext)
                } else {
                    Log.d(TAG, "Firebase not configured (no google-services.json); analytics disabled")
                    null
                }
            }.getOrNull()
        }
        return analytics
    }

    /** Generic escape hatch. Prefer the named helpers below so the event taxonomy stays consistent. */
    fun log(context: Context, event: String, params: Map<String, Any>? = null) {
        val fa = instance(context) ?: return
        runCatching {
            val bundle = Bundle()
            params?.forEach { (k, v) ->
                when (v) {
                    is Int -> bundle.putLong(k, v.toLong())
                    is Long -> bundle.putLong(k, v)
                    is Double -> bundle.putDouble(k, v)
                    is Boolean -> bundle.putString(k, if (v) "true" else "false")
                    else -> bundle.putString(k, v.toString().take(100))
                }
            }
            fa.logEvent(event, bundle)
        }
    }

    // ---- The recording funnel: start -> saved -> transcript -> insights ----

    /** [source] is where the recording was started from (notification, dock, tile, volume, in_app) when known. */
    fun recordingStarted(context: Context, source: String? = null) =
        log(context, EVENT_RECORDING_STARTED, source?.let { mapOf(PARAM_SOURCE to it) })

    fun recordingSaved(context: Context, durationMs: Long) =
        log(context, EVENT_RECORDING_SAVED, mapOf(PARAM_DURATION_BUCKET to durationBucket(durationMs)))

    fun recordingDiscarded(context: Context) = log(context, EVENT_RECORDING_DISCARDED)

    /** The live transcript was stored on the note. */
    fun transcriptStored(context: Context) = log(context, EVENT_TRANSCRIPT_STORED)

    fun aiInsightsReady(context: Context) = log(context, EVENT_AI_INSIGHTS_READY)

    fun aiProcessingFailed(context: Context, reason: String) =
        log(context, EVENT_AI_PROCESSING_FAILED, mapOf(PARAM_REASON to reason.take(100)))

    /** Nobody answered the "still recording?" prompt and the note was saved automatically. */
    fun silenceAutoSaved(context: Context) = log(context, EVENT_SILENCE_AUTO_SAVED)

    // ---- Note actions ----

    fun noteOpened(context: Context) = log(context, EVENT_NOTE_OPENED)

    fun noteRenamed(context: Context) = log(context, EVENT_NOTE_RENAMED)

    /** [keptContent] true when only the audio was deleted and the insights/tasks stayed. */
    fun noteDeleted(context: Context, keptContent: Boolean) =
        log(context, EVENT_NOTE_DELETED, mapOf(PARAM_KEPT_CONTENT to keptContent))

    fun aiRetried(context: Context) = log(context, EVENT_AI_RETRIED)

    // ---- Onboarding funnel: step viewed -> step result -> completed ----

    /** [step] is a short id: intro_1, intro_2, intro_3, dock. */
    fun onboardingStepViewed(context: Context, step: String) =
        log(context, EVENT_ONBOARDING_STEP_VIEWED, mapOf(PARAM_STEP to step))

    /** [result] is granted, skipped, denied or next. [timeOnStepMs] is how long the step was on screen. */
    fun onboardingStepResult(context: Context, step: String, result: String, timeOnStepMs: Long) =
        log(
            context,
            EVENT_ONBOARDING_STEP_RESULT,
            mapOf(PARAM_STEP to step, PARAM_RESULT to result, PARAM_TIME_ON_STEP_MS to timeOnStepMs)
        )

    fun onboardingCompleted(context: Context) = log(context, EVENT_ONBOARDING_COMPLETED)

    /** [permission] is mic, notifications, storage or overlay; [trigger] is what asked for it. */
    fun permissionResult(context: Context, permission: String, granted: Boolean, trigger: String) =
        log(
            context,
            EVENT_PERMISSION_RESULT,
            mapOf(
                PARAM_PERMISSION to permission,
                PARAM_RESULT to if (granted) RESULT_GRANTED else RESULT_DENIED,
                PARAM_TRIGGER to trigger
            )
        )

    fun sampleNoteOpened(context: Context) = log(context, EVENT_SAMPLE_NOTE_OPENED)

    // ---- Settings ----

    /** A Settings switch changed. [setting] is a short key, [enabled] the new value. */
    fun settingChanged(context: Context, setting: String, enabled: Boolean) =
        log(context, EVENT_SETTING_CHANGED, mapOf(PARAM_SETTING to setting, PARAM_ENABLED to enabled))

    /**
     * Durable traits for segmenting users in the console (which features they keep on). Set at startup.
     * Only on/off flags, never content. Register each name in Analytics > Custom definitions to chart it.
     */
    fun setUserProperties(context: Context) {
        val fa = instance(context) ?: return
        runCatching {
            fa.setUserProperty("auto_ai_on", AppPreferences.isAutoAiEnabled(context).toString())
            fa.setUserProperty("dock_on", AppPreferences.isFloatingDockEnabled(context).toString())
            fa.setUserProperty("quick_notif_on", AppPreferences.isPersistentNotificationEnabled(context).toString())
            fa.setUserProperty("ask_name_on", AppPreferences.isAskNameAfterRecording(context).toString())
        }
    }

    /** A coarse bucket instead of the exact seconds, to keep the data about behavior, not content. */
    internal fun durationBucket(durationMs: Long): String {
        val s = durationMs / 1000
        return when {
            s < 15 -> "0_15s"
            s < 60 -> "15_60s"
            s < 300 -> "1_5m"
            s < 900 -> "5_15m"
            s < 3600 -> "15_60m"
            else -> "60m_plus"
        }
    }

    const val EVENT_RECORDING_STARTED = "recording_started"
    const val EVENT_RECORDING_SAVED = "recording_saved"
    const val EVENT_RECORDING_DISCARDED = "recording_discarded"
    const val EVENT_TRANSCRIPT_STORED = "transcript_stored"
    const val EVENT_AI_INSIGHTS_READY = "ai_insights_ready"
    const val EVENT_AI_PROCESSING_FAILED = "ai_processing_failed"
    const val EVENT_SILENCE_AUTO_SAVED = "silence_auto_saved"
    const val EVENT_NOTE_OPENED = "note_opened"
    const val EVENT_NOTE_RENAMED = "note_renamed"
    const val EVENT_NOTE_DELETED = "note_deleted"
    const val EVENT_AI_RETRIED = "ai_retried"
    const val EVENT_SETTING_CHANGED = "setting_changed"

    const val EVENT_ONBOARDING_STEP_VIEWED = "onboarding_step_viewed"
    const val EVENT_ONBOARDING_STEP_RESULT = "onboarding_step_result"
    const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
    const val EVENT_PERMISSION_RESULT = "permission_result"
    const val EVENT_SAMPLE_NOTE_OPENED = "sample_note_opened"

    const val PARAM_SOURCE = "source"
    const val PARAM_DURATION_BUCKET = "duration_bucket"
    const val PARAM_REASON = "reason"
    const val PARAM_KEPT_CONTENT = "kept_content"
    const val PARAM_SETTING = "setting"
    const val PARAM_ENABLED = "enabled"
    const val PARAM_STEP = "step"
    const val PARAM_RESULT = "result"
    const val PARAM_TIME_ON_STEP_MS = "time_on_step_ms"
    const val PARAM_PERMISSION = "permission"
    const val PARAM_TRIGGER = "trigger"

    const val RESULT_GRANTED = "granted"
    const val RESULT_DENIED = "denied"
    const val RESULT_SKIPPED = "skipped"
    const val RESULT_NEXT = "next"
}
