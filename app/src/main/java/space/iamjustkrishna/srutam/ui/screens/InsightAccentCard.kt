@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState

/**
 * Idea and Decision card: 3-line clamp with tap to expand,
 * a "Why" block for decision rationale, and a quiet footer with source and next step.
 */
@Composable
internal fun InsightAccentCard(
    insight: InsightEntity,
    isIdea: Boolean,
    state: InsightsUiState,
    open: (Long) -> Unit,
    hasNextStep: Boolean,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsCosmicDark.current
    val badgeContainer = if (isIdea) {
        if (dark) Color(0xFF4C1D95).copy(alpha = 0.35f) else Color(0xFFEDE9FE)
    } else {
        if (dark) Color(0xFF06281E) else EmeraldContainer
    }
    val badgeContent = if (isIdea) {
        if (dark) CosmicGlowPurple else Color(0xFF7C3AED)
    } else {
        if (dark) CosmicAuroraGreen else OnEmeraldContainer
    }
    val rationale = insight.rationale?.takeIf { !isIdea && it.isNotBlank() }

    var expanded by rememberSaveable(insight.id) { mutableStateOf(false) }
    var overflowing by remember(insight.id) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (dark) CosmicVoidCard else CeramicWhite,
        contentColor = if (dark) TextOnDarkPrimary else TextPrimary,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder),
        shadowElevation = if (dark) 0.dp else 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightPillBadge(
                        text = if (isIdea) "✦ Idea" else "⚖ Decided",
                        containerColor = badgeContainer,
                        contentColor = badgeContent
                    )
                    Text(
                        text = formatHumanRelativeDate(insight.createdAt),
                        fontSize = 11.5.sp,
                        color = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = overflowing || expanded) { expanded = !expanded }
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = insight.text,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) TextOnDarkPrimary else TextPrimary,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }
                    )

                    if (rationale != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (dark) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "WHY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = badgeContent
                                )
                                Text(
                                    text = rationale,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = if (dark) TextOnDarkSecondary else TextSecondary,
                                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (overflowing || expanded) {
                        Text(
                            text = if (expanded) "Show less" else "Show more",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) CosmicGlowBlue else CobaltBlue
                        )
                    }
                }

                HorizontalDivider(color = if (dark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.6f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightSourceChip(
                        id = insight.recordingId,
                        state = state,
                        open = open,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isIdea) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = onNextStep,
                            shape = RoundedCornerShape(8.dp),
                            color = if (dark) CobaltBlue.copy(alpha = 0.22f) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasNextStep) Icons.Default.Visibility else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (dark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (hasNextStep) "View step" else "Next step",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dark) CosmicGlowBlue else CobaltBlue,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
