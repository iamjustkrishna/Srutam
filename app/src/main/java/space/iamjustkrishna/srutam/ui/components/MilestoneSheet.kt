package space.iamjustkrishna.srutam.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.utils.StatsConfig
import space.iamjustkrishna.srutam.utils.TimeSaved
import space.iamjustkrishna.srutam.utils.TimeSavedMilestones

/**
 * Shown once when the total time saved crosses a milestone. A calm sheet in the app's own theme:
 * the card that would be shared, tilted a little, a name for the moment, how far the next one is,
 * and one button that says what it does. [card] is what "Share this card" posts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneSheet(
    step: Long,
    allTimeSeconds: Long,
    card: TimeSavedCardData?,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
    val sub = if (dark) TextOnDarkSecondary else Color(0xFF64748B)
    val next = StatsConfig.nextAfter(allTimeSeconds)
    val context = LocalContext.current

    // The thumbnail is the real card, drawn small.
    val thumb by produceState<Bitmap?>(null, card) {
        value = if (card == null) null else withContext(Dispatchers.Default) {
            runCatching {
                val full = TimeSavedCardRenderer.render(context, card)
                Bitmap.createScaledBitmap(full, 224, 280, true).also { if (it !== full) full.recycle() }
            }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .semantics { contentDescription = "Milestone: you have saved ${TimeSavedMilestones.label(step)} by talking instead of typing." },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (card != null) {
                Box(
                    Modifier
                        .padding(top = 2.dp, bottom = 14.dp)
                        .rotate(-3f)
                        .shadow(14.dp, RoundedCornerShape(10.dp), ambientColor = Void, spotColor = Void)
                        .size(width = 112.dp, height = 140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Void)
                ) {
                    thumb?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth()) }
                }
            }
            Text(
                TimeSavedMilestones.title(step),
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "You've saved ${TimeSavedMilestones.label(step)} by talking instead of typing.",
                fontSize = 14.5.sp,
                color = sub,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Next stop: ${TimeSavedMilestones.label(next)} · ${TimeSaved.hero(next - allTimeSeconds)} to go",
                fontSize = 12.5.sp,
                color = sub,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            PillButton(
                "Share this card", onShare,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                icon = Icons.Default.Share
            )
            PillButton("Not now", onDismiss, style = PillStyle.Text, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
