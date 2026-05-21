package space.iamjustkrishna.srutam.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cachedDateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

fun formatDate(timestamp: Long): String {
    return cachedDateFormat.format(Date(timestamp))
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}

fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
