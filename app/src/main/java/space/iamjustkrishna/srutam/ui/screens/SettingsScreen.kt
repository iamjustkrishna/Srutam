package space.iamjustkrishna.srutam.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.service.PersistentRecordingNotificationService
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AppPreferences

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isFloatingDockEnabled by remember {
        mutableStateOf(AppPreferences.isFloatingDockEnabled(context))
    }
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(AppPreferences.isPersistentNotificationEnabled(context))
    }
    var selectedProvider by remember {
        mutableStateOf(AppPreferences.getAIProvider(context))
    }
    var customApiKey by remember {
        mutableStateOf(AppPreferences.getCustomApiKey(context))
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SquircleActionButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onNavigateBack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Srutam",
                                fontSize = 28.sp,
                                fontFamily = PlayfairDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Settings",
                                fontSize = 28.sp,
                                fontFamily = PlayfairDisplayFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal,
                                color = CobaltBlue
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CobaltContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNavigateBack)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        containerColor = CeramicWhite,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // =========================================================
            // Section 1: Instant Capture Shortcuts
            // =========================================================
            SettingsSection(title = "INSTANT CAPTURE SHORTCUTS") {
                // Row 1: On-Screen Floating Dock
                SettingsToggleRow(
                    icon = Icons.Default.PictureInPicture,
                    iconBg = CobaltContainer,
                    iconTint = CobaltBlue,
                    title = "On-Screen Floating Dock",
                    subtitle = "Discreet edge pill for instant capture from any app",
                    checked = isFloatingDockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, "Grant overlay permission for floating dock", Toast.LENGTH_LONG).show()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            isFloatingDockEnabled = enabled
                            AppPreferences.setFloatingDockEnabled(context, enabled)
                            val serviceIntent = Intent(context, FloatingButtonService::class.java)
                            if (enabled) {
                                context.startService(serviceIntent)
                            } else {
                                context.stopService(serviceIntent)
                            }
                        }
                    }
                )

                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                // Row 2: Quick Settings Notification Tile
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Quick Settings Tile",
                    subtitle = "Persistent one-tap recording in status bar",
                    checked = isPersistentNotificationEnabled,
                    onCheckedChange = { enabled ->
                        isPersistentNotificationEnabled = enabled
                        AppPreferences.setPersistentNotificationEnabled(context, enabled)
                        val intent = Intent(context, PersistentRecordingNotificationService::class.java).apply {
                            action = if (enabled) {
                                PersistentRecordingNotificationService.ACTION_START_RECORDING
                            } else {
                                PersistentRecordingNotificationService.ACTION_STOP_NOTIFICATION
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                )
            }

            // =========================================================
            // Section 2: Speech Recognition Engine
            // =========================================================
            SettingsSection(title = "SPEECH RECOGNITION ENGINE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CobaltContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Speech Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Proprietary on-device model · Zero cloud latency",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = CobaltContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Srutam Voice v1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Privacy Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Raw audio files never leave your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = EmeraldContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "100% On-Device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // =========================================================
            // Section 3: AI Intelligence Provider
            // =========================================================
            SettingsSection(title = "AI INTELLIGENCE & PROVIDER") {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Option A: Srutam Cloud (Default)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedProvider = AppPreferences.PROVIDER_SRUTAM_DEFAULT
                                AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_SRUTAM_DEFAULT)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedProvider == AppPreferences.PROVIDER_SRUTAM_DEFAULT,
                                onClick = {
                                    selectedProvider = AppPreferences.PROVIDER_SRUTAM_DEFAULT
                                    AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_SRUTAM_DEFAULT)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CobaltBlue)
                            )
                            Column {
                                Text(
                                    text = "Srutam Cloud (Default)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Out-of-the-box summaries and chat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(color = SlateGrouped, shape = CircleShape) {
                            Text(
                                text = "Free with limits",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = SlateBorder.copy(alpha = 0.6f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Option B: Custom API Key (BYOK)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedProvider == AppPreferences.PROVIDER_SRUTAM_DEFAULT) {
                                    selectedProvider = AppPreferences.PROVIDER_OPENAI
                                    AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_OPENAI)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider != AppPreferences.PROVIDER_SRUTAM_DEFAULT,
                            onClick = {
                                if (selectedProvider == AppPreferences.PROVIDER_SRUTAM_DEFAULT) {
                                    selectedProvider = AppPreferences.PROVIDER_OPENAI
                                    AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_OPENAI)
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CobaltBlue)
                        )
                        Column {
                            Text(
                                text = "Custom API Key (BYOK)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Bring your own key for unlimited queries",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Provider Pills Selection
                    if (selectedProvider != AppPreferences.PROVIDER_SRUTAM_DEFAULT) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val providers = listOf(
                                AppPreferences.PROVIDER_OPENAI to "OpenAI",
                                AppPreferences.PROVIDER_ANTHROPIC to "Anthropic",
                                AppPreferences.PROVIDER_GEMINI to "Gemini",
                                AppPreferences.PROVIDER_GROQ to "Groq"
                            )

                            providers.forEach { (code, label) ->
                                val isSelected = selectedProvider == code
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedProvider = code
                                            AppPreferences.setAIProvider(context, code)
                                        },
                                    color = if (isSelected) CobaltBlue else SlateGrouped,
                                    border = BorderStroke(1.dp, if (isSelected) CobaltBlue else SlateBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clean single-line API Key input
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            placeholder = { Text("Paste $selectedProvider API Key") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (customApiKey.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                if (customApiKey.trim().length >= 10) {
                                                    Toast.makeText(context, "Key verified for $selectedProvider", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Key appears too short", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text("Test", fontWeight = FontWeight.Medium, color = Color(0xFF8E8E93), fontSize = 13.sp)
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            AppPreferences.setCustomApiKey(context, customApiKey)
                                            Toast.makeText(context, "API Key saved", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Save", fontWeight = FontWeight.Bold, color = CobaltBlue)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CobaltBlue,
                                unfocusedBorderColor = SlateBorder
                            )
                        )
                    }
                }
            }

            // =========================================================
            // Section 4: Build & App Info
            // =========================================================
            SettingsSection(title = "ABOUT & STORAGE") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "App-Private Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Srutam v1.2.0 (Build 42)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CeramicWhite.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, SlateBorder),
            shadowElevation = 2.dp,
            content = { Column(content = content) }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconBg: Color = CobaltContainer,
    iconTint: Color = CobaltBlue,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CobaltBlue
            )
        )
    }
}
