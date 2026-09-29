package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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

        val cardWidth = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 32.dp) * 0.82f

        // Horizontal Carousel (82% width with peek of next card)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            itemsIndexed(remindersToReview, key = { _, item -> item.id }) { index, reminder ->
                val isMilestone = reminder.type == ReminderType.MILESTONE
                val badgeLabel = if (isMilestone) "TARGET" else "EVENT"
                val badgeContainer = if (isMilestone) {
                    if (dark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
                } else {
                    if (dark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFDBEAFE)
                }
                val badgeContent = if (isMilestone) {
                    if (dark) StardustGold else Color(0xFFD97706)
                } else {
                    if (dark) CosmicGlowBlue else CobaltBlue
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (dark) CosmicVoidCard else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (dark) 0.dp else 1.dp,
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag("review_card_${reminder.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge + Index row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InsightPillBadge(
                                text = badgeLabel,
                                containerColor = badgeContainer,
                                contentColor = badgeContent
                            )

                            if (remindersToReview.size > 1) {
                                Text(
                                    text = "${index + 1}/${remindersToReview.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) TextOnDarkSecondary else TextMuted
                                )
                            }
                        }

                        // Title
                        Text(
                            text = reminder.title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) TextOnDarkPrimary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Date status + Source chip (single compact row)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = reminderDate(reminder).let {
                                    if (it.length > 22) it.take(22) + "..." else it
                                },
                                fontSize = 10.5.sp,
                                color = if (dark) TextOnDarkSecondary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            InsightSourceChip(
                                id = reminder.recordingId,
                                state = state,
                                open = onRecordingClick,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        // Compact Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Review & Schedule (Primary)
                            Surface(
                                onClick = { onReview(reminder) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (dark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFEFF6FF),
                                border = BorderStroke(
                                    1.dp,
                                    if (dark) CosmicGlowBlue.copy(alpha = 0.5f) else Color(0xFFBFDBFE)
                                ),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Review",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (dark) CosmicGlowBlue else CobaltBlue
                                    )
                                }
                            }

                            // Dismiss (Destructive/Subtle)
                            Surface(
                                onClick = { onDismiss(reminder) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (dark) Color(0xFF7F1D1D).copy(alpha = 0.25f) else Color(0xFFFEF2F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (dark) Color(0xFF991B1B).copy(alpha = 0.4f) else Color(0xFFFECACA)
                                ),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (dark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
