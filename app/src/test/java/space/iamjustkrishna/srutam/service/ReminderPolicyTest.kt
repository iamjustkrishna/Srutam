package space.iamjustkrishna.srutam.service

import org.junit.Assert.*
import org.junit.Test
import space.iamjustkrishna.srutam.data.*

class ReminderPolicyTest {
    private val now = 1_800_000_000_000L
    private val confirmed = ReminderEntity(
        id = "confirmed", recordingId = 1, title = "Call", originalText = "tomorrow at 3pm",
        eventTimeMs = now + 3_600_000, notificationEnabled = true, needsReview = false,
        confirmedAt = now, timePrecision = "EXACT", scheduleRevision = 2
    )

    @Test fun exactAiSuggestionIsNotAuthorization() {
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(notificationEnabled = false, confirmedAt = null, needsReview = true), now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(confirmedAt = null), now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(needsReview = true), now))
    }

    @Test fun onlyActiveConfirmedFutureExactTimeSchedules() {
        assertTrue(ReminderPolicy.canSchedule(confirmed, now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(status = ReminderStatus.DISMISSED), now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(eventTimeMs = now - 1), now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(timePrecision = "DATE_ONLY"), now))
        assertFalse(ReminderPolicy.canSchedule(confirmed.copy(eventTimeMs = null), now))
    }

    @Test fun categoryDoesNotOverrideUserChoice() {
        assertTrue(ReminderPolicy.canSchedule(confirmed.copy(type = ReminderType.MILESTONE), now))
    }

    @Test fun staleDisabledOrOrphanedBroadcastCannotDeliver() {
        val due = confirmed.eventTimeMs!!
        assertTrue(ReminderPolicy.canDeliver(confirmed, 2, due, false, due, true))
        assertFalse(ReminderPolicy.canDeliver(confirmed, 1, due, false, due, true))
        assertFalse(ReminderPolicy.canDeliver(confirmed, 2, due - 1, false, due, true))
        assertFalse(ReminderPolicy.canDeliver(confirmed, 2, due, false, due, false))
        assertFalse(ReminderPolicy.canDeliver(confirmed.copy(notificationEnabled = false), 2, due, false, due, true))
        // Within 60s early tolerance is accepted so micro clock drifts or early OS timer ticks don't drop alerts
        assertTrue(ReminderPolicy.canDeliver(confirmed, 2, due, false, due - 1, true))
        // Beyond early tolerance window is rejected
        assertFalse(ReminderPolicy.canDeliver(confirmed, 2, due, false, due - ReminderPolicy.EARLY_TOLERANCE_MS - 1, true))
    }

    @Test fun advanceNotificationRequiresSeparateConsent() {
        val trigger = confirmed.eventTimeMs!! - ReminderPolicy.ADVANCE_MS
        assertFalse(ReminderPolicy.canDeliver(confirmed, 2, trigger, true, trigger, true))
        assertTrue(ReminderPolicy.canDeliver(confirmed.copy(advanceNotification = true), 2, trigger, true, trigger, true))
    }
}
