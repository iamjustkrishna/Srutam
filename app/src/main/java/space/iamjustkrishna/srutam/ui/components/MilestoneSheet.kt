package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.utils.TimeSaved
import space.iamjustkrishna.srutam.utils.TimeSavedMilestones

/**
 * Shown once when the total time saved crosses a milestone (5 minutes, 30 minutes, an hour...).
 * A big plain number, one sentence, and a way to share it. Always dark, like the share card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneSheet(
    step: Long,
    allTimeSeconds: Long,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val next = TimeSavedMilestones.STEPS.firstOrNull { it > allTimeSeconds }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Void2,
        contentColor = Color.White
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .semantics { contentDescription = "Milestone: you have saved ${TimeSavedMilestones.label(step)} by talking instead of typing." },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MILESTONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.9.sp, color = Glow)
            Spacer(Modifier.height(8.dp))
            Text(
                text = TimeSaved.hero(step),
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 64.sp,
                color = Color.White,
                style = LINING,
                maxLines = 1
            )
            Text("saved by talking instead of typing", fontSize = 15.sp, color = Mist, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (next != null) "Next stop: ${TimeSavedMilestones.label(next)}." else "That's a lot of typing you skipped.",
                fontSize = 14.sp,
                color = Ice,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                SheetButton("Not now", filled = false, modifier = Modifier.weight(1f), onClick = onDismiss)
                SheetButton("Share", filled = true, modifier = Modifier.weight(1f), onClick = onShare)
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) Cobalt else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
