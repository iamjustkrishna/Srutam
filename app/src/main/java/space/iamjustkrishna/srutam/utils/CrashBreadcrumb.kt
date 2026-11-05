package space.iamjustkrishna.srutam.utils

import android.content.Context

/**
 * A crash can't be sent to Firebase while it happens, so the app writes a short note about it and
 * reports it on the next launch: which exception, and where in Srutam's own code. Never the
 * message text, which could hold something a person said.
 */
object CrashBreadcrumb {
    private const val PREFS = "srutam_crash_breadcrumb"
    private const val KEY_CLASS = "exception_class"
    private const val KEY_AT = "at"

    /** Call once at startup: remembers the next crash, then lets Android handle it as usual. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val (cls, at) = describe(error)
                // commit(), not apply(): the process is about to die.
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CLASS, cls).putString(KEY_AT, at).commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Sends the crash noted last time, if any, and clears it. */
    fun reportPrevious(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cls = prefs.getString(KEY_CLASS, null) ?: return
        val at = prefs.getString(KEY_AT, null).orEmpty()
        prefs.edit().clear().apply()
        Analytics.appCrash(context, cls, at)
    }

    /** Packages that are never Srutam's own code, so the first frame outside them is where the app was. */
    private val FRAMEWORK = listOf(
        "java.", "javax.", "android.", "androidx.", "kotlin.", "kotlinx.", "dalvik.", "com.android.",
        "com.google.", "okhttp3.", "okio.", "org.", "sun.", "libcore."
    )
    private const val APP_PREFIX = "space.iamjustkrishna.srutam."

    /**
     * The exception's class and the first frame in the app's own code, as "Class.method:line". Release
     * builds rename most classes (to things like "f6.a"), so the app's code is told apart from the
     * framework's by exclusion; the renamed frame can be read back with that release's mapping.txt.
     */
    internal fun describe(error: Throwable): Pair<String, String> {
        val root = generateSequence(error) { it.cause }.last()
        fun ownFrame(t: Throwable) = t.stackTrace.firstOrNull { f -> FRAMEWORK.none { f.className.startsWith(it) } }
        val frame = ownFrame(root) ?: ownFrame(error)
        val at = frame?.let { it.className.removePrefix(APP_PREFIX) + "." + it.methodName + ":" + it.lineNumber } ?: ""
        return root.javaClass.simpleName.take(60) to at.take(100)
    }
}
