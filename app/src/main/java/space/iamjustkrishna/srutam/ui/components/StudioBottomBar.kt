package space.iamjustkrishna.srutam.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.*

enum class RootTab {
    NOTES,
    ACTIONS,
    AI
}

enum class RecordingMode {
    IDLE,
    HOLDING,
    LOCKED
}

/**
 * Premium Minimal Floating Studio Bottom Bar matching reference design.
 *
 * Left / Center: Soft floating frosted capsule containing "Notes", "Actions", and "AI" tabs,
 *                with an active pure-white elevated pill and matching icons.
 * Right: Concentric ruby-crimson circular recording button.
 *
 * Supports in-place morphing:
 * - Hold to Record: Red button stays continuously mounted under finger. The left capsule morphs into
 *   active controls (trash, timer, waveform, pause). Releasing finger stops and opens save dialog.
 * - Tap to Record: Quick tap locks into active recording bar without holding. Right button turns to Stop button.
 * - Pause / Resume: Dedicated toggle button allows pausing mid-sentence and resuming cleanly.
 */
@Composable
fun StudioBottomBar(
    currentTab: RootTab,
    onTabSelected: (RootTab) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean = false,
    recordingElapsedMs: Long,
    onStartRecording: () -> Unit,
    onPauseToggle: () -> Unit = {},
    onFinishRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    pendingActionCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var recordingMode by remember { mutableStateOf(RecordingMode.IDLE) }
    var dragYOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRecording) {
        if (isRecording && recordingMode == RecordingMode.IDLE) {
            recordingMode = RecordingMode.LOCKED
        } else if (!isRecording) {
            recordingMode = RecordingMode.IDLE
        }
    }

    val isBarActiveRecording = isRecording || recordingMode != RecordingMode.IDLE

    val recordInteractionSource = remember { MutableInteractionSource() }
    val isRecordPressed by recordInteractionSource.collectIsPressedAsState()

    // Tactile Spring-Damped Harmonic Scale: Depresses to 0.86f, rebounds to 1.08f, then settles to 1.00f
    val recordScale by animateFloatAsState(
        targetValue = if (isRecordPressed || recordingMode == RecordingMode.HOLDING) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 400f
        ),
        label = "record_spring_scale"
    )

    var pulseTrigger by remember { mutableIntStateOf(0) }
    val haloProgress = remember { Animatable(0f) }

    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            haloProgress.snapTo(0f)
            haloProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Morphing Capsule on the Left ──
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                if (isBarActiveRecording) {
                    if (isPaused) Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFFEF3C7)))
                    else Brush.horizontalGradient(listOf(Color(0xFFFECACA), Color(0xFFFEE2E2)))
                } else {
                    SolidColor(Color(0xFFE2E8F0))
                }
            ),
            shadowElevation = if (isBarActiveRecording) 8.dp else 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isBarActiveRecording) {
                            if (isPaused) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFFFFFF))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFF7F7), Color(0xFFFFFFFF), Color(0xFFFFF5F5))
                                )
                            }
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF8FAFC).copy(alpha = 0.95f), Color(0xFFF1F5F9).copy(alpha = 0.95f))
                            )
                        }
                    )
            ) {
                AnimatedContent(
                    targetState = isBarActiveRecording,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                                expandHorizontally(
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                                    expandFrom = Alignment.End
                                )) togetherWith
                                (fadeOut(animationSpec = tween(120)) +
                                        shrinkHorizontally(animationSpec = tween(120), shrinkTowards = Alignment.End))
                    },
                    label = "capsule_morph"
                ) { active ->
                if (active) {
                    ActiveRecordingControls(
                        recordingElapsedMs = recordingElapsedMs,
                        isPaused = isPaused,
                        onPauseToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPauseToggle()
                        },
                        onCancel = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            recordingMode = RecordingMode.IDLE
                            onCancelRecording()
                        }
                    )
                } else {
                    // Normal Idle Floating Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StudioTabItem(
                            label = "Notes",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = if (currentTab == RootTab.NOTES) Color(0xFF0F172A) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            isSelected = currentTab == RootTab.NOTES,
                            onClick = { onTabSelected(RootTab.NOTES) },
                            modifier = Modifier.weight(1.0f)
                        )

                        StudioTabItem(
                            label = "Insights",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = if (currentTab == RootTab.ACTIONS) Color(0xFF0F172A) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            isSelected = currentTab == RootTab.ACTIONS,
                            badgeCount = pendingActionCount,
                            onClick = { onTabSelected(RootTab.ACTIONS) },
                            modifier = Modifier.weight(1.25f)
                        )

                        StudioTabItem(
                            label = "AI",
                            icon = {
                                Text(
                                    text = "✦",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTab == RootTab.AI) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            },
                            isSelected = currentTab == RootTab.AI,
                            onClick = { onTabSelected(RootTab.AI) },
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                }
            }
        }
    }

        // ── 2. Persistently Mounted Record / Stop Shutter Button on the Right ──
        // Stays in composition tree throughout gestures so pointerInput never gets unmounted
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            // Floating WhatsApp-Style Slide-to-Lock Target Pill
            androidx.compose.animation.AnimatedVisibility(
                visible = recordingMode == RecordingMode.HOLDING,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                        ) { it / 2 },
                exit = fadeOut(animationSpec = tween(150)) +
                        slideOutVertically(animationSpec = tween(150)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-78).dp)
            ) {
                SlideToLockIndicator(dragYOffset = dragYOffset)
            }

            // Expanding Radiant Sonic Halo
            if (haloProgress.value in 0.01f..0.99f) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(1.0f + haloProgress.value * 0.65f)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = (1f - haloProgress.value) * 0.55f))
                )
            }

            if (recordingMode == RecordingMode.LOCKED) {
                // In locked mode: tap stop button finishes recording
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(recordScale)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFDC2626).copy(alpha = 0.3f),
                            spotColor = Color(0xFFDC2626).copy(alpha = 0.45f)
                        )
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            recordingMode = RecordingMode.IDLE
                            onFinishRecording()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                }
            } else {
                // In idle or holding mode: dual-mode gesture detector with WhatsApp-style slide-to-lock
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(recordScale)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFDC2626).copy(alpha = 0.3f),
                            spotColor = Color(0xFFDC2626).copy(alpha = 0.45f)
                        )
                        .clip(CircleShape)
                        .background(Color(0xFFFECACA).copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f), CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                val startTime = System.currentTimeMillis()
                                var dragX = 0f
                                var dragY = 0f
                                dragYOffset = 0f
                                recordingMode = RecordingMode.HOLDING
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pulseTrigger++
                                onStartRecording()

                                var isTouching = true
                                while (isTouching) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()

                                    if (change != null && change.pressed) {
                                        val posDelta = change.positionChange()
                                        dragX += posDelta.x
                                        dragY += posDelta.y
                                        dragYOffset = dragY
                                        change.consume()

                                        // 1. WhatsApp Slide-to-Lock: drag up >= 60dp (~160px)
                                        if (dragY <= -160f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            recordingMode = RecordingMode.LOCKED
                                            dragYOffset = 0f
                                            isTouching = false
                                        }
                                        // 2. Slide left past -140px to cancel
                                        else if (dragX <= -140f) {
                                            onCancelRecording()
                                            recordingMode = RecordingMode.IDLE
                                            dragYOffset = 0f
                                            isTouching = false
                                        }
                                    } else {
                                        isTouching = false
                                        dragYOffset = 0f
                                        val duration = System.currentTimeMillis() - startTime
                                        if (duration < 350) {
                                            // Quick tap -> lock into active recording
                                            recordingMode = RecordingMode.LOCKED
                                        } else {
                                            // Hold released without locking -> stop recording & open save dialog
                                            recordingMode = RecordingMode.IDLE
                                            onFinishRecording()
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner concentric crimson ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444),
                                        Color(0xFFDC2626),
                                        Color(0xFFB91C1C)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Central concentric red core dot
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF991B1B))
                        )
                    }
                }
            }
        }
    }
}

