package space.iamjustkrishna.srutam.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.utils.TimeSaved

internal val Void = Color(0xFF050714)
internal val Void2 = Color(0xFF0B1230)
internal val Royal = Color(0xFF1A3A9C)
internal val Cobalt = Color(0xFF2563EB)
internal val Glow = Color(0xFF5B93FF)
internal val Ice = Color(0xFFE8EEFC)
internal val Mist = Color(0xFF93A3C7)
internal val Slate = Color(0xFF46568A)
internal val Green = Color(0xFF10B981)

/** Playfair's figures are old-style by default (some drop below the line); lining ones read as numbers. */
internal val LINING = TextStyle(fontFeatureSettings = "lnum")

/** "6h 23m": the digits large in white, the units small and blue. Shared by the reward and Your Srutam. */
internal fun heroText(hero: String, digitSize: TextUnit = 56.sp, unitSize: TextUnit = 26.sp): AnnotatedString = buildAnnotatedString {
    Regex("\\d+|[^\\d\\s]+").findAll(hero).forEach { m ->
        val token = m.value
        if (token[0].isDigit()) {
            withStyle(SpanStyle(fontSize = digitSize, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-1.5).sp)) { append(token) }
        } else {
            withStyle(SpanStyle(fontSize = unitSize, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold, color = Glow)) { append(token + " ") }
            append(" ")
        }
    }
}

/**
 * The reward after a first note, in the same style as the Your Srutam hero but small: the time saved
 * counts up, two thin bars say how (typing against saying), and one line says how far the first
 * milestone is. It takes about a third of the room the earlier version did. Always dark, like the
 * share card; with animations turned off in the system settings it shows its final state at once.
 * [onShare], when given, adds a small share button in the corner.
 */
@Composable
fun TimeSavedReward(result: TimeSaved.Result, modifier: Modifier = Modifier, onShare: (() -> Unit)? = null) {
    val context = LocalContext.current
    val animated = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
            .getOrDefault(true)
    }

    val appear = remember { Animatable(if (animated) 0f else 1f) }
    val count = remember { Animatable(if (animated) 0f else result.savedSeconds.toFloat()) }
    val bars = remember { Animatable(if (animated) 0f else 1f) }
    val milestone = remember { Animatable(if (animated) 0f else result.milestoneProgress) }

    LaunchedEffect(result) {
        if (!animated) return@LaunchedEffect
        coroutineScope {
            launch { delay(120); appear.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            launch { delay(400); count.animateTo(result.savedSeconds.toFloat(), tween(1300, easing = FastOutSlowInEasing)) }
            launch { delay(500); bars.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { delay(700); milestone.animateTo(result.milestoneProgress, tween(1100, easing = FastOutSlowInEasing)) }
        }
    }

    val shown = count.value.toLong().coerceAtMost(result.savedSeconds)
    val sayingShare = if (result.typingSeconds > 0) (result.spokenSeconds.toFloat() / result.typingSeconds).coerceIn(0.05f, 1f) else 1f

    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 12.dp.toPx()
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Royal, Void2, Void), start = Offset(0f, 0f), end = Offset(900f, 500f)))
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics {
                contentDescription = "You saved ${TimeSaved.words(result.savedSeconds)}: you said it in ${TimeSaved.words(result.spokenSeconds)}, typing it takes about ${TimeSaved.words(result.typingSeconds)}."
            }
    ) {
        // The number on the left, how it was counted on the right.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(0.42f)) {
                Text("TIME SAVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Glow, maxLines = 1)
                Text(text = heroText(TimeSaved.hero(shown), digitSize = 34.sp, unitSize = 17.sp), maxLines = 1, style = LINING)
                Text("on this note", fontSize = 11.5.sp, color = Mist, maxLines = 1)
            }
            Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CompareBar("Typing", "~${TimeSaved.short(result.typingSeconds)}", 1f * bars.value, Brush.horizontalGradient(listOf(Slate, Slate)), labelWidth = 44.dp, valueWidth = 58.dp)
                CompareBar("Saying", TimeSaved.short(result.spokenSeconds), sayingShare * bars.value, Brush.horizontalGradient(listOf(Cobalt, Glow)), labelWidth = 44.dp, valueWidth = 58.dp)
            }
        }

        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                if (result.reachedFirstMilestone) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(15.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                        Text("First milestone reached: 5 minutes", fontSize = 11.5.sp, color = Ice, maxLines = 1)
                    }
                } else {
                    Text("${TimeSaved.short(result.secondsToFirstMilestone)} to your first milestone (5 min)", fontSize = 11.5.sp, color = Ice, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                        Box(Modifier.fillMaxWidth(milestone.value.coerceIn(0.02f, 1f)).height(4.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Cobalt, Glow))))
                    }
                }
            }
            if (onShare != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onShare)
                        .semantics { contentDescription = "Share" },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp)) }
            }
        }
    }
}

@Composable
internal fun CompareBar(
    label: String,
    value: String,
    fraction: Float,
    fill: Brush,
    labelWidth: androidx.compose.ui.unit.Dp = 62.dp,
    valueWidth: androidx.compose.ui.unit.Dp = 76.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = Mist, maxLines = 1, modifier = Modifier.width(labelWidth))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f))
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(fill))
        }
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(valueWidth))
    }
}
