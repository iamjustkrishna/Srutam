package space.iamjustkrishna.srutam.analytics

import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyNoteCount(
    val dayLabel: String,
    val fullDayName: String,
    val count: Int,
    val isToday: Boolean,
    val dateMillis: Long
)

data class UserActivityMetrics(
    val notesLast7Days: Int = 0,
    val aiExtractedLast7Days: Int = 0,
    val actionItemsCompleted: Int = 0,
    val ideasCaptured: Int = 0,
    val weeklyActivity: List<DailyNoteCount> = emptyList(),
    val maxDailyCount: Int = 4
)

object UserActivityAnalytics {

    fun computeMetrics(
        recordings: List<Recording>,
        insights: List<InsightEntity>,
        now: Long = System.currentTimeMillis()
    ): UserActivityMetrics {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val sevenDaysAgoMillis = startOfToday - (6L * 86_400_000L)

        val notes7Days = recordings.count { it.timestamp >= sevenDaysAgoMillis }
        val aiExtracted7Days = recordings.count {
            it.timestamp >= sevenDaysAgoMillis &&
                (it.aiStatus == RecordingAiStatus.READY || !it.summary.isNullOrBlank())
        }

        val completedActions = insights.count {
            it.kind == InsightKind.ACTION && it.status == InsightStatus.COMPLETED
        }

        val ideasCount = insights.count {
            it.kind == InsightKind.IDEA
        }

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        val dailyList = (6 downTo 0).map { offset ->
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = startOfToday
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 86_400_000L - 1L

            val countForDay = recordings.count { it.timestamp in dayStart..dayEnd }
            val label = dayFormat.format(dayCal.time)
            val fullName = fullDayFormat.format(dayCal.time)

            DailyNoteCount(
                dayLabel = label,
                fullDayName = fullName,
                count = countForDay,
                isToday = offset == 0,
                dateMillis = dayStart
            )
        }

        val highest = dailyList.maxOfOrNull { it.count } ?: 0
        val maxScale = if (highest <= 2) 4 else ((highest + 1) / 2) * 2

        return UserActivityMetrics(
            notesLast7Days = notes7Days,
            aiExtractedLast7Days = aiExtracted7Days,
            actionItemsCompleted = completedActions,
            ideasCaptured = ideasCount,
            weeklyActivity = dailyList,
            maxDailyCount = maxScale.coerceAtLeast(4)
        )
    }
}
