package space.iamjustkrishna.srutam.ai

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderTimeResolverTest {
    private val recorded = Instant.parse("2026-09-27T18:40:00Z").toEpochMilli()
    private val context = RecordingTimeContext(recorded, "Asia/Kolkata")

    @Test fun relativeCalendarDateUsesRecordingDayAndExplicitTime() {
        // Recording is Sep 28, 00:10 locally. It may be processed days later.
        val result = ReminderTimeResolver.resolve("tomorrow at 3pm", context)
        assertEquals("2026-09-29", result.localDate)
        assertEquals("15:00", result.localTime)
        assertEquals(Instant.parse("2026-09-29T09:30:00Z").toEpochMilli(), result.eventTimeMs)
    }

    @Test fun dayAfterTomorrowDoesNotMatchTomorrowFirst() {
        assertEquals("2026-09-30", ReminderTimeResolver.resolve("day after tomorrow", context).localDate)
    }

    @Test fun dateOnlyDoesNotInheritRecordingTimeOrModelClockTime() {
        val result = ReminderTimeResolver.resolve("May 20, 2027", context, "2027-05-20", "00:10")
        assertEquals("DATE_ONLY", result.precision)
        assertNull(result.eventTimeMs)
        assertEquals("2027-05-20", result.localDate)
    }

    @Test fun unknownNeverBecomesTomorrow() {
        for (text in listOf("", "sometime eventually", "our profit projection", "20 percent growth")) {
            val result = ReminderTimeResolver.resolve(text, context, "2026-09-29", "15:00")
            assertEquals("UNKNOWN", result.precision)
            assertNull(result.eventTimeMs)
        }
    }

    @Test fun pastDateIsNotRolledIntoFuture() {
        val result = ReminderTimeResolver.resolve("May 20, 2025 at 3pm", context)
        assertEquals("2025-05-20", result.localDate)
        assertFalse(ReminderTimeResolver.isFuture(result, Clock.fixed(Instant.ofEpochMilli(recorded), ZoneOffset.UTC)))
    }

    @Test fun invalidClockAndCalendarValuesStayUnresolved() {
        for (text in listOf("February 30, 2026 at 3pm", "tomorrow at 25:61", "tomorrow at 13pm")) {
            assertNull(ReminderTimeResolver.resolve(text, context).eventTimeMs)
        }
        assertEquals("UNKNOWN", ReminderTimeResolver.resolve("February 30, 2026", context, "2026-03-02").precision)
    }

    @Test fun relativeHoursUseRecordingInstant() {
        assertEquals(recorded + 7_200_000, ReminderTimeResolver.resolve("in 2 hours", context).eventTimeMs)
    }

    @Test fun daylightSavingGapAndOverlapRequireReview() {
        val zone = ZoneId.of("America/New_York")
        assertNull(ReminderTimeResolver.resolveLocal(LocalDate.of(2026, 3, 8), LocalTime.of(2, 30), zone).eventTimeMs)
        assertNull(ReminderTimeResolver.resolveLocal(LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), zone).eventTimeMs)
    }

    @Test fun invalidZoneDoesNotSilentlyScheduleInAnotherZone() {
        assertNull(ReminderTimeResolver.resolve("tomorrow at 3pm", context, explicitZone = "not/a-zone").eventTimeMs)
    }
}
