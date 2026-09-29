package space.iamjustkrishna.srutam.ai

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class RecordingTimeContext(
    val recordedAtMs: Long,
    val zoneId: String,
    val zoneInferred: Boolean = false
)

data class ResolvedReminderTime(
    val eventTimeMs: Long? = null,
    val localDate: String? = null,
    val localTime: String? = null,
    val zoneId: String,
    val precision: String = "UNKNOWN"
)

/** Resolves suggestions only. A valid result is never authorization to schedule an alarm. */
object ReminderTimeResolver {
    fun resolve(
        expression: String,
        context: RecordingTimeContext,
        suggestedDate: String? = null,
        suggestedTime: String? = null,
        explicitZone: String? = null
    ): ResolvedReminderTime {
        val zone = runCatching { ZoneId.of(explicitZone ?: context.zoneId) }.getOrNull()
            ?: return ResolvedReminderTime(zoneId = context.zoneId)
        val unknown = ResolvedReminderTime(zoneId = zone.id)
        val text = expression.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return unknown
        val reference = Instant.ofEpochMilli(context.recordedAtMs).atZone(zone)

        val relative = Regex("""\bin\s+(\d+|an?|one|two)\s+(hours?|minutes?)\b""").find(text)
        if (relative != null) {
            val amount = when (val value = relative.groupValues[1]) {
                "a", "an", "one" -> 1L
                "two" -> 2L
                else -> value.toLongOrNull() ?: return unknown
            }
            if (amount !in 1..100_000) return unknown
            val seconds = amount * if (relative.groupValues[2].startsWith("hour")) 3600 else 60
            val resolved = reference.plusSeconds(seconds)
            return ResolvedReminderTime(
                resolved.toInstant().toEpochMilli(), resolved.toLocalDate().toString(),
                resolved.toLocalTime().withSecond(0).withNano(0).toString(), zone.id, "EXACT"
            )
        }

        val date = when {
            "day after tomorrow" in text -> reference.toLocalDate().plusDays(2)
            "tomorrow" in text -> reference.toLocalDate().plusDays(1)
            Regex("""\btoday\b""").containsMatchIn(text) -> reference.toLocalDate()
            else -> {
                val parsed = parseDate(text, reference.toLocalDate())
                val explicitCalendar = Regex("""\b(?:\d{4}-\d{2}-\d{2}|(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2})\b""")
                if (parsed == null && explicitCalendar.containsMatchIn(text)) return unknown
                parsed ?: suggestedDate?.takeIf { hasDateEvidence(text) }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            }
        } ?: return unknown

        // Never turn a vague time or date-only mention into a clock time supplied by the model.
        val clockMatch = Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)\b""").find(text)
        val twentyFourHour = Regex("""\b(\d{1,2}):(\d{2})\b""").find(text)
        val time = when {
            clockMatch != null -> {
                val hour = clockMatch.groupValues[1].toInt()
                val minute = clockMatch.groupValues[2].ifBlank { "0" }.toInt()
                if (hour !in 1..12 || minute !in 0..59) return unknown
                LocalTime.of(hour % 12 + if (clockMatch.groupValues[3].startsWith("p")) 12 else 0, minute)
            }
            twentyFourHour != null -> runCatching {
                LocalTime.of(twentyFourHour.groupValues[1].toInt(), twentyFourHour.groupValues[2].toInt())
            }.getOrElse { return unknown }
            Regex("""\bnoon\b""").containsMatchIn(text) -> LocalTime.NOON
            Regex("""\bmidnight\b""").containsMatchIn(text) -> LocalTime.MIDNIGHT
            // Structured time is accepted only for a literal ISO date/time in the source.
            suggestedTime != null && text.contains("${date}t${suggestedTime}".lowercase(Locale.ROOT)) ->
                runCatching { LocalTime.parse(suggestedTime) }.getOrNull()
            else -> null
        }
        if (time == null) return ResolvedReminderTime(localDate = date.toString(), zoneId = zone.id, precision = "DATE_ONLY")
        return resolveLocal(date, time, zone)
    }

    fun resolveLocal(date: LocalDate, time: LocalTime, zone: ZoneId): ResolvedReminderTime {
        val local = LocalDateTime.of(date, time)
        val offsets = zone.rules.getValidOffsets(local)
        // DST gaps and overlaps require a different, unambiguous time from the user.
        if (offsets.size != 1) return ResolvedReminderTime(
            localDate = date.toString(), localTime = time.toString(), zoneId = zone.id
        )
        return ResolvedReminderTime(
            local.toInstant(offsets.single()).toEpochMilli(), date.toString(),
            time.toString(), zone.id, "EXACT"
        )
    }

    fun isFuture(time: ResolvedReminderTime, clock: Clock): Boolean =
        time.precision == "EXACT" && (time.eventTimeMs ?: Long.MIN_VALUE) > clock.millis()

    private fun hasDateEvidence(text: String): Boolean =
        Regex("""\b(\d{1,2}[/-]\d{1,2}|today|tomorrow|next\s+(week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)|monday|tuesday|wednesday|thursday|friday|saturday|sunday|january|february|march|april|may|june|july|august|september|october|november|december)\b""")
            .containsMatchIn(text)

    private fun parseDate(text: String, reference: LocalDate): LocalDate? {
        val iso = Regex("""\b\d{4}-\d{2}-\d{2}\b""").find(text)?.value
        if (iso != null) return runCatching { LocalDate.parse(iso) }.getOrNull()
        for (day in DayOfWeek.entries) {
            val name = day.name.lowercase(Locale.ROOT)
            if (Regex("""\bnext $name\b""").containsMatchIn(text)) {
                return reference.with(TemporalAdjusters.next(day))
            }
        }
        val month = Regex("""\b(january|february|march|april|may|june|july|august|september|october|november|december)\s+(\d{1,2})(?:st|nd|rd|th)?(?:,?\s+(\d{4}))?\b""")
            .find(text) ?: return null
        val year = month.groupValues[3].ifBlank { reference.year.toString() }
        return runCatching {
            LocalDate.parse(
                "${month.groupValues[1]} ${month.groupValues[2]} $year",
                DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH)
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT)
                    .let { java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().append(it).toFormatter(Locale.ENGLISH)
                        .withResolverStyle(java.time.format.ResolverStyle.STRICT) }
            )
        }.getOrNull()
    }
}
