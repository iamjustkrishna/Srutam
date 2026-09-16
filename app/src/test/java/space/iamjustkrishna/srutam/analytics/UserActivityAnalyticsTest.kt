package space.iamjustkrishna.srutam.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus

class UserActivityAnalyticsTest {

    @Test
    fun testComputeMetricsWithEmptyData() {
        val now = System.currentTimeMillis()
        val metrics = UserActivityAnalytics.computeMetrics(emptyList(), emptyList(), now)

        assertEquals(0, metrics.notesLast7Days)
        assertEquals(0, metrics.aiExtractedLast7Days)
        assertEquals(0, metrics.actionItemsCompleted)
        assertEquals(0, metrics.ideasCaptured)
        assertEquals(7, metrics.weeklyActivity.size)
        assertTrue(metrics.weeklyActivity.last().isToday)
    }

    @Test
    fun testComputeMetricsWithActiveNotesAndInsights() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 86_400_000L

        val recordings = listOf(
            Recording(id = 1, timestamp = now, audioFilePath = "/path/1.m4a", aiStatus = RecordingAiStatus.READY, summary = "Summary 1"),
            Recording(id = 2, timestamp = now - oneDayMillis, audioFilePath = "/path/2.m4a", aiStatus = RecordingAiStatus.NOT_REQUESTED),
            Recording(id = 3, timestamp = now - (3 * oneDayMillis), audioFilePath = "/path/3.m4a", aiStatus = RecordingAiStatus.READY, summary = "Summary 3"),
            // Outside 7 days
            Recording(id = 4, timestamp = now - (10 * oneDayMillis), audioFilePath = "/path/4.m4a", aiStatus = RecordingAiStatus.READY)
        )

        val insights = listOf(
            InsightEntity(id = "1", recordingId = 1, kind = InsightKind.ACTION, text = "Action 1", status = InsightStatus.COMPLETED),
            InsightEntity(id = "2", recordingId = 1, kind = InsightKind.ACTION, text = "Action 2", status = InsightStatus.OPEN),
            InsightEntity(id = "3", recordingId = 2, kind = InsightKind.IDEA, text = "Idea 1"),
            InsightEntity(id = "4", recordingId = 3, kind = InsightKind.IDEA, text = "Idea 2"),
            InsightEntity(id = "5", recordingId = 3, kind = InsightKind.DECISION, text = "Decision 1")
        )

        val metrics = UserActivityAnalytics.computeMetrics(recordings, insights, now)

        assertEquals(3, metrics.notesLast7Days)
        assertEquals(2, metrics.aiExtractedLast7Days)
        assertEquals(1, metrics.actionItemsCompleted)
        assertEquals(2, metrics.ideasCaptured)
        assertEquals(7, metrics.weeklyActivity.size)
    }
}