/**
 * In-place expanded active recording capsule with discard, pulsing red/amber indicator,
 * clean MM:SS timer, dynamic waveform, and pause/resume button.
 */
@Composable
private fun ActiveRecordingControls(
    recordingElapsedMs: Long,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val minutes = (recordingElapsedMs / 1000) / 60
    val seconds = (recordingElapsedMs / 1000) % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Discard / Trash button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFFEE2E2), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Cancel Recording",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Live Elapsed Time + Pulsing Indicator with dedicated boundary padding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .padding(3.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (isPaused) 0.5f else dotAlpha)
                ) {
                    drawCircle(if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = formattedTime,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isPaused) Color(0xFFF59E0B) else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Dynamic Waveform (animates when recording, freezes/flattens when paused)
        ActiveRecordingWaveform(
            isPaused = isPaused,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Dedicated Pause / Resume Toggle Button
        IconButton(
            onClick = onPauseToggle,
            modifier = Modifier
                .size(36.dp)
                .background(if (isPaused) Color(0xFFEFF6FF) else Color(0xFFF1F5F9), CircleShape)
                .border(1.dp, if (isPaused) Color(0xFFBFDBFE) else Color(0xFFE2E8F0), CircleShape)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                tint = if (isPaused) Color(0xFF2563EB) else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Real-time animated dynamic waveform bars simulating live microphone input.
 */
@Composable
private fun ActiveRecordingWaveform(
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseHeights = listOf(6, 12, 18, 10, 20, 14, 8, 16, 22, 12, 18, 8, 14, 20)
        baseHeights.forEachIndexed { i, baseH ->
            val sinVal = if (isPaused) 0f else kotlin.math.sin(phase + i * 0.45f).toFloat()
            val animatedHeight = if (isPaused) 4f else (baseH * (0.55f + 0.45f * sinVal)).coerceIn(4f, 24f)
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(animatedHeight.dp)
                    .background(if (isPaused) Color(0xFF94A3B8) else Color(0xFFEF4444), CircleShape)
            )
        }
    }
}

@Composable
private fun StudioTabItem(
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .padding(vertical = 3.dp, horizontal = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon()
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    color = Color(0xFF0F172A),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon()
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    color = Color(0xFF64748B),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * WhatsApp-style vertical slide-to-lock floating pill indicator.
 * Appears right above the red record button during hold-to-record.
 * Shows animated lock icon and upward chevron, tracking the thumb drag.
 */
@Composable
private fun SlideToLockIndicator(
    dragYOffset: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chevron_bounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val lockProgress = (-dragYOffset / 160f).coerceIn(0f, 1f)
    val isNearLock = lockProgress >= 0.7f

    Surface(
        modifier = modifier
            .width(44.dp)
            .height(68.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isNearLock) Color(0xFFEF4444) else Color(0xFFFECACA)
        ),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isNearLock) {
                            listOf(Color(0xFFFEE2E2), Color(0xFFFCA5A5))
                        } else {
                            listOf(Color.White, Color(0xFFFFF1F2))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isNearLock) Icons.Default.Lock else Icons.Outlined.LockOpen,
                    contentDescription = "Slide up to lock",
                    tint = if (isNearLock) Color(0xFFDC2626) else Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (isNearLock) Color(0xFFDC2626) else Color(0xFFF87171),
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = bounceY.dp)
                )
            }
        }
    }
}

