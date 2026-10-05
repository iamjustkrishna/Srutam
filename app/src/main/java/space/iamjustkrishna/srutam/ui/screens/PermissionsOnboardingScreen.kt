package space.iamjustkrishna.srutam.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsOnboardingScreen(
    multiplePermissionsState: MultiplePermissionsState,
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var requestAttemptCount by rememberSaveable { mutableIntStateOf(0) }
    var hasDeniedOnce by rememberSaveable { mutableStateOf(false) }

    // Re-check system permissions on lifecycle ON_RESUME (e.g. returning from app settings)
    var resumeTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val micPermissionState = multiplePermissionsState.permissions.find {
        it.permission == Manifest.permission.RECORD_AUDIO
    }
    val storagePermissionState = multiplePermissionsState.permissions.find {
        it.permission == Manifest.permission.READ_MEDIA_AUDIO ||
                it.permission == Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val notificationPermissionState = multiplePermissionsState.permissions.find {
        it.permission == Manifest.permission.POST_NOTIFICATIONS
    }

    // Reactive status checks: reacts to Accompanist state changes and lifecycle resume
    val isMicGranted = resumeTrigger.let {
        micPermissionState?.status?.isGranted == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    val isStorageGranted = resumeTrigger.let {
        storagePermissionState?.status?.isGranted == true ||
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
    }

    val isNotificationsGranted = resumeTrigger.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState?.status?.isGranted == true ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val hasAllRequired = isMicGranted && isStorageGranted

    val shouldShowRationale = multiplePermissionsState.shouldShowRationale
    LaunchedEffect(shouldShowRationale) {
        if (shouldShowRationale) {
            hasDeniedOnce = true
        }
    }

    // If all required permissions are granted, transition smoothly
    LaunchedEffect(hasAllRequired) {
        if (hasAllRequired) {
            onAllPermissionsGranted()
        }
    }

    // Determine if permanently denied ("Don't ask again")
    val isPermanentlyDenied = (hasDeniedOnce && !shouldShowRationale && !hasAllRequired) ||
            (requestAttemptCount >= 2 && !hasAllRequired && !shouldShowRationale)

    PermissionsOnboardingContent(
        isMicGranted = isMicGranted,
        isStorageGranted = isStorageGranted,
        isNotificationsGranted = isNotificationsGranted,
        isPermanentlyDenied = isPermanentlyDenied,
        onRequestPermissions = {
            requestAttemptCount++
            multiplePermissionsState.launchMultiplePermissionRequest()
        },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    )
}

@Composable
fun PermissionsOnboardingContent(
    isMicGranted: Boolean,
    isStorageGranted: Boolean,
    isNotificationsGranted: Boolean,
    isPermanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Sem.card,
                        Sem.soft,
                        Sem.chip
                    )
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 48.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Sem.accentContainer)
                    .border(1.dp, Sem.accentBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    tint = Sem.accent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to Srutam",
                fontFamily = PlayfairDisplayFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Sem.text,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To provide high-accuracy local transcription and AI insights, Srutam needs access to your microphone and audio files.",
                fontSize = 14.sp,
                color = Sem.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Permission Cards Stack
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PermissionFeatureCard(
                    icon = Icons.Default.Mic,
                    iconTint = Sem.accent,
                    iconBg = Sem.accentContainer,
                    title = "Microphone Access",
                    subtitle = "Required to capture audio notes and meetings, transcribed on your device by Srutam Voice.",
                    isGranted = isMicGranted,
                    isRequired = true
                )

                PermissionFeatureCard(
                    icon = Icons.Outlined.Folder,
                    iconTint = Color(0xFF7C3AED),
                    iconBg = Sem.chip,
                    title = "Audio Storage",
                    subtitle = "Required to save M4A audio files and organize them in your local storage.",
                    isGranted = isStorageGranted,
                    isRequired = true
                )

                PermissionFeatureCard(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Sem.onEmerald,
                    iconBg = Sem.emeraldContainer,
                    title = "Notifications",
                    subtitle = "Recommended for persistent background recording controls and audio processing alerts.",
                    isGranted = isNotificationsGranted,
                    isRequired = false
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPermanentlyDenied) Sem.errorContainer else Sem.accentContainer
                ),
                border = BorderStroke(
                    1.dp,
                    if (isPermanentlyDenied) Sem.errorBorder else Sem.accentBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = if (isPermanentlyDenied) Sem.error else Sem.accent,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPermanentlyDenied) {
                            "Permissions are disabled in system settings. Tap below to open Settings, select Permissions, and enable Microphone and Storage."
                        } else {
                            "Srutam requires microphone and audio storage permissions to record and organize notes. Please grant access to continue."
                        },
                        fontSize = 13.sp,
                        color = if (isPermanentlyDenied) Sem.onErrorContainer else Sem.accent,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Bottom CTA Section
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Sem.card.copy(alpha = 0f),
                            Sem.card.copy(alpha = 0.94f),
                            Sem.card
                        )
                    )
                )
                .padding(bottom = 32.dp, top = 16.dp)
        ) {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onOpenSettings()
                    } else {
                        onRequestPermissions()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color(0x332563EB),
                        ambientColor = Color(0x222563EB)
                    ),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPermanentlyDenied) Color(0xFF0F172A) else Color(0xFF2563EB)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPermanentlyDenied) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open App Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Grant Access",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Allow Access",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    isRequired: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Sem.emeraldBorder else Sem.border,
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Sem.emeraldContainer else Sem.card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isGranted) Sem.emeraldContainer else iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = title,
                    tint = if (isGranted) Sem.onEmerald else iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Sem.text
                    )

                    // Status pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isGranted) Sem.emeraldContainer
                                else if (isRequired) Sem.chip
                                else Sem.soft
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isGranted) "Granted" else if (isRequired) "Required" else "Recommended",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isGranted) Sem.onEmerald else Sem.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Sem.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
