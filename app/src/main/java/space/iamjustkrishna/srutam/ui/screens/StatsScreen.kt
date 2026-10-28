package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.ui.components.CompareBar
import space.iamjustkrishna.srutam.ui.components.Cobalt
import space.iamjustkrishna.srutam.ui.components.Glow
import space.iamjustkrishna.srutam.ui.components.Ice
import space.iamjustkrishna.srutam.ui.components.LINING
import space.iamjustkrishna.srutam.ui.components.Mist
import space.iamjustkrishna.srutam.ui.components.Royal
import space.iamjustkrishna.srutam.ui.components.Slate
import space.iamjustkrishna.srutam.ui.components.Void
import space.iamjustkrishna.srutam.ui.components.Void2
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.utils.TimeSaved

/**
 * Your Srutam: how much time talking has saved, this month and in all. Opened from the line under
 * the title on Notes. Only lengths and word counts feed it, never what a note says.
 */
@Composable
fun StatsScreen(
    recordings: Collection<Recording>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totals = remember(recordings) { TimeSaved.totals(recordings, TimeSaved.monthStart()) }
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF1E2229)

    Column(
        modifier
            .fillMaxSize()
            .background(if (dark) CosmicVoidBackground else Color(0xFFF4F5F8))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ink)
            }
            Text("Your Srutam", fontSize = 22.sp, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold, color = ink)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // On a tablet or in landscape the card keeps a readable width instead of stretching.
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                if (totals.allTimeSeconds < TimeSaved.MIN_WORTH_SHOWING_SECONDS) {
                    EmptyStats(ink)
                } else {
                    StatsPanel(totals)
                    Spacer(Modifier.height(16.dp))
                    HowItsCounted(ink, dark)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatsPanel(totals: TimeSaved.Totals) {
    val sayingShare = if (totals.typingSeconds > 0) (totals.spokenSeconds.toFloat() / totals.typingSeconds).coerceIn(0.05f, 1f) else 1f
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Royal, Void2, Void), start = Offset(0f, 0f), end = Offset(900f, 1200f)))
            .semantics {
                contentDescription = "You have saved ${TimeSaved.words(totals.allTimeSeconds)} by talking, ${TimeSaved.words(totals.thisMonthSeconds)} of it this month."
            }
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("THIS MONTH", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.9.sp, color = Glow)
            Spacer(Modifier.height(4.dp))
            Text(
                text = TimeSaved.hero(totals.thisMonthSeconds),
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp,
                color = Color.White,
                style = LINING,
                maxLines = 1
            )
            Text("saved by talking instead of typing", fontSize = 14.sp, color = Mist)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Fact("ALL TIME", TimeSaved.hero(totals.allTimeSeconds))
                Fact("NOTES", totals.notes.toString())
                Fact("WORDS", totals.words.toString())
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Spacer(Modifier.height(14.dp))
            CompareBar("Typing it", "~${TimeSaved.short(totals.typingSeconds)}", 1f, Brush.horizontalGradient(listOf(Slate, Slate)))
            Spacer(Modifier.height(9.dp))
            CompareBar("Saying it", TimeSaved.short(totals.spokenSeconds), sayingShare, Brush.horizontalGradient(listOf(Cobalt, Glow)))
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Mist)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ice, style = LINING, maxLines = 1)
    }
}

@Composable
private fun HowItsCounted(ink: Color, dark: Boolean) {
    val soft = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
    Column {
        Text("How it's counted", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "For each note, Srutam works out how long typing the same words would take, at ${TimeSaved.TYPING_WORDS_PER_MINUTE} words a minute, " +
                "and subtracts how long you actually spent talking. The difference is the time you saved. " +
                "Only each note's length and word count are used, never what it says.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = soft
        )
    }
}

@Composable
private fun EmptyStats(ink: Color) {
    Column(Modifier.padding(vertical = 40.dp)) {
        Text("Nothing to count yet", fontSize = 20.sp, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Record a voice note and Srutam will show how much time talking saved over typing it.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF64748B)
        )
    }
}
