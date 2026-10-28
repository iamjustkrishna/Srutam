package space.iamjustkrishna.srutam.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CosmicGlowBlue
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.utils.TimeSaved

private val LINE_STYLE = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "lnum")

/**
 * The line under "Srutam" in the Notes header: `⚡ 6h 23m saved this month ›`, on a soft tinted
 * chip. Tapping it opens Your Srutam.
 *
 * It never wraps and never pushes the title or the icons: it measures a list of wordings, longest
 * first, against the room it's given and shows the first that fits ("saved this month", then
 * "saved", then the time alone, then hours only). Hidden until a minute has been saved, so a new
 * person never meets a bare "0m".
 */
@Composable
fun TimeSavedLine(totals: TimeSaved.Totals, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (!totals.worthShowingInHeader) return
    val dark = LocalIsCosmicDark.current
    val accent = if (dark) CosmicGlowBlue else CobaltBlue
    val chip = if (dark) CobaltBlue.copy(alpha = 0.2f) else Color(0xFFEAF1FF)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val candidates = remember(totals) { TimeSaved.headerCandidates(totals) }

    // Icon (13) + gaps and padding around it (4 + 7 + 9), the same dp the chip below uses.
    val chromePx = with(density) { (13 + 4 + 7 + 9).dp.roundToPx() }

    BoxWithConstraints(modifier) {
        val room = constraints.maxWidth
        val text = candidates.firstOrNull { measurer.measure(it, LINE_STYLE, maxLines = 1).size.width + chromePx <= room }
            ?: candidates.last()
        val spoken = "${TimeSaved.words(totals.headerSeconds)} saved${if (totals.headerIsThisMonth) " this month" else ""}. Opens Your Srutam."
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(chip)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = spoken }
                .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp)
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Text(
                text = buildAnnotatedString {
                    // "6h 23m saved" in the accent, "this month ›" softer, like the mockup.
                    val i = text.indexOf("this month")
                    if (i >= 0) {
                        withStyle(SpanStyle(color = accent)) { append(text.substring(0, i)) }
                        withStyle(SpanStyle(color = if (dark) Color(0xFF94A3B8) else Color(0xFF5B6B8A), fontWeight = FontWeight.Normal)) { append(text.substring(i)) }
                    } else {
                        withStyle(SpanStyle(color = accent)) { append(text) }
                    }
                },
                style = LINE_STYLE,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

/**
 * The one-time bubble under [TimeSavedLine], the first time the line shows: "Time you've saved by
 * talking instead of typing." It leaves after a few seconds or at a tap, and never returns.
 * It sits at the top of the content area, directly under the header.
 */
@Composable
fun TimeSavedTip(enabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled || TimeSavedTipState.wasShown(context)) return@LaunchedEffect
        delay(1_500L)
        TimeSavedTipState.markShown(context)
        visible = true
        delay(7_000L)
        visible = false
    }
    LaunchedEffect(enabled) { if (!enabled) visible = false }

    val dark = LocalIsCosmicDark.current
    val fill = if (dark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val strong = if (dark) Color(0xFF0F172A) else Color.White
    val soft = if (dark) Color(0xFF475569) else Color(0xFF93A3C7)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { -it / 4 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 5 },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { visible = false }
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            // A small pointer up to the line.
            Canvas(Modifier.padding(start = 26.dp).size(width = 12.dp, height = 6.dp)) {
                drawPath(
                    Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    fill
                )
            }
            Surface(shape = RoundedCornerShape(14.dp), color = fill, shadowElevation = 6.dp, modifier = Modifier.widthIn(max = 260.dp)) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = strong, fontWeight = FontWeight.Bold)) { append("Time you've saved by talking") }
                            withStyle(SpanStyle(color = strong)) { append(" instead of typing.") }
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.width(1.dp))
                    Text("Tap this line any time to see how it's counted.", fontSize = 12.sp, lineHeight = 16.sp, color = soft, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Whether the "time saved" bubble has been shown on this install. */
internal object TimeSavedTipState {
    private const val PREFS = "srutam_hints"
    private const val KEY = "time_saved_tip_shown"

    fun wasShown(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markShown(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}
