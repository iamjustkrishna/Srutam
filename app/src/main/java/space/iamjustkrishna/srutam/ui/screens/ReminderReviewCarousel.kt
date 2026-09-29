package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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

@Composable
fun ReminderReviewCarousel(
    remindersToReview: List<ReminderEntity>,
    state: InsightsUiState,
    onRecordingClick: (Long) -> Unit,
    onReview: (ReminderEntity) -> Unit,
    onDismiss: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (remindersToReview.isEmpty()) return

    val dark = LocalIsCosmicDark.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("reminder_review_carousel")
    ) {
        // Compact Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (dark) StardustGold else Color(0xFFD97706),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "To Review",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) TextOnDarkPrimary else TextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (dark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7)
            ) {
                Text(
                    text = "${remindersToReview.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) StardustGold else Color(0xFFD97706),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        val cardWidth = ((LocalConfiguration.current.screenWidthDp - 32) * 0.82f).dp
        val rowState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(rowState)

        // Horizontal Carousel matching CompactReminderCard styling and snap fling
        LazyRow(
            state = rowState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            itemsIndexed(remindersToReview, key = { _, item -> item.id }) { index, reminder ->
                val isMilestone = reminder.type == ReminderType.MILESTONE
                val timing = remember(reminder, state.now) { formatGlanceReminderTiming(reminder, state.now) }

                val accent = if (isMilestone) {
                    if (dark) StardustGold else Color(0xFFD97706)
                } else {
                    if (dark) CosmicGlowBlue else CobaltBlue
                }
                val tintTop = if (isMilestone) {
                    if (dark) Color(0xFF78350F).copy(alpha = 0.30f) else Color(0xFFFFF7E0)
                } else {
                    if (dark) CobaltBlue.copy(alpha = 0.22f) else Color(0xFFEAF2FF)
                }
                val base = if (dark) CosmicVoidCard else Color.White

                Surface(
                    onClick = { onReview(reminder) },
                    shape = RoundedCornerShape(20.dp),
                    color = base,
                    border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else accent.copy(alpha = 0.18f)),
                    shadowElevation = if (dark) 0.dp else 2.dp,
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag("review_card_${reminder.id}")
                        .semantics {
                            contentDescription = "Review ${if (isMilestone) "target date" else "reminder"}: ${reminder.title}, $timing"
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(tintTop, base), endY = 260f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top row: squircle icon badge + overline + timing + index counter on right
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
                                    imageVector = if (isMilestone) Icons.Default.Flag else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMilestone) "TARGET DATE • REVIEW" else "NEEDS REVIEW",
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
                            if (remindersToReview.size > 1) {
                                Text(
                                    text = "${index + 1}/${remindersToReview.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) TextOnDarkSecondary else TextMuted
                                )
                            }
                        }

                        // Title
                        Text(
                            text = reminder.title,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) TextOnDarkPrimary else TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heightIn(min = 40.dp)
                        )

                        // Footer row: Source note chip + quiet circular action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InsightSourceChip(
                                id = reminder.recordingId,
                                state = state,
                                open = onRecordingClick,
                                compact = true,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ReminderRoundAction(
                                    icon = Icons.Default.Close,
                                    description = "Dismiss ${reminder.title}",
                                    onClick = { onDismiss(reminder) },
                                    container = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                                    tint = if (dark) TextOnDarkSecondary else TextSecondary
                                )
                                ReminderRoundAction(
                                    icon = Icons.Default.Edit,
                                    description = "Review and schedule ${reminder.title}",
                                    onClick = { onReview(reminder) },
                                    container = if (dark) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.12f),
                                    tint = accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
