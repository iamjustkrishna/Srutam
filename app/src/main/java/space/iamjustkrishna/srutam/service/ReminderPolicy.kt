package space.iamjustkrishna.srutam.service

import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus

interface ReminderAlarmService {
    /** Null means scheduled; a non-null message describes failure or inexact delivery. */
    fun schedule(reminder: ReminderEntity): String?
    fun cancel(reminder: ReminderEntity)
}

object ReminderPolicy {
    const val ADVANCE_MS = 15 * 60 * 1000L

    fun authorized(reminder: ReminderEntity): Boolean =
        reminder.status == ReminderStatus.ACTIVE && reminder.notificationEnabled &&
            reminder.confirmedAt != null && !reminder.needsReview &&
            reminder.timePrecision == "EXACT" && reminder.eventTimeMs != null

    fun canSchedule(reminder: ReminderEntity, now: Long): Boolean =
        authorized(reminder) && reminder.eventTimeMs!! > now

    fun canDeliver(
        reminder: ReminderEntity, revision: Int, trigger: Long,
        headsUp: Boolean, now: Long, sourceExists: Boolean
    ): Boolean {
        if (!sourceExists || !authorized(reminder) || reminder.scheduleRevision != revision) return false
        if (headsUp && !reminder.advanceNotification) return false
        val expected = reminder.eventTimeMs!! - if (headsUp) ADVANCE_MS else 0
        return trigger == expected && now >= expected && now <= reminder.eventTimeMs + 60 * 60 * 1000L
    }
}
