package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.ui.screens.formatDate
import space.iamjustkrishna.srutam.ui.screens.formatDuration
import space.iamjustkrishna.srutam.ui.screens.formatFileSize
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCard
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary

enum class DialogBadgeType {
    PRIMARY,
    DESTRUCTIVE,
    WARNING,
    INFO,
    SUCCESS
}

@Composable
fun SrutamDialogIconBadge(
    icon: ImageVector,
    badgeType: DialogBadgeType = DialogBadgeType.PRIMARY,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    innerSize: Dp = 44.dp,
    iconSize: Dp = 20.dp
) {
    val isDark = LocalIsCosmicDark.current

    val outerGradientColors = when (badgeType) {
        DialogBadgeType.DESTRUCTIVE -> if (isDark) {
            listOf(Color(0xFF5F1D1D).copy(alpha = 0.7f), Color(0xFF2E1010))
        } else {
            listOf(Color(0xFFFEE2E2), Color(0xFFFEF2F2))
        }
        DialogBadgeType.PRIMARY -> if (isDark) {
            listOf(Color(0xFF1E3A8A).copy(alpha = 0.7f), Color(0xFF0F172A))
        } else {
            listOf(Color(0xFFDBEAFE), Color(0xFFEFF6FF))
        }
        DialogBadgeType.WARNING -> if (isDark) {
            listOf(Color(0xFF78350F).copy(alpha = 0.7f), Color(0xFF1C1917))
        } else {
            listOf(Color(0xFFFEF3C7), Color(0xFFFFFBEB))
        }
        DialogBadgeType.INFO -> if (isDark) {
            listOf(Color(0xFF334155).copy(alpha = 0.7f), Color(0xFF0F172A))
        } else {
            listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9))
        }
        DialogBadgeType.SUCCESS -> if (isDark) {
            listOf(Color(0xFF064E3B).copy(alpha = 0.7f), Color(0xFF0F172A))
        } else {
            listOf(Color(0xFFD1FAE5), Color(0xFFECFDF5))
        }
    }

    val outerBorderColor = when (badgeType) {
        DialogBadgeType.DESTRUCTIVE -> if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)
        DialogBadgeType.PRIMARY -> if (isDark) Color(0xFF2563EB) else Color(0xFFBFDBFE)
        DialogBadgeType.WARNING -> if (isDark) Color(0xFFB45309) else Color(0xFFFDE68A)
        DialogBadgeType.INFO -> if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
        DialogBadgeType.SUCCESS -> if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0)
    }

    val innerGradientColors = when (badgeType) {
        DialogBadgeType.DESTRUCTIVE -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        DialogBadgeType.PRIMARY -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        DialogBadgeType.WARNING -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        DialogBadgeType.INFO -> listOf(Color(0xFF64748B), Color(0xFF475569))
        DialogBadgeType.SUCCESS -> listOf(Color(0xFF10B981), Color(0xFF059669))
    }

    val shadowColor = when (badgeType) {
        DialogBadgeType.DESTRUCTIVE -> Color(0x4DDC2626)
        DialogBadgeType.PRIMARY -> Color(0x4D2563EB)
        DialogBadgeType.WARNING -> Color(0x4DD97706)
        DialogBadgeType.INFO -> Color(0x260F172A)
        DialogBadgeType.SUCCESS -> Color(0x4D059669)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(outerGradientColors),
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isDark) Color.White.copy(alpha = 0.25f) else Color.White,
                        outerBorderColor
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = shadowColor
                )
                .background(
                    brush = Brush.verticalGradient(innerGradientColors),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SrutamDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val buttonBackground = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
        isDestructive -> Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
        else -> Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
    }
    val contentColor = if (enabled) Color.White else Color(0xFFF1F5F9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        spotColor = if (isDestructive) Color(0x40DC2626) else Color(0x402563EB)
                    )
                } else Modifier
            )
            .background(brush = buttonBackground, shape = CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SrutamDialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SrutamCustomDialog(
    onDismissRequest: () -> Unit,
    iconBadge: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit = {},
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val isDark = LocalIsCosmicDark.current
    val surfaceColor = if (isDark) CosmicVoidCard else Color(0xFAFFFFFF)
    val titleColor = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val subtitleColor = if (isDark) TextOnDarkSecondary else Color(0xFF64748B)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.15f),
                CosmicVoidCardBorder
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.45f)
            )
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(12.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = if (isDark) Color(0x80000000) else Color(0x380F172A),
                    ambientColor = if (isDark) Color(0x60000000) else Color(0x180F172A)
                ),
            shape = RoundedCornerShape(26.dp),
            color = surfaceColor,
            border = BorderStroke(
                width = 1.dp,
                brush = borderBrush
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                iconBadge()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            dismissButton()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
fun SrutamStandardDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    badgeType: DialogBadgeType = DialogBadgeType.PRIMARY,
    confirmText: String = "Confirm",
    dismissText: String? = "Cancel",
    isDestructive: Boolean = badgeType == DialogBadgeType.DESTRUCTIVE,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    content: @Composable () -> Unit = {}
) {
    SrutamCustomDialog(
        onDismissRequest = onDismissRequest,
        iconBadge = {
            SrutamDialogIconBadge(
                icon = icon,
                badgeType = badgeType
            )
        },
        title = title,
        subtitle = subtitle,
        content = content,
        confirmButton = {
            SrutamDialogConfirmButton(
                text = confirmText,
                onClick = onConfirm,
                isDestructive = isDestructive,
                enabled = confirmEnabled
            )
        },
        dismissButton = if (dismissText != null) {
            {
                SrutamDialogDismissButton(
                    text = dismissText,
                    onClick = onDismiss
                )
            }
        } else null
    )
}

