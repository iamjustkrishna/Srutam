package space.iamjustkrishna.srutam.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordingNameFormatter {
    private val voiceNoteDateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    private val generatedRecordingPattern = Regex("^recording_?\\d+$", RegexOption.IGNORE_CASE)
    private val numericPattern = Regex("^\\d+$")

    fun displayName(fileName: String, timestamp: Long, savedName: String? = null): String {
        val preferred = savedName?.trim().orEmpty()
        val isLegacyName = preferred.isBlank() ||
            preferred.startsWith("Voice note ", ignoreCase = true) ||
            preferred.startsWith("Voice Note", ignoreCase = true) ||
            preferred.equals("Recording", ignoreCase = true)

        if (!isLegacyName) {
            return preferred
        }

        val cleanFileBase = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .trim()

        if (cleanFileBase.isNotBlank()) {
            return cleanFileBase
        }

        return "recording_${timestamp}"
    }

    private fun String.isUsableTitle(): Boolean {
        if (isBlank()) return false
        if (equals("Recording", ignoreCase = true)) return false
        if (generatedRecordingPattern.matches(this)) return false
        if (numericPattern.matches(this)) return false
        if (startsWith("Voice note ", ignoreCase = true) && length > 14) {
            // Filter out old legacy names that embedded the date/time string
            return false
        }
        return true
    }

    private fun prettifyFileName(baseName: String): String {
        return baseName
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
