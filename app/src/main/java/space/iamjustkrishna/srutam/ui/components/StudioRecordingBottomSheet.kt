package space.iamjustkrishna.srutam.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioRecordingBottomSheet(
    elapsedMs: Long,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onStopAndSave: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHandsFreeLocked by remember { mutableStateOf(false) }

    // Pulsing recording red dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "studio_rec_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val seconds = (elapsedMs / 1000) % 60
    val minutes = (elapsedMs / 1000) / 60
    val millisTens = (elapsedMs % 1000) / 100
    val formattedTime = String.format("%02d:%02d.%d", minutes, seconds, millisTens)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        dragHandle = {
            // Apple-style centered pill drag handle
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.5.dp)
                    .background(Color(0xFFD1D1D6), CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status & Mode Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(if (isPaused) 0.5f else dotAlpha)
                    ) {
                        drawCircle(color = if (isPaused) Color(0xFF8E8E93) else Color(0xFFFF3B30))
                    }

                    Text(
                        text = if (isPaused) "Recording Paused" else "Recording Voice Note",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaused) Color(0xFF8E8E93) else Color(0xFF1C1C1E)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Hands-Free Lock Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HANDS-FREE LOCK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isHandsFreeLocked) Color(0xFF34C759) else Color(0xFF8E8E93)
                    )
                    Switch(
                        checked = isHandsFreeLocked,
                        onCheckedChange = { isHandsFreeLocked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E5EA),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Digital Precision Timecode
            Text(
                text = formattedTime,
                style = TextStyle(
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-1).sp,
                    fontFeatureSettings = "tnum"
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Live Audio Soundwave Frequency Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveSoundwaveVisualizer(isPaused = isPaused)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Studio Hardware Controls Row (matching Apple Studio mockup: Pause on Left, Stop in Center, Delete on Right)
            val haptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left Control: Pause / Resume
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPauseToggle()
                        }),
                    shape = CircleShape,
                    color = Color(0xFFF2F4F7)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color(0xFF1C1C1E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center Control: Stop & Save (Large vibrant red hero circle with white square stop icon)
                Surface(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopAndSave()
                        }),
                    shape = CircleShape,
                    color = Color(0xFFFF3B30),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop and Save",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Right Control: Delete / Discard (Soft pink circle with red trash icon)
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancel()
                        }),
                    shape = CircleShape,
                    color = Color(0xFFFEE4E2)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Discard",
                            tint = Color(0xFFD92D20),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSoundwaveVisualizer(isPaused: Boolean) {
    val barCount = 32
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            val rawHeight = if (isPaused) {
                0.2f
            } else {
                val sinVal = kotlin.math.sin(normalizedIdx * 5f + wavePhase)
                val cosVal = kotlin.math.cos(normalizedIdx * 3f - wavePhase)
                ((sinVal + cosVal + 2f) / 4f).coerceIn(0.18f, 1f)
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(rawHeight)
                    .background(
                        color = if (i % 7 == 0 || i % 7 == 1) Color(0xFFFF3B30) else CobaltBlue,
                        shape = CircleShape
                    )
            )
        }
    }
}
