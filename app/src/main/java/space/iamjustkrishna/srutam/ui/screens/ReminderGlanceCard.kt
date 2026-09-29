package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderType
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState

/**
 * Card for one scheduled event or target date in the expanded Dates & reminders row.
 * Tap the card to edit. The footer carries the source note and two quiet icon actions.
 */
@Composable
internal fun CompactReminderCard(
    item: ReminderEntity,
    state: InsightsUiState,
    open: (Long) -> Unit,
    onReview: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: Long = System.currentTimeMillis()
) {
    val dark = LocalIsCosmicDark.current
    val isTarget = item.type == ReminderType.MILESTONE || !item.notificationEnabled
    val timing = remember(item, nowMs) { formatGlanceReminderTiming(item, nowMs) }

    val accent = if (isTarget) {
        if (dark) StardustGold else Color(0xFFD97706)
    } else {
        if (dark) CosmicGlowBlue else CobaltBlue
    }
    val tintTop = if (isTarget) {
        if (dark) Color(0xFF78350F).copy(alpha = 0.30f) else Color(0xFFFFF7E0)
    } else {
        if (dark) CobaltBlue.copy(alpha = 0.22f) else Color(0xFFEAF2FF)
    }
    val base = if (dark) CosmicVoidCard else Color.White

    Surface(
        onClick = onReview,
        shape = RoundedCornerShape(20.dp),
        color = base,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else accent.copy(alpha = 0.18f)),
        shadowElevation = if (dark) 0.dp else 2.dp,
        modifier = modifier.semantics {
            contentDescription = "${if (isTarget) "Target date" else "Scheduled reminder"}: ${item.title}, $timing"
        }
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(tintTop, base), endY = 260f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = if (dark) 0.22f else 0.14f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTarget) Icons.Default.Flag else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isTarget) "TARGET DATE" else "SCHEDULED",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = accent.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                    Text(
                        text = timing,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = item.title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) TextOnDarkPrimary else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightSourceChip(
                    id = item.recordingId,
                    state = state,
                    open = open,
                    compact = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReminderRoundAction(
                        icon = Icons.Default.Close,
                        description = "Dismiss ${item.title}",
                        onClick = onDismiss,
                        container = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                        tint = if (dark) TextOnDarkSecondary else TextSecondary
                    )
                    ReminderRoundAction(
                        icon = Icons.Default.Check,
                        description = "Mark ${item.title} done",
                        onClick = onDone,
                        container = if (dark) CosmicAuroraGreen.copy(alpha = 0.22f) else EmeraldContainer,
                        tint = if (dark) CosmicAuroraGreen else OnEmeraldContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReminderRoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    container: Color,
    tint: Color
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
