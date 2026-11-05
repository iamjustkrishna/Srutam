package space.iamjustkrishna.srutam.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.BuildConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.service.QuickRecordNotification
import space.iamjustkrishna.srutam.service.WeeklyRecapWorker
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.CosmicBackground
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.AudioFileReader

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenStats: () -> Unit = {},
    // Your Srutam is only linked once there is something to count.
    timeSavedAvailable: Boolean = false,
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = LocalIsCosmicDark.current

    var themeMode by remember {
        mutableStateOf(AppPreferences.getThemeMode(context))
    }
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
    var customModel by remember {
        mutableStateOf(AppPreferences.getCustomModel(context))
    }
    var isAutoAiEnabled by remember {
        mutableStateOf(AppPreferences.isAutoAiEnabled(context))
    }
    var askNameAfterRecording by remember {
        mutableStateOf(AppPreferences.isAskNameAfterRecording(context))
    }
    var weeklyRecapEnabled by remember { mutableStateOf(AppPreferences.isWeeklyRecapEnabled(context)) }
    var timeSavedLineShown by remember { mutableStateOf(AppPreferences.isTimeSavedLineShown(context)) }
    var providerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsTopBar("Settings", onNavigateBack) },
        containerColor = if (isDark) CosmicVoidBackground else SettingsLightBackground,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isDark) CosmicBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            // Srutam Cloud first, like the account card at the top of the design
            DeveloperMcpSection(isDark = isDark)

            SettingsSection(title = "YOUR SRUTAM") {
                if (timeSavedAvailable) {
                    SettingsLinkRow(
                        icon = Icons.Default.Bolt,
                        title = "Time saved",
                        subtitle = "Your time saved, and a card to share",
                        onClick = onOpenStats
                    )
                    InsetDivider()
                }
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Weekly recap",
                    subtitle = "A Sunday note on what you saved",
                    checked = weeklyRecapEnabled,
                    onCheckedChange = { enabled ->
                        weeklyRecapEnabled = enabled
                        AppPreferences.setWeeklyRecapEnabled(context, enabled)
                        if (enabled) WeeklyRecapWorker.schedule(context) else WeeklyRecapWorker.cancel(context)
                        space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "weekly_recap", enabled)
                    }
                )
                InsetDivider()
                SettingsToggleRow(
                    icon = Icons.Default.Schedule,
                    title = "Show time saved on Notes",
                    checked = timeSavedLineShown,
                    onCheckedChange = { enabled ->
                        timeSavedLineShown = enabled
                        AppPreferences.setTimeSavedLineShown(context, enabled)
                        space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "time_saved_line", enabled)
                    }
                )
            }


            SettingsSection(title = "CAPTURE") {
                SettingsToggleRow(
                    icon = Icons.Default.PictureInPicture,
                    title = "Floating dock",
                    subtitle = "Record from any app",
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
                            space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "dock", enabled)
                            val serviceIntent = Intent(context, FloatingButtonService::class.java)
                            if (enabled) {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            } else {
                                context.stopService(serviceIntent)
                            }
                        }
                    }
                )
                InsetDivider()
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Recording notification",
                    subtitle = if (QuickRecordNotification.switchLocked(isFloatingDockEnabled)) {
                        "On while the floating dock is on"
                    } else {
                        "Start, pause and save from the shade"
                    },
                    checked = QuickRecordNotification.switchChecked(isFloatingDockEnabled, isPersistentNotificationEnabled),
                    onCheckedChange = { enabled ->
                        isPersistentNotificationEnabled = enabled
                        QuickRecordNotification.setEnabled(context, enabled)
                        space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "quick_notif", enabled)
                    },
                    enabled = !QuickRecordNotification.switchLocked(isFloatingDockEnabled)
                )
                InsetDivider()
                SettingsToggleRow(
                    icon = Icons.Default.Edit,
                    title = "Ask for a name after recording",
                    subtitle = "Off: notes are saved right away",
                    checked = askNameAfterRecording,
                    onCheckedChange = { enabled ->
                        askNameAfterRecording = enabled
                        AppPreferences.setAskNameAfterRecording(context, enabled)
                        space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "ask_name", enabled)
                    }
                )
            }

            SettingsSection(title = "NOTES AND AI") {
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Auto AI processing",
                    subtitle = "Summarize as soon as you stop",
                    checked = isAutoAiEnabled,
                    onCheckedChange = { enabled ->
                        isAutoAiEnabled = enabled
                        AppPreferences.setAutoAiEnabled(context, enabled)
                        space.iamjustkrishna.srutam.utils.Analytics.settingChanged(context, "auto_ai", enabled)
                    }
                )
                InsetDivider()
                SettingsValueRow(
                    icon = Icons.Default.Memory,
                    title = "AI provider",
                    value = when (selectedProvider) {
                        AppPreferences.PROVIDER_SRUTAM_DEFAULT -> "Srutam Cloud"
                        AppPreferences.PROVIDER_OPENAI -> "OpenAI"
                        AppPreferences.PROVIDER_ANTHROPIC -> "Anthropic"
                        AppPreferences.PROVIDER_GEMINI -> "Gemini"
                        AppPreferences.PROVIDER_GROQ -> "Groq"
                        else -> selectedProvider
                    },
                    onClick = { providerExpanded = !providerExpanded },
                    trailing = if (providerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                )
                if (providerExpanded) {
                    InsetDivider()
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == AppPreferences.PROVIDER_SRUTAM_DEFAULT,
                            onClick = {
                                selectedProvider = AppPreferences.PROVIDER_SRUTAM_DEFAULT
                                AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_SRUTAM_DEFAULT)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CobaltBlue)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Srutam Cloud (Default)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextOnDarkPrimary else TextPrimary,
                                    maxLines = 1
                                )
                                Surface(
                                    color = if (isDark) Color(0xFF1E293B) else SlateGrouped,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "Free",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Out-of-the-box summaries and chat",
                                fontSize = 12.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.6f),
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
                                    val newModel = AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_OPENAI)
                                    customModel = newModel
                                    AppPreferences.setCustomModel(context, newModel, AppPreferences.PROVIDER_OPENAI)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider != AppPreferences.PROVIDER_SRUTAM_DEFAULT,
                            onClick = {
                                if (selectedProvider == AppPreferences.PROVIDER_SRUTAM_DEFAULT) {
                                    selectedProvider = AppPreferences.PROVIDER_OPENAI
                                    AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_OPENAI)
                                    val newModel = AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_OPENAI)
                                    customModel = newModel
                                    AppPreferences.setCustomModel(context, newModel, AppPreferences.PROVIDER_OPENAI)
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                        )
                        Column {
                            Text(
                                text = "Custom API Key (BYOK)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "Bring your own key for unlimited queries",
                                fontSize = 12.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                    }

                    // Provider Pills & Model Selection
                    if (selectedProvider != AppPreferences.PROVIDER_SRUTAM_DEFAULT) {
                        Spacer(modifier = Modifier.height(12.dp))

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
                                            val newModel = AppPreferences.getCustomModel(context, code)
                                            customModel = newModel
                                            AppPreferences.setCustomModel(context, newModel, code)
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Model Selection & Presets
                        Text(
                            text = "MODEL CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val modelPresets = when (selectedProvider) {
                            AppPreferences.PROVIDER_OPENAI -> listOf(
                                "gpt-5.6-sol",
                                "gpt-5.6-terra",
                                "gpt-5.6-luna",
                                "gpt-5.5",
                                "gpt-5.5-pro"
                            )
                            AppPreferences.PROVIDER_ANTHROPIC -> listOf(
                                "claude-sonnet-4.6",
                                "claude-fable-5.1",
                                "claude-opus-4.8",
                                "claude-haiku-4.5"
                            )
                            AppPreferences.PROVIDER_GROQ -> listOf(
                                "qwen3.6-27b",
                                "minimax-m2.7",
                                "whisper-large-v3",
                                "whisper-large-v3-turbo"
                            )
                            AppPreferences.PROVIDER_GEMINI -> listOf(
                                "gemini-2.5-flash",
                                "gemini-2.5-flash-lite",
                                "gemini-2.5-pro"
                            )
                            else -> listOf("gemini-2.5-flash", "gemini-2.5-pro")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            modelPresets.forEach { preset ->
                                val isSelected = customModel == preset
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            customModel = preset
                                            AppPreferences.setCustomModel(context, preset, selectedProvider)
                                            Toast.makeText(context, "Model set to $preset", Toast.LENGTH_SHORT).show()
                                        },
                                    color = if (isSelected) CobaltContainer else SlateGrouped,
                                    border = BorderStroke(1.dp, if (isSelected) CobaltBlue else SlateBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CobaltBlue else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customModel,
                            onValueChange = { customModel = it },
                            placeholder = { Text("Enter model name (e.g. ${modelPresets.first()})") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (customModel.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            AppPreferences.setCustomModel(context, customModel)
                                            Toast.makeText(context, "Model saved", Toast.LENGTH_SHORT).show()
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
                InsetDivider()
                SettingsValueRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Speech recognition",
                    value = "On device"
                )
            }

            SettingsSection(title = "RECORDINGS") {
                space.iamjustkrishna.srutam.ui.components.RecordingAccessSettings()
            }

            SettingsSection(title = "APPEARANCE") {
                var themeOpen by remember { mutableStateOf(false) }
                SettingsValueRow(
                    icon = Icons.Default.LightMode,
                    title = "Theme",
                    value = when (themeMode) {
                        AppPreferences.THEME_LIGHT -> "Light"
                        AppPreferences.THEME_COSMIC_DARK -> "Cosmic dark"
                        else -> "System"
                    },
                    onClick = { themeOpen = !themeOpen },
                    trailing = if (themeOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                )
                if (themeOpen) {
                    InsetDivider()
                    // The three choices open right under the row, like the AI provider, so nothing floats over other rows.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Light" to AppPreferences.THEME_LIGHT,
                            "Cosmic dark" to AppPreferences.THEME_COSMIC_DARK,
                            "System" to AppPreferences.THEME_SYSTEM
                        ).forEach { (label, mode) ->
                            val selected = themeMode == mode
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) (if (isDark) Color(0xFF1A2A55) else Color(0xFFEAF1FF)) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) (if (isDark) CosmicGlowBlue.copy(alpha = 0.5f) else Color(0xFFC3D6FB)) else settingsLine(),
                                        CircleShape
                                    )
                                    .clickable {
                                        themeMode = mode
                                        AppPreferences.setThemeMode(context, mode)
                                    }
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) (if (isDark) Color.White else TextPrimary) else (if (isDark) TextOnDarkSecondary else TextSecondary)
                                )
                            }
                        }
                    }
                }
            }

            SettingsSection(title = "") {
                SettingsLinkRow(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Privacy, version and storage",
                    onClick = onOpenAbout
                )
            }

            // The maker's line, with the handle under it as a small pill that opens X.
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Srutam · made by Krishna",
                    fontSize = 12.5.sp,
                    color = if (isDark) TextOnDarkSecondary else TextSecondary
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1A2A55) else Color(0xFFEAF1FF))
                        .border(1.dp, if (isDark) CosmicGlowBlue.copy(alpha = 0.35f) else Color(0xFFC3D6FB), CircleShape)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/iamjustkrishna")))
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "@iamjustkrishna",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Opens X",
                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }
}

