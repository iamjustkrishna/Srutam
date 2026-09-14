package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.*

data class TabletFolderItem(
    val name: String,
    val count: Int = 0
)

val DEFAULT_TABLET_FOLDERS = listOf(
    TabletFolderItem("All Notes", 12),
    TabletFolderItem("Work", 24),
    TabletFolderItem("Personal", 12),
    TabletFolderItem("Meetings", 18),
    TabletFolderItem("Ideas", 9),
    TabletFolderItem("Archive", 4)
)

@Composable
fun TabletSideNavRail(
    currentTab: RootTab,
    onTabSelected: (RootTab) -> Unit,
    onSettingsClick: () -> Unit,
    selectedFolder: String = "All Notes",
    onFolderSelected: (String) -> Unit = {},
    isCompact: Boolean = false,
    storageUsedText: String = "2.4 GB used",
    storageTotalText: String = "10 GB",
    storagePercent: Float = 0.24f,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val railBg = if (isDark) CosmicVoidCard else Color.White
    val railBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary
    val activePillBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEFF6FF)
    val activeAccent = if (isDark) CosmicGlowBlue else CobaltBlue

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isCompact) 76.dp else 232.dp),
        color = railBg,
        border = BorderStroke(1.dp, railBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = 20.dp),
            horizontalAlignment = if (isCompact) Alignment.CenterHorizontally else Alignment.Start
        ) {
            // Brand Title / Header
            if (isCompact) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activePillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = activeAccent
                    )
                }
            } else {
                Column(modifier = Modifier.padding(start = 4.dp, bottom = 24.dp)) {
                    Text(
                        text = "Srutam",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pure Voice, Crystallized Thought",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 4.dp))

            // Primary Navigation Items
            TabletNavItem(
                icon = Icons.AutoMirrored.Filled.Article,
                label = "Notes",
                isSelected = currentTab == RootTab.NOTES,
                isCompact = isCompact,
                onClick = { onTabSelected(RootTab.NOTES) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TabletNavItem(
                icon = Icons.Default.Lightbulb,
                label = "Insights",
                isSelected = currentTab == RootTab.ACTIONS,
                isCompact = isCompact,
                onClick = { onTabSelected(RootTab.ACTIONS) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TabletNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "AI",
                isSelected = currentTab == RootTab.AI,
                isCompact = isCompact,
                onClick = { onTabSelected(RootTab.AI) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TabletNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = false,
                isCompact = isCompact,
                onClick = onSettingsClick
            )

            if (!isCompact) {
                Spacer(modifier = Modifier.height(28.dp))

                // Folders Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folders",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Folder List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    DEFAULT_TABLET_FOLDERS.forEach { folder ->
                        val isFolderSelected = folder.name == selectedFolder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isFolderSelected) activePillBg else Color.Transparent)
                                .clickable { onFolderSelected(folder.name) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = folder.name,
                                tint = if (isFolderSelected) activeAccent else textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = folder.name,
                                fontSize = 13.sp,
                                fontWeight = if (isFolderSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isFolderSelected) activeAccent else textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Local First Storage Gauge Card at Bottom
                Spacer(modifier = Modifier.height(16.dp))
                TabletLocalFirstCard(
                    storageUsedText = storageUsedText,
                    storageTotalText = storageTotalText,
                    storagePercent = storagePercent
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabletNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val activePillBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEFF6FF)
    val activeAccent = if (isDark) CosmicGlowBlue else CobaltBlue
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activePillBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = if (isCompact) 6.dp else 12.dp),
        contentAlignment = if (isCompact) Alignment.Center else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeAccent else textSecondary,
                modifier = Modifier.size(20.dp)
            )
            if (!isCompact) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) activeAccent else textPrimary
                )
            }
        }
    }
}

@Composable
fun TabletLocalFirstCard(
    storageUsedText: String,
    storageTotalText: String,
    storagePercent: Float,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val cardBg = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local First",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your data stays on your device.",
                fontSize = 11.sp,
                color = textSecondary,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { storagePercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (isDark) CosmicGlowBlue else CobaltBlue,
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = storageUsedText,
                    fontSize = 10.sp,
                    color = textSecondary
                )
                Text(
                    text = storageTotalText,
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }
    }
}
