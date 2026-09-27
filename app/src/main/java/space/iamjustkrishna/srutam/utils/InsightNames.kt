package space.iamjustkrishna.srutam.utils

import space.iamjustkrishna.srutam.data.Recording
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class InsightSource(val id: Long, val label: String, val available: Boolean)

object InsightNames {
    fun isMeaningful(name: String): Boolean {
        val value = name.trim().substringBeforeLast('.')
        return value.isNotBlank() &&
            !Regex("""(?i)(recording[_ -]?\d*|\d+|voice note(?:\s.*)?|audiofile[_ -]?\d*)""").matches(value)
    }

    fun source(id: Long, recordings: Map<Long, Recording>, zone: ZoneId = ZoneId.systemDefault()): InsightSource {
        val recording = recordings[id] ?: return InsightSource(id, "Source note unavailable", false)
        val label = if (isMeaningful(recording.name)) recording.name.trim() else {
            val date = Instant.ofEpochMilli(recording.timestamp).atZone(zone)
            // Include the year for stable screenshots and unambiguous history labels.
            "Voice note · ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.getDefault()))}"
        }
        return InsightSource(id, label, true)
    }
}
