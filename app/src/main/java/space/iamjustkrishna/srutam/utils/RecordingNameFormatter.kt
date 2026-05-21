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
        if (preferred.isUsableTitle()) {
            return preferred
        }

        val baseName = fileName.substringBeforeLast('.').trim()
        if (baseName.isUsableTitle()) {
            return prettifyFileName(baseName)
        }

        return defaultVoiceNoteName(timestamp)
    }

    fun defaultVoiceNoteName(timestamp: Long): String {
        return "Voice note ${voiceNoteDateFormat.format(Date(timestamp))}"
    }

    private fun String.isUsableTitle(): Boolean {
        if (isBlank()) return false
        if (equals("Recording", ignoreCase = true)) return false
        if (generatedRecordingPattern.matches(this)) return false
        if (numericPattern.matches(this)) return false
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