@Composable
fun DeleteConfirmationDialog(
    recordingName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SrutamStandardDialog(
        onDismissRequest = onDismiss,
        title = "Delete Recording?",
        subtitle = "Are you sure you want to permanently delete \"$recordingName\"? This action cannot be undone.",
        icon = Icons.Outlined.Delete,
        badgeType = DialogBadgeType.DESTRUCTIVE,
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun MultiDeleteConfirmationDialog(
    recordingNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val listCardBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val listCardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val itemTextColor = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    SrutamStandardDialog(
        onDismissRequest = onDismiss,
        title = "Delete ${recordingNames.size} Recording${if (recordingNames.size > 1) "s" else ""}?",
        subtitle = "You are about to permanently delete the following recordings:",
        icon = Icons.Outlined.Delete,
        badgeType = DialogBadgeType.DESTRUCTIVE,
        confirmText = "Delete All",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = listCardBackground,
                border = BorderStroke(1.dp, listCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recordingNames) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = itemTextColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SaveRecordingDialog(
    defaultName: String,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var text by remember { mutableStateOf(defaultName) }
    val isNameValid = text.trim().isNotEmpty()
    val isDark = LocalIsCosmicDark.current
    val fieldBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val fieldBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val fieldTextColor = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    SrutamStandardDialog(
        onDismissRequest = onDiscard,
        title = "Save Voice Note",
        subtitle = "Give your new recording a name or keep the default.",
        icon = Icons.Default.Mic,
        badgeType = DialogBadgeType.PRIMARY,
        confirmText = "Save Note",
        dismissText = "Discard",
        confirmEnabled = isNameValid,
        onConfirm = {
            if (isNameValid) {
                onSave(text.trim())
            }
        },
        onDismiss = onDiscard,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(
                            onClick = { text = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear text",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldBackground,
                    unfocusedContainerColor = fieldBackground,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = fieldBorder,
                    focusedTextColor = fieldTextColor,
                    unfocusedTextColor = fieldTextColor
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    val isDark = LocalIsCosmicDark.current
    val fieldBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val fieldBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val fieldTextColor = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    SrutamStandardDialog(
        onDismissRequest = onDismiss,
        title = "Rename Recording",
        subtitle = "Enter a new title for this audio recording.",
        icon = Icons.Outlined.Edit,
        badgeType = DialogBadgeType.PRIMARY,
        confirmText = "Rename",
        dismissText = "Cancel",
        onConfirm = {
            onRename(text.takeIf { it.isNotBlank() } ?: currentName)
        },
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(
                            onClick = { text = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear text",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldBackground,
                    unfocusedContainerColor = fieldBackground,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = fieldBorder,
                    focusedTextColor = fieldTextColor,
                    unfocusedTextColor = fieldTextColor
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    )
}

@Composable
fun AudioInfoDialog(
    displayName: String,
    audioFile: AudioFileInfo,
    recording: Recording? = null,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val cardBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val dividerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val locationText = if (audioFile.filePath == "srutam://welcome") {
        "Built-in Guide (srutam://welcome)"
    } else {
        audioFile.filePath
    }

    val aiStatusText = when {
        recording == null -> "Not processed"
        recording.isProcessing || recording.aiStatus == RecordingAiStatus.TRANSCRIBING -> "Transcribing audio..."
        recording.aiStatus == RecordingAiStatus.SUMMARY_PROCESSING -> "Analyzing insights..."
        recording.aiStatus == RecordingAiStatus.ERROR -> "Error processing"
        !recording.summary.isNullOrBlank() -> "Summarized (AI Insights ready)"
        else -> "Ready"
    }

    SrutamCustomDialog(
        onDismissRequest = onDismiss,
        iconBadge = {
            SrutamDialogIconBadge(
                icon = Icons.Outlined.Info,
                badgeType = DialogBadgeType.INFO
            )
        },
        title = "File Info",
        subtitle = "Audio metadata and storage location details.",
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DialogInfoRow(label = "Title", value = displayName)
                    HorizontalDivider(color = dividerColor)
                    DialogInfoRow(label = "Duration", value = formatDuration(audioFile.duration))
                    HorizontalDivider(color = dividerColor)
                    DialogInfoRow(label = "Recorded Date", value = formatDate(audioFile.timestamp))
                    HorizontalDivider(color = dividerColor)
                    DialogInfoRow(label = "File Size", value = formatFileSize(audioFile.sizeBytes))
                    HorizontalDivider(color = dividerColor)
                    DialogInfoRow(label = "AI Status", value = aiStatusText)
                    HorizontalDivider(color = dividerColor)
                    DialogInfoRow(label = "File Location", value = locationText)
                }
            }
        },
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Done",
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun ArchiveTasksDialog(
    taskCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SrutamStandardDialog(
        onDismissRequest = onDismiss,
        title = "Archive Completed Tasks",
        subtitle = "Archive $taskCount completed item${if (taskCount > 1) "s" else ""}? They will be hidden from your active task view. You can restore them anytime.",
        icon = Icons.Default.Archive,
        badgeType = DialogBadgeType.WARNING,
        confirmText = "Archive",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DialogInfoRow(label: String, value: String) {
    val isDark = LocalIsCosmicDark.current
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val valueColor = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
