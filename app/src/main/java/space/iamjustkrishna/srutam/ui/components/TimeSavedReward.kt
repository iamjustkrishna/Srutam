package space.iamjustkrishna.srutam.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.utils.TimeSaved
import kotlin.math.cos
import kotlin.math.sin

private val Void = Color(0xFF050714)
private val Void2 = Color(0xFF0B1230)
private val Royal = Color(0xFF1A3A9C)
private val Cobalt = Color(0xFF2563EB)
private val Glow = Color(0xFF5B93FF)
private val Ice = Color(0xFFE8EEFC)
private val Mist = Color(0xFF93A3C7)
private val Slate = Color(0xFF46568A)
private val Green = Color(0xFF10B981)

/** Playfair's figures are old-style by default (some drop below the line); lining ones read as numbers. */
private val LINING = TextStyle(fontFeatureSettings = "lnum")

/**
 * The reward moment after a note: a ring that fills towards the first milestone while the time
 * saved counts up, a few sparks, and what it means in plain words. Always dark, like a card: it
 * looks the same in light and dark themes. Plays once when it appears; with animations turned off
 * in the system settings, it just shows its final state.
 */
@Composable
fun TimeSavedReward(result: TimeSaved.Result, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animated = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
            .getOrDefault(true)
    }

    val appear = remember { Animatable(if (animated) 0f else 1f) }
    val ring = remember { Animatable(if (animated) 0f else result.milestoneProgress) }
    val count = remember { Animatable(if (animated) 0f else result.savedSeconds.toFloat()) }
    val bars = remember { Animatable(if (animated) 0f else 1f) }
    val pop = remember { Animatable(1f) }
    val sparks = remember { Animatable(if (animated) 0f else 1f) }

    LaunchedEffect(result) {
        if (!animated) return@LaunchedEffect
        coroutineScope {
            launch { delay(150); appear.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { delay(500); ring.animateTo(result.milestoneProgress, tween(1600, easing = FastOutSlowInEasing)) }
            launch { delay(500); count.animateTo(result.savedSeconds.toFloat(), tween(1600, easing = FastOutSlowInEasing)) }
            launch { delay(650); bars.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
            launch {
                delay(2100)
                pop.animateTo(1.07f, tween(160, easing = LinearOutSlowInEasing))
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            launch { delay(2000); sparks.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
        }
    }

    val shown = count.value.toLong().coerceAtMost(result.savedSeconds)
    val sayingShare = if (result.typingSeconds > 0) (result.spokenSeconds.toFloat() / result.typingSeconds).coerceIn(0.05f, 1f) else 1f

    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 18.dp.toPx()
                val s = 0.97f + 0.03f * appear.value
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Royal, Void2, Void), start = Offset(0f, 0f), end = Offset(900f, 1200f)))
            .semantics {
                contentDescription = "You saved ${TimeSaved.words(result.savedSeconds)}: you said it in ${TimeSaved.words(result.spokenSeconds)}, typing it takes about ${TimeSaved.words(result.typingSeconds)}."
            }
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(96.dp)) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                        if (ring.value > 0f) {
                            // A wider, fainter pass underneath gives the arc its glow.
                            drawArc(Glow.copy(alpha = 0.28f), -90f, 360f * ring.value, false, Offset(inset - 3.dp.toPx(), inset - 3.dp.toPx()),
                                Size(arcSize.width + 6.dp.toPx(), arcSize.height + 6.dp.toPx()), style = Stroke(stroke + 6.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(Glow, -90f, 360f * ring.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    }
                    val centre = TimeSaved.hero(shown)
                    Text(
                        text = centre,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (centre.length <= 3) 30.sp else 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = LINING
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("TIME SAVED", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.9.sp, color = Glow)
                    Spacer(Modifier.height(4.dp))
                    val headline = "You got ${TimeSaved.words(result.savedSeconds)} back."
                    Text(
                        text = headline,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (headline.length > 26) 22.sp else 26.sp,
                        lineHeight = if (headline.length > 26) 25.sp else 29.sp,
                        color = Color.White,
                        style = LINING,
                        modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You said that in ${TimeSaved.short(result.spokenSeconds)}. Typing it takes about ${TimeSaved.short(result.typingSeconds)}.",
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Mist
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            CompareBar("Typing it", "~${TimeSaved.short(result.typingSeconds)}", 1f * bars.value, Brush.horizontalGradient(listOf(Slate, Slate)))
            Spacer(Modifier.height(9.dp))
            CompareBar("Saying it", TimeSaved.short(result.spokenSeconds), sayingShare * bars.value, Brush.horizontalGradient(listOf(Cobalt, Glow)))

            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Spacer(Modifier.height(12.dp))
            if (result.reachedFirstMilestone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("First milestone reached", fontSize = 14.sp, color = Ice, fontWeight = FontWeight.Medium)
                        Text("5 minutes saved by talking", fontSize = 12.sp, color = Mist)
                    }
                }
            } else {
                Column {
                    Text("First milestone: 5 minutes saved", fontSize = 14.sp, color = Ice, fontWeight = FontWeight.Medium)
                    Text("${TimeSaved.short(result.secondsToFirstMilestone)} to go", fontSize = 12.sp, color = Mist)
                }
            }
        }

        // A few sparks, bursting from the ring when the number lands.
        Canvas(Modifier.matchParentSize()) {
            val t = sparks.value
            if (t <= 0f || t >= 1f) return@Canvas
            val centre = Offset(18.dp.toPx() + 48.dp.toPx(), 18.dp.toPx() + 48.dp.toPx())
            val eased = 1f - (1f - t) * (1f - t)
            SPARKS.forEachIndexed { i, (angle, distance) ->
                val d = distance.dp.toPx() * eased
                val colour = when (i % 4) { 0 -> Glow; 1 -> Color.White; 2 -> Green; else -> Glow }
                drawCircle(
                    colour.copy(alpha = 1f - t),
                    radius = (4.dp.toPx() * (1f - 0.6f * t)),
                    center = Offset(centre.x + cos(angle) * d, centre.y + sin(angle) * d)
                )
            }
        }
    }
}

/** Angle in radians and how far, in dp, each spark flies. */
private val SPARKS = listOf(
    -2.3f to 70, -1.2f to 82, -0.4f to 96, 0.5f to 78, 1.3f to 88, 2.2f to 74, 3.0f to 100, -2.9f to 92
)

@Composable
private fun CompareBar(label: String, value: String, fraction: Float, fill: Brush) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = Mist, maxLines = 1, modifier = Modifier.width(70.dp))
        Box(
            Modifier
                .weight(1f)
                .height(9.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f))
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(9.dp).clip(CircleShape).background(fill))
        }
        Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(88.dp))
    }
}
