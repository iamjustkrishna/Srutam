package space.iamjustkrishna.srutam.ui.mesh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.ui.screens.formatHumanRelativeDate
import space.iamjustkrishna.srutam.ui.theme.*

@Composable
fun MeshNodeDetailCard(
    node: MeshNode?,
    connectedNodes: List<MeshNode>,
    onSelectNode: (MeshNode) -> Unit,
    onRecordingClick: (Long) -> Unit,
    onActionToggle: (InsightEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = node != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (node != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CeramicWhite.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Top Drag Indicator & Header Row
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SlateBorder)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Node Kind & Status Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (badgeBg, badgeFg, icon) = when (node.kind) {
                                MeshNodeKind.ACTION -> Triple(CobaltContainer, CobaltBlue, Icons.Default.TaskAlt)
                                MeshNodeKind.IDEA -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Lightbulb)
                                MeshNodeKind.DECISION -> Triple(EmeraldContainer, EmeraldSuccess, Icons.Default.CheckCircle)
                                MeshNodeKind.RECORDING -> Triple(SlateGrouped, Color(0xFF334155), Icons.Default.GraphicEq)
                                MeshNodeKind.THEME -> Triple(Color(0xFFF3E8FF), Color(0xFF8B5CF6), Icons.Default.Hub)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = badgeFg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = node.kind.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeFg,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            if (node.status != null) {
                                val isDone = node.status == InsightStatus.COMPLETED
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDone) EmeraldContainer else SlateGrouped,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isDone) "COMPLETED" else "OPEN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDone) EmeraldSuccess else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Close Action
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close details",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Content Text
                    Text(
                        text = node.fullText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )

                    // Evidence / Transcript Quote Block
                    if (!node.evidence.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 135.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(node.color)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 110.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = if (node.kind == MeshNodeKind.RECORDING) "TRANSCRIPT" else "TRANSCRIPT EVIDENCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "\"${node.evidence}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Rationale Block
                    if (!node.rationale.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = node.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Source Note Row or Action Button
                    if (node.recordingId != null && node.recordingId > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (node.kind == MeshNodeKind.RECORDING) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CobaltContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRecordingClick(node.recordingId) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = CobaltBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Voice Note & Player",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = CobaltBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SlateGrouped.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRecordingClick(node.recordingId) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(CobaltContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = null,
                                                tint = CobaltBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = node.recordingName ?: "Source Voice Note",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (node.timestamp > 0) {
                                                Text(
                                                    text = formatHumanRelativeDate(node.timestamp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Open Note",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CobaltBlue
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = CobaltBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Toggle Button (if this is an Action Item)
                    if (node.rawInsight != null && node.kind == MeshNodeKind.ACTION) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isDone = node.status == InsightStatus.COMPLETED
                        Button(
                            onClick = { onActionToggle(node.rawInsight) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) SlateGrouped else CobaltBlue,
                                contentColor = if (isDone) TextPrimary else CeramicWhite
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.Refresh else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDone) "Reopen Task" else "Mark as Done",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Connected Nodes Carousel / Traverse Row
                    val otherConnected = connectedNodes.filter { it.id != node.id }
                    if (otherConnected.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CONNECTED NODES (${otherConnected.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Tap node to explore",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            otherConnected.forEach { related ->
                                val isCrossNoteIdea = node.kind == MeshNodeKind.IDEA && related.kind == MeshNodeKind.IDEA &&
                                        node.recordingId != null && related.recordingId != null && node.recordingId != related.recordingId

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCrossNoteIdea) Color(0xFFFEF3C7).copy(alpha = 0.6f) else SlateSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCrossNoteIdea) Color(0xFFF59E0B).copy(alpha = 0.7f) else SlateBorder.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .clickable { onSelectNode(related) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        if (isCrossNoteIdea) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(related.color)
                                            )
                                        }
                                        Text(
                                            text = if (isCrossNoteIdea) "${related.label} (Linked Idea)" else related.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCrossNoteIdea) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCrossNoteIdea) Color(0xFF92400E) else TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
