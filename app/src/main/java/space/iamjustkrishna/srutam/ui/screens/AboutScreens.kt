package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.ui.components.CosmicBackground
import space.iamjustkrishna.srutam.ui.theme.CeramicWhite
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import space.iamjustkrishna.srutam.ui.theme.EmeraldContainer
import space.iamjustkrishna.srutam.ui.theme.EmeraldSuccess
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.SlateBorder
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.ui.theme.TextPrimary
import space.iamjustkrishna.srutam.ui.theme.TextSecondary
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.AudioFileReader

/** The top bar for Settings and its sub-screens: back arrow and a serif title on the page background. */
@Composable
internal fun SettingsTopBar(title: String, onNavigateBack: () -> Unit) {
    val isDark = LocalIsCosmicDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = if (isDark) TextOnDarkPrimary else TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            title,
            fontSize = 21.sp,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (isDark) TextOnDarkPrimary else TextPrimary
        )
    }
}

/** A Settings sub-screen: the same top bar and background as Settings, with [content] in its column. */
@Composable
private fun SettingsSubScreen(title: String, onNavigateBack: () -> Unit, content: @Composable () -> Unit) {
    val isDark = LocalIsCosmicDark.current
    Scaffold(
        topBar = { SettingsTopBar(title, onNavigateBack) },
        containerColor = if (isDark) CosmicVoidBackground else SettingsLightBackground
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isDark) CosmicBackground()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) { content() }
        }
    }
}

/** About: the app, where it keeps things, its version, and the way into Privacy. */
@Composable
fun AboutScreen(onNavigateBack: () -> Unit, onOpenPrivacy: () -> Unit) {
    val isDark = LocalIsCosmicDark.current
    val ink = if (isDark) TextOnDarkPrimary else TextPrimary
    val sub = if (isDark) TextOnDarkSecondary else TextSecondary
    val recordingsDir = remember { AudioFileReader.getRecordingsDirectory() }
    val audioFiles = remember { recordingsDir.listFiles() ?: emptyArray() }
    val totalBytes = remember { audioFiles.sumOf { it.length() } }
    val formattedStorage = remember(totalBytes) {
        when {
            totalBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", totalBytes / (1024f * 1024f))
            totalBytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", totalBytes / 1024f)
            else -> "$totalBytes B"
        }
    }

    SettingsSubScreen("About", onNavigateBack) {
        SettingsSection(title = "SRUTAM") {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.srutam_final_log),
                        contentDescription = "Srutam App Logo",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Srutam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                        Text("Intelligent Voice and Thought Engine", style = MaterialTheme.typography.bodySmall, color = sub)
                    }
                }
                HorizontalDivider(Modifier.padding(bottom = 14.dp), color = if (isDark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                AboutLine("Storage Location", "App-Private Music", ink, sub)
                Spacer(Modifier.height(12.dp))
                AboutLine("Audio Storage", "$formattedStorage (${audioFiles.size} notes)", ink, sub)
                Spacer(Modifier.height(12.dp))
                AboutLine("Version", "Srutam v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", ink, sub)
            }
        }
        SettingsSection(title = "PRIVACY") {
            SettingsLinkRow(
                icon = Icons.Default.Shield,
                title = "Privacy",
                subtitle = "What stays on your phone, and usage data",
                onClick = onOpenPrivacy
            )
        }
    }
}

@Composable
private fun AboutLine(label: String, value: String, ink: Color, sub: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ink)
        Text(value, style = MaterialTheme.typography.bodySmall, color = sub)
    }
}

/** Privacy: what stays on the phone, and the one switch for usage analytics (on by default, never pushed). */
@Composable
fun PrivacyScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val isDark = LocalIsCosmicDark.current
    val ink = if (isDark) TextOnDarkPrimary else TextPrimary
    val sub = if (isDark) TextOnDarkSecondary else TextSecondary
    var analyticsEnabled by remember { mutableStateOf(AppPreferences.isAnalyticsEnabled(context)) }

    SettingsSubScreen("Privacy", onNavigateBack) {
        SettingsSection(title = "ON YOUR PHONE") {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) EmeraldContainer.copy(alpha = 0.2f) else EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp)) }
                Column {
                    Text("Your audio stays on this phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                    Text(
                        "Recordings and transcripts are made on the device. Only what you choose to sync with Srutam Cloud leaves it.",
                        style = MaterialTheme.typography.bodySmall, color = sub
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSection(title = "USAGE DATA") {
                SettingsToggleRow(
                    icon = Icons.Default.BarChart,
                    title = "Usage analytics",
                    subtitle = "Anonymous counts of which features are used, and what goes wrong. Never what you say.",
                    checked = analyticsEnabled,
                    onCheckedChange = { enabled ->
                        analyticsEnabled = enabled
                        Analytics.setEnabled(context, enabled)
                    }
                )
            }
            Text(
                "Turning it off stops the counts. Srutam works the same either way.",
                fontSize = 12.5.sp, color = sub, modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