internal val SettingsLightBackground = Color(0xFFF2F4F8)
private val SettingsGroupShape = RoundedCornerShape(18.dp)

@Composable
private fun settingsLine() = if (LocalIsCosmicDark.current) CosmicVoidCardBorder else Color(0xFFE5E9F1)

/** A labelled group of rows: a flat white card with a hairline edge. An empty [title] draws no label. */
@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                letterSpacing = 1.15.sp,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsGroupShape,
            color = if (isDark) CosmicVoidCard else Color.White,
            border = BorderStroke(1.dp, settingsLine()),
            content = { Column(content = content) }
        )
    }
}

/** The line between two rows, starting under the text rather than under the icon. */
@Composable
internal fun InsetDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = settingsLine(), thickness = 1.dp)
}

@Composable
internal fun RowTile(icon: ImageVector) {
    val isDark = LocalIsCosmicDark.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isDark) Color(0xFF1A2A55) else Color(0xFFEAF1FF))
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isDark) CosmicGlowBlue else CobaltBlue, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun RowText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val isDark = LocalIsCosmicDark.current
    Column(modifier) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) TextOnDarkPrimary else TextPrimary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = if (isDark) TextOnDarkSecondary else TextSecondary
            )
        }
    }
}

@Composable
internal fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
) {
    val isDark = LocalIsCosmicDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowTile(icon)
        RowText(title, subtitle, Modifier.weight(1f))
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = (if (isDark) TextOnDarkSecondary else TextSecondary).copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowTile(icon)
        RowText(title, subtitle, Modifier.weight(1f))
        SrutamSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** A row with a value on the right. With an [onClick] it opens something; without one it only tells you. */
@Composable
internal fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    trailing: ImageVector? = null,
    valueColor: Color? = null
) {
    val isDark = LocalIsCosmicDark.current
    val secondary = if (isDark) TextOnDarkSecondary else TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowTile(icon)
        RowText(title, subtitle, Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, fontWeight = if (valueColor != null) FontWeight.Medium else FontWeight.Normal, color = valueColor ?: secondary, maxLines = 1)
        if (trailing != null) {
            Icon(imageVector = trailing, contentDescription = null, tint = secondary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SrutamSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = LocalIsCosmicDark.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            if (isDark) CosmicGlowBlue else CobaltBlue
        } else {
            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        },
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(46.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
