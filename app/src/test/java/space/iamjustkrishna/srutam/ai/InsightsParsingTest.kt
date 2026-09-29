package space.iamjustkrishna.srutam.ai

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InsightsParsingTest {
    private val context = RecordingTimeContext(Instant.parse("2026-09-27T10:00:00Z").toEpochMilli(), "UTC")
    private fun parse(reminders: String, transcript: String = "Call tomorrow at 3pm.") =
        AIProcessor(ApplicationProvider.getApplicationContext()).parseAIResponse(
            """{"summary":"Keep this summary.","keyPoints":["A useful point"],"actionItems":[],"ideas":["A useful idea"],"decisions":[],"reminders":$reminders,"wiifm":"Value"}""",
            transcript, context
        )

    @Test fun malformedReminderFieldsDoNotDiscardOtherInsights() {
        val result = parse("""[{"title":"Call","timeDescription":17,"date":false,"time":[]},"invalid",{"title":42}]""")
        assertEquals("Keep this summary.", result.summary)
        assertTrue("A useful idea" in result.ideas)
        assertEquals(1, result.reminders.size)
        assertNull(result.reminders.single().eventTimeMs)
    }

    @Test fun inventedQuoteCannotSupplyAnAlarmTime() {
        val result = parse("""[{"title":"Call","timeDescription":"tomorrow at 8pm","date":"2026-09-28","time":"20:00"}]""")
        assertEquals("UNKNOWN", result.reminders.single().timePrecision)
        assertNull(result.reminders.single().eventTimeMs)
    }

    @Test fun exactQuoteUsesRecordingContextAndIgnoresOldEstimatedOffset() {
        val result = parse("""[{"title":"Call","timeDescription":"tomorrow at 3pm","estimatedTimeOffsetHours":999,"date":"2026-09-28","time":"15:00"}]""")
        assertEquals(Instant.parse("2026-09-28T15:00:00Z").toEpochMilli(), result.reminders.single().eventTimeMs)
    }

    @Test fun dateOnlyQuoteDoesNotTakeInventedModelTime() {
        val result = parse("""[{"title":"Target","timeDescription":"May 20, 2027","date":"2027-05-20","time":"00:10","type":"MILESTONE"}]""",
            "Our target is May 20, 2027.")
        assertEquals("DATE_ONLY", result.reminders.single().timePrecision)
        assertNull(result.reminders.single().eventTimeMs)
    }
}
