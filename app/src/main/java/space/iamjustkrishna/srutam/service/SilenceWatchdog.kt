package space.iamjustkrishna.srutam.service

/** Milliseconds from a clock that only moves forward, shared by everything that feeds the watchdog. */
internal fun monotonicMs(): Long = System.nanoTime() / 1_000_000

/**
 * Notices a recording that nobody is speaking into, such as one started by mistake and forgotten. It
 * never decides anything itself: it says when to ask "still recording?" and, if nobody answers, when to
 * stop and save. Nothing is ever deleted. Times are plain milliseconds so tests can drive it.
 *
 * - Asks after [quietLimitMs] without speech, or after the shorter [neverSpokeLimitMs] if no speech has
 *   been heard at all since the recording began (a button pressed by mistake).
 * - [onKeep] (or any speech, or resuming from pause) starts the count again.
 * - Stops [answerMs] after asking if there was no answer. Paused time never counts.
 */
internal class SilenceWatchdog(
    private val neverSpokeLimitMs: Long = 3 * MINUTE,
    private val quietLimitMs: Long = 10 * MINUTE,
    private val answerMs: Long = 5 * MINUTE
) {
    enum class Action { NONE, ASK, STOP }

    private var startedAt = 0L
    private var lastSpeechAt: Long? = null
    private var askedAt: Long? = null
    private var paused = false

    fun start(nowMs: Long) {
        startedAt = nowMs
        lastSpeechAt = null
        askedAt = null
        paused = false
    }

    /** Speech was heard at [atMs]. Returns true if that cancelled a pending "still recording?" question. */
    fun onSpeech(atMs: Long): Boolean {
        val previous = lastSpeechAt
        if (previous != null && atMs <= previous) return false
        lastSpeechAt = atMs
        val asked = askedAt
        if (asked != null && atMs >= asked) {
            askedAt = null
            return true
        }
        return false
    }

    /** The user said to keep going. */
    fun onKeep(nowMs: Long) {
        lastSpeechAt = nowMs
        askedAt = null
    }

    fun onPaused() {
        paused = true
    }

    /** Resuming means someone is there, so the quiet count starts again. */
    fun onResumed(nowMs: Long) {
        paused = false
        onKeep(nowMs)
    }

    fun check(nowMs: Long): Action {
        if (paused) return Action.NONE
        val asked = askedAt
        if (asked != null) {
            return if (nowMs - asked >= answerMs) Action.STOP else Action.NONE
        }
        val heard = lastSpeechAt
        val quietSince = heard ?: startedAt
        val limit = if (heard == null) neverSpokeLimitMs else quietLimitMs
        if (nowMs - quietSince >= limit) {
            askedAt = nowMs
            return Action.ASK
        }
        return Action.NONE
    }

    /** Whole minutes of quiet so far, for the wording of the notifications. */
    fun quietMinutes(nowMs: Long): Long = (nowMs - (lastSpeechAt ?: startedAt)) / MINUTE

    companion object {
        const val MINUTE = 60_000L
    }
}
