package space.iamjustkrishna.srutam.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.ui.theme.CeramicWhite
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CobaltBorder
import space.iamjustkrishna.srutam.ui.theme.CobaltContainer
import space.iamjustkrishna.srutam.ui.theme.CosmicGlowBlue
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCard
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.SlateBorder
import space.iamjustkrishna.srutam.ui.theme.SlateGrouped
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.TextMuted
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.ui.theme.TextPrimary
import space.iamjustkrishna.srutam.ui.theme.TextSecondary
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * First-launch capture activation screen that highlights the floating dock
 * as an ambient, friction-free way to capture voice thoughts from anywhere.
 */
@Composable
fun CaptureSetupScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isDark = LocalIsCosmicDark.current

    var isGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        )
    }
    var hasAdvanced by remember { mutableStateOf(false) }

    // Start service and advance when permission is newly detected
    fun onPermissionNewlyGranted() {
        if (!hasAdvanced) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            AppPreferences.setFloatingDockEnabled(context, true)
            val serviceIntent = Intent(context, FloatingButtonService::class.java)
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                // Ignore service start error on background restriction
            }
            coroutineScope.launch {
                delay(1500)
                if (!hasAdvanced) {
                    hasAdvanced = true
                    onComplete()
                }
            }
        }
    }

    // Auto-detect overlay permission upon returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                if (currentGranted && !isGranted) {
                    isGranted = true
                    onPermissionNewlyGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) CosmicVoidBackground else SlateGrouped)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Section: Hero Visual + Headlines
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Tag Capsule
                Surface(
                    color = if (isDark) CobaltContainer.copy(alpha = 0.5f) else CobaltContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CobaltBorder.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AMBIENT CAPTURE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CobaltBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hero Graphic: Phone Mockup with Animated Floating Dock
                FloatingDockHeroIllustration(isDark = isDark, isEnabled = isGranted)

                Spacer(modifier = Modifier.height(24.dp))

                // Value Prop Headline
                Text(
                    text = "Capture thoughts instantly",
                    fontSize = 28.sp,
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextOnDarkPrimary else TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Value Prop Subtitle
                Text(
                    text = "A discreet floating button stays on your screen edge. Capture ideas from any app without ever breaking your flow.",
                    fontSize = 14.sp,
                    color = if (isDark) TextOnDarkSecondary else TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Feature Highlights
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Default.Mic,
                        title = "One-Tap From Any App",
                        description = "Tap to speak while browsing, reading, or on the move.",
                        isDark = isDark
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Layers,
                        title = "Discreet & Snappable",
                        description = "Tucks unobtrusively into the screen edge. Drag anywhere.",
                        isDark = isDark
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Security,
                        title = "100% Private & Intentional",
                        description = "Only records when you actively tap. Never listens passively.",
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Bottom Section: Primary CTA + Skip Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonColor by animateColorAsState(
                    targetValue = if (isGranted) Color(0xFF10B981) else CobaltBlue,
                    animationSpec = tween(durationMillis = 400),
                    label = "ctaButtonColor"
                )

                Button(
                    onClick = {
                        if (isGranted) {
                            if (!hasAdvanced) {
                                hasAdvanced = true
                                onComplete()
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback without package URI if device doesn't support specific intent
                                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    context.startActivity(fallbackIntent)
                                }
                            } else {
                                isGranted = true
                                onPermissionNewlyGranted()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = buttonColor.copy(alpha = 0.4f),
                            spotColor = buttonColor.copy(alpha = 0.4f)
                        )
                ) {
                    AnimatedVisibility(
                        visible = isGranted,
                        enter = fadeIn() + scaleIn(),
                        label = "grantedContent"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Floating Dock Enabled! ✓",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (!isGranted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Enable Floating Dock",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Skip for now link
                Text(
                    text = "Skip for now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) TextOnDarkSecondary else TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (!hasAdvanced) {
                                hasAdvanced = true
                                onSkip()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Pure Compose Canvas & Vector hero illustration.
 * Renders an elegant phone mockup with a glowing, pulsing floating dock
 * snapped to its right bezel, emanating soundwave ripples.
 */
@Composable
private fun FloatingDockHeroIllustration(
    isDark: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dockPulse")

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Wave ripple phase
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    // Vertical bobbing
    val bobbingY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbingY"
    )

    val phoneBorderColor = if (isDark) CosmicVoidCardBorder else SlateBorder
    val phoneInnerColor = if (isDark) Color(0xFF070B14) else CeramicWhite
    val placeholderColor = if (isDark) Color(0xFF141C2E) else Color(0xFFEDF2F7)
    val dockAccent = if (isEnabled) Color(0xFF10B981) else CobaltBlue
    val glowColor = if (isEnabled) Color(0xFF10B981) else CosmicGlowBlue

    Box(
        modifier = modifier
            .size(width = 240.dp, height = 180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phoneWidth = 190.dp.toPx()
            val phoneHeight = 150.dp.toPx()
            val phoneLeft = (size.width - phoneWidth) / 2f
            val phoneTop = (size.height - phoneHeight) / 2f
            val cornerRadius = 24.dp.toPx()

            // 1. Phone Body Shadow
            drawRoundRect(
                color = phoneBorderColor.copy(alpha = 0.2f),
                topLeft = Offset(phoneLeft, phoneTop + 4.dp.toPx()),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 2. Phone Screen Background
            drawRoundRect(
                color = phoneInnerColor,
                topLeft = Offset(phoneLeft, phoneTop),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 3. Phone Bezel Outline
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        phoneBorderColor,
                        phoneBorderColor.copy(alpha = 0.4f)
                    ),
                    start = Offset(phoneLeft, phoneTop),
                    end = Offset(phoneLeft + phoneWidth, phoneTop + phoneHeight)
                ),
                topLeft = Offset(phoneLeft, phoneTop),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Subtle Top Notch / Speaker
            val speakerWidth = 40.dp.toPx()
            val speakerHeight = 4.dp.toPx()
            drawRoundRect(
                color = placeholderColor,
                topLeft = Offset((size.width - speakerWidth) / 2f, phoneTop + 10.dp.toPx()),
                size = Size(speakerWidth, speakerHeight),
                cornerRadius = CornerRadius(speakerHeight / 2, speakerHeight / 2)
            )

            // 5. App UI Faint Wireframe Lines inside phone (gives realistic context)
            val uiStartY = phoneTop + 28.dp.toPx()
            val cardLeft = phoneLeft + 16.dp.toPx()
            val cardWidth = phoneWidth - 32.dp.toPx()

            // Header line
            drawRoundRect(
                color = placeholderColor,
                topLeft = Offset(cardLeft, uiStartY),
                size = Size(60.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Feed card 1
            drawRoundRect(
                color = placeholderColor.copy(alpha = 0.7f),
                topLeft = Offset(cardLeft, uiStartY + 16.dp.toPx()),
                size = Size(cardWidth - 28.dp.toPx(), 36.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Feed card 2
            drawRoundRect(
                color = placeholderColor.copy(alpha = 0.5f),
                topLeft = Offset(cardLeft, uiStartY + 58.dp.toPx()),
                size = Size(cardWidth - 36.dp.toPx(), 32.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 6. Floating Dock Bubble snapped to Right Phone Edge
            val dockCenterX = phoneLeft + phoneWidth - 10.dp.toPx()
            val dockCenterY = phoneTop + (phoneHeight / 2f) + bobbingY.dp.toPx()
            val dockRadius = 22.dp.toPx() * pulseScale

            // Expanding soundwave ripple circles
            val rippleRadius = dockRadius + (rippleProgress * 28.dp.toPx())
            val rippleAlpha = (1f - rippleProgress).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = glowColor.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = Offset(dockCenterX, dockCenterY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val secondRippleProgress = (rippleProgress + 0.5f) % 1f
            val secondRippleRadius = dockRadius + (secondRippleProgress * 28.dp.toPx())
            val secondRippleAlpha = (1f - secondRippleProgress).coerceIn(0f, 1f) * 0.3f
            drawCircle(
                color = glowColor.copy(alpha = secondRippleAlpha),
                radius = secondRippleRadius,
                center = Offset(dockCenterX, dockCenterY),
                style = Stroke(width = 1.dp.toPx())
            )

            // Glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(dockCenterX, dockCenterY),
                    radius = dockRadius * 1.6f
                ),
                radius = dockRadius * 1.6f,
                center = Offset(dockCenterX, dockCenterY)
            )

            // Dock Capsule Body (Gradient filled)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        dockAccent,
                        dockAccent.copy(alpha = 0.85f)
                    ),
                    start = Offset(dockCenterX - dockRadius, dockCenterY - dockRadius),
                    end = Offset(dockCenterX + dockRadius, dockCenterY + dockRadius)
                ),
                radius = dockRadius,
                center = Offset(dockCenterX, dockCenterY)
            )

            // Dock Border
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = dockRadius,
                center = Offset(dockCenterX, dockCenterY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Mic / Audio Waves inside Dock Bubble
            val barWidth = 2.5.dp.toPx()
            val barSpacing = 4.dp.toPx()
            val heights = listOf(8.dp.toPx(), 14.dp.toPx(), 20.dp.toPx(), 14.dp.toPx(), 8.dp.toPx())
            val totalBarsWidth = (heights.size * barWidth) + ((heights.size - 1) * barSpacing)
            val barsStartX = dockCenterX - (totalBarsWidth / 2f)

            heights.forEachIndexed { index, barHeight ->
                val dynamicHeight = if (isEnabled) {
                    barHeight * (0.8f + 0.4f * kotlin.math.sin((rippleProgress * kotlin.math.PI * 2 + index).toFloat()))
                } else {
                    barHeight
                }
                val bx = barsStartX + index * (barWidth + barSpacing)
                val by = dockCenterY - (dynamicHeight / 2f)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(bx, by),
                    size = Size(barWidth, dynamicHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CosmicVoidCard.copy(alpha = 0.6f) else CeramicWhite)
            .border(
                1.dp,
                if (isDark) CosmicVoidCardBorder.copy(alpha = 0.5f) else SlateBorder.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = if (isDark) CobaltContainer.copy(alpha = 0.4f) else CobaltContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CobaltBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) TextOnDarkPrimary else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureSetupScreenPreview() {
    SrutamTheme(darkTheme = false) {
        CaptureSetupScreen(onComplete = {}, onSkip = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureSetupScreenDarkPreview() {
    SrutamTheme(darkTheme = true) {
        CaptureSetupScreen(onComplete = {}, onSkip = {})
    }
}
