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

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
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
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to Srutam",
                fontFamily = PlayfairDisplayFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To capture thoughts and organize your voice memos seamlessly, Srutam requires a few device permissions.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Microphone Permission Card
            PermissionFeatureCard(
                icon = Icons.Default.Mic,
                iconTint = Color(0xFF2563EB),
                iconBg = Color(0xFFEFF6FF),
                title = "Microphone Access",
                subtitle = "Record high-fidelity voice notes and transcribe your thoughts on-device.",
                isGranted = isMicGranted,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Storage Permission Card
            val storageTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "Audio Media Access"
            } else {
                "Files & Storage Access"
            }
            PermissionFeatureCard(
                icon = Icons.Outlined.Folder,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFF0F9FF),
                title = storageTitle,
                subtitle = "Save audio recordings in your device Music folder, and manage playback and file deletion smoothly.",
                isGranted = isStorageGranted,
                isRequired = true
            )

            // Notifications Permission Card (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(14.dp))
                PermissionFeatureCard(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Color(0xFF8B5CF6),
                    iconBg = Color(0xFFF5F3FF),
                    title = "Live Recording Controls",
                    subtitle = "Display recording chronometer, background status, and quick pause/stop in your status bar.",
                    isGranted = isNotificationsGranted,
                    isRequired = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rationale / Help Banner if user denied or permanently denied
            AnimatedVisibility(
                visible = isPermanentlyDenied || (hasDeniedOnce && shouldShowRationale),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isPermanentlyDenied) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                        )
                        .border(
                            1.dp,
                            if (isPermanentlyDenied) Color(0xFFFECACA) else Color(0xFFBFDBFE),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = if (isPermanentlyDenied) Color(0xFFDC2626) else Color(0xFF2563EB),
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
                            color = if (isPermanentlyDenied) Color(0xFF991B1B) else Color(0xFF1E40AF),
                            lineHeight = 18.sp
                        )
                    }
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
                            Color(0x00FFFFFF),
                            Color(0xF0FFFFFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(bottom = 32.dp, top = 16.dp)
        ) {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } else {
                        requestAttemptCount++
                        multiplePermissionsState.launchMultiplePermissionRequest()
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
                if (isGranted) Color(0xFFD1FAE5) else Color(0xFFE2E8F0),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFFAFCFA) else Color(0xFFFFFFFF)
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
                    .background(if (isGranted) Color(0xFFECFDF5) else iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = title,
                    tint = if (isGranted) Color(0xFF059669) else iconTint,
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
                        color = Color(0xFF0F172A)
                    )

                    // Status pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isGranted) Color(0xFFECFDF5)
                                else if (isRequired) Color(0xFFF1F5F9)
                                else Color(0xFFF8FAFC)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isGranted) "Granted" else if (isRequired) "Required" else "Recommended",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isGranted) Color(0xFF059669) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
