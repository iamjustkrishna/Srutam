package space.iamjustkrishna.srutam.ui.screens

import android.graphics.Bitmap
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.ui.components.Cobalt
import space.iamjustkrishna.srutam.ui.components.Glow
import space.iamjustkrishna.srutam.ui.components.heroText
import space.iamjustkrishna.srutam.ui.components.LINING
import space.iamjustkrishna.srutam.ui.components.Mist
import space.iamjustkrishna.srutam.ui.components.PillButton
import space.iamjustkrishna.srutam.ui.components.PillStyle
import space.iamjustkrishna.srutam.ui.components.Royal
import space.iamjustkrishna.srutam.ui.components.Slate
import space.iamjustkrishna.srutam.ui.components.TimeSavedCardData
import space.iamjustkrishna.srutam.ui.components.TimeSavedCardRenderer
import space.iamjustkrishna.srutam.ui.components.Void
import space.iamjustkrishna.srutam.ui.components.Void2
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCard
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.StatsBar
import space.iamjustkrishna.srutam.utils.StatsConfig
import space.iamjustkrishna.srutam.utils.StatsRange
import space.iamjustkrishna.srutam.utils.StatsView
import space.iamjustkrishna.srutam.utils.StatsViews
import space.iamjustkrishna.srutam.utils.TimeSaved
import space.iamjustkrishna.srutam.utils.TimeSavedMilestones
import java.text.NumberFormat

/**
 * Your Srutam: how much time talking has saved. A hero with the number for the chosen range and a
 * bar for each day, two small facts, the next milestone, and one slim row to share the card. How it
 * is counted sits behind the small "i". Only lengths and word counts feed it, never what a note says.
 */
@Composable
fun StatsScreen(
    recordings: Collection<Recording>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF1E2229)
    var rangeName by rememberSaveable { mutableStateOf(StatsRange.MONTH.name) }
    val range = StatsRange.valueOf(rangeName)
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val allTimeSeconds = remember(recordings) { TimeSaved.totals(recordings, TimeSaved.monthStart()).allTimeSeconds }
    val view = remember(recordings, range) { StatsViews.build(recordings, range) }
    val latest = remember(recordings) { latestNote(recordings) }

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

        // Nothing opens this screen until there is something to count, so below that it just stays blank
        // (the notes are still loading when it first opens).
        if (allTimeSeconds >= StatsConfig.MIN_SECONDS_TO_SHOW) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Wide or short screens (a tablet, a phone on its side) get two columns.
                val twoColumns = maxWidth >= 600.dp
                val scroll = rememberScrollState()
                if (twoColumns) {
                    Row(
                        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) { Hero(view, range, { rangeName = it.name }, { showInfo = true }) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Facts(view)
                            Milestones(allTimeSeconds)
                            ShareRow(view, recordings)
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 8.dp).navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Hero(view, range, { rangeName = it.name }, { showInfo = true })
                        Facts(view)
                        Milestones(allTimeSeconds)
                        ShareRow(view, recordings)
                    }
                }
            }
        }
    }

    if (showInfo) HowItsCountedSheet(latest = latest, onDismiss = { showInfo = false })
}

// ---- hero --------------------------------------------------------------------------------

@Composable
private fun Hero(view: StatsView, range: StatsRange, onRange: (StatsRange) -> Unit, onInfo: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Royal, Void2, Void), start = Offset(0f, 0f), end = Offset(900f, 1300f)))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "${view.eyebrow.lowercase()}: ${TimeSaved.words(view.savedSeconds)} saved by talking instead of typing" }
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(view.eyebrow, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp, color = Glow)
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                    .clickable(onClick = onInfo)
                    .semantics { contentDescription = "How the time saved is counted" },
                contentAlignment = Alignment.Center
            ) {
                Text("i", fontSize = 12.sp, fontFamily = PlayfairDisplayFontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Mist)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = heroText(TimeSaved.hero(view.savedSeconds)), maxLines = 1, style = LINING)
        Text("saved by talking instead of typing", fontSize = 13.sp, color = Mist)

        view.deltaText?.let { text ->
            Spacer(Modifier.height(8.dp))
            val tint = if (view.deltaIsGain) Color(0xFF6EE7B7) else Mist
            Row(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (view.deltaIsGain) Color(0xFF10B981).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(if (view.deltaIsGain) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = tint)
            }
        }

        Spacer(Modifier.height(11.dp))
        RangeSwitch(range, onRange)
        Spacer(Modifier.height(11.dp))
        BarChart(view.bars, range)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            view.axis.forEach { Text(it, fontSize = 10.sp, color = Mist) }
        }
        view.bestText?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 11.5.sp, color = Mist)
        }
    }
}

@Composable
private fun RangeSwitch(range: StatsRange, onRange: (StatsRange) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StatsRange.entries.forEach { r ->
            val on = r == range
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (on) Color.White.copy(alpha = 0.17f) else Color.Transparent)
                    .clickable { onRange(r) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(r.label, fontSize = 11.5.sp, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal, color = if (on) Color.White else Mist, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BarChart(bars: List<StatsBar>, range: StatsRange) {
    val context = LocalContext.current
    val animated = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }.getOrDefault(true)
    }
    val grow = remember(range) { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(range) { if (animated) grow.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }
    val maxSeconds = (bars.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)

    Canvas(Modifier.fillMaxWidth().height(60.dp)) {
        if (bars.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(1f)
        val minHeight = 4.dp.toPx()
        val corner = CornerRadius(minOf(3.dp.toPx(), barWidth / 2))
        bars.forEachIndexed { i, bar ->
            val h = if (bar.seconds <= 0 || bar.isFuture) minHeight
            else (minHeight + (size.height - minHeight) * (bar.seconds.toFloat() / maxSeconds)) * grow.value.coerceAtLeast(0.02f)
            val x = i * (barWidth + gap)
            val topLeft = Offset(x, size.height - h)
            val barSize = Size(barWidth, h)
            when {
                bar.seconds <= 0 || bar.isFuture -> drawRoundRect(Color.White.copy(alpha = 0.07f), topLeft, barSize, corner)
                bar.isBest -> {
                    drawRoundRect(Glow.copy(alpha = 0.35f), Offset(topLeft.x - 3.dp.toPx(), topLeft.y - 3.dp.toPx()), Size(barSize.width + 6.dp.toPx(), barSize.height + 6.dp.toPx()), corner)
                    drawRoundRect(Brush.verticalGradient(listOf(Glow, Cobalt), startY = topLeft.y, endY = size.height), topLeft, barSize, corner)
                }
                else -> drawRoundRect(Color(0xFF2A3A73), topLeft, barSize, corner)
            }
        }
    }
}

// ---- facts, milestones, share ------------------------------------------------------------

@Composable
private fun cardColors(): Pair<Color, Color> {
    val dark = LocalIsCosmicDark.current
    return (if (dark) CosmicVoidCard else Color.White) to (if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
}

@Composable
private fun Facts(view: StatsView) {
    val (fill, line) = cardColors()
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
    val sub = if (dark) TextOnDarkSecondary else Color(0xFF64748B)
    val number = NumberFormat.getIntegerInstance()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Fact(
            "SPEAKING PACE",
            if (view.paceWpm > 0) buildAnnotatedString {
                append(view.paceWpm.toString())
                withStyle(SpanStyle(fontSize = 12.sp, fontFamily = null, fontWeight = FontWeight.Medium, color = sub)) { append(" wpm") }
            } else buildAnnotatedString { append("-") },
            if (view.paceMultiple.isNotEmpty()) "${view.paceMultiple} times faster than typing" else "Talk a little more to see this",
            fill, line, ink, sub, Modifier.weight(1f)
        )
        Fact(
            "NOTES",
            buildAnnotatedString { append(number.format(view.period.notes)) },
            "${number.format(view.period.words)} words",
            fill, line, ink, sub, Modifier.weight(1f)
        )
    }
}

@Composable
private fun Fact(label: String, value: androidx.compose.ui.text.AnnotatedString, caption: String, fill: Color, line: Color, ink: Color, sub: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, line, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 12.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, color = sub)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 26.sp, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold, color = ink, style = LINING, maxLines = 1)
        Text(caption, fontSize = 12.sp, lineHeight = 15.sp, color = sub)
    }
}

@Composable
private fun Milestones(allTimeSeconds: Long) {
    val (fill, line) = cardColors()
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
    val sub = if (dark) TextOnDarkSecondary else Color(0xFF64748B)
    val accent = if (dark) Glow else CobaltBlue
    val next = StatsConfig.nextAfter(allTimeSeconds)
    val previous = StatsConfig.reachedBy(allTimeSeconds).lastOrNull() ?: 0L
    val badges = StatsConfig.badges(allTimeSeconds)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                "Next milestone: ${TimeSavedMilestones.label(next)}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.weight(1f, fill = false)
            )
            Text("${TimeSaved.hero(next - allTimeSeconds)} to go", fontSize = 12.5.sp, color = sub, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(if (dark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8EDF5))) {
            // How far between the last milestone and the next, so it fills again after each one.
            val fraction = ((allTimeSeconds - previous).toFloat() / (next - previous)).coerceIn(0.02f, 1f)
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Cobalt, Glow))))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (badges.earlier > 0) Badge("✓ ${badges.earlier}", done = true, isNext = false, dark, accent, ink, sub)
            badges.steps.forEach { step ->
                Badge(TimeSavedMilestones.badge(step), done = step <= allTimeSeconds, isNext = step == next, dark, accent, ink, sub)
            }
        }
    }
}

@Composable
private fun RowScope.Badge(label: String, done: Boolean, isNext: Boolean, dark: Boolean, accent: Color, ink: Color, sub: Color) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(99.dp))
            .background(
                when {
                    done -> if (dark) Cobalt.copy(alpha = 0.22f) else Color(0xFFEEF4FF)
                    else -> if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFE8EDF5)
                }
            )
            .then(if (done) Modifier.border(1.dp, if (dark) Glow.copy(alpha = 0.4f) else Color(0xFFC3D6FB), RoundedCornerShape(99.dp)) else Modifier)
            .then(
                if (isNext) Modifier.drawBehind {
                    drawRoundRect(accent, cornerRadius = CornerRadius(size.height / 2), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                } else Modifier
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            color = when { done -> accent; isNext -> ink; else -> sub.copy(alpha = 0.7f) }
        )
    }
}

@Composable
private fun ShareRow(view: StatsView, recordings: Collection<Recording>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (fill, line) = cardColors()
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
    val sub = if (dark) TextOnDarkSecondary else Color(0xFF64748B)
    val card = remember(view) { TimeSavedCardData(view.cardLabel, view.period) }
    val title = when (view.range) {
        StatsRange.WEEK -> "Your week's card"
        StatsRange.MONTH -> "Your ${view.cardLabel} card"
        StatsRange.ALL -> "Your all-time card"
    }

    // The thumbnail is the real card, drawn small, so what is shared is no surprise.
    val thumb by produceState<Bitmap?>(null, card) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val full = TimeSavedCardRenderer.render(context, card)
                Bitmap.createScaledBitmap(full, 108, 135, true).also { if (it !== full) full.recycle() }
            }.getOrNull()
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, line, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(Modifier.size(width = 38.dp, height = 48.dp).clip(RoundedCornerShape(7.dp)).background(Void)) {
            thumb?.let { Image(it.asImageBitmap(), contentDescription = "Preview of the card", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = ink, maxLines = 1)
            Text("A picture with your number", fontSize = 11.5.sp, color = sub, maxLines = 1)
        }
        PillButton(
            "Share",
            onClick = {
                Analytics.timeSavedShared(context, "stats")
                scope.launch {
                    runCatching { TimeSavedCardRenderer.share(context, card, campaign = "time_saved") }
                        .onFailure { Toast.makeText(context, "Couldn't share the card", Toast.LENGTH_SHORT).show() }
                }
            },
            icon = Icons.Default.Share,
            compact = true
        )
    }
}

// ---- the "i" sheet ----------------------------------------------------------------------

/** The most recent note with words in it: the sheet works its example from this. */
internal fun latestNote(recordings: Collection<Recording>): TimeSaved.Result? =
    recordings.filter { !it.transcript.isNullOrBlank() }
        .maxByOrNull { it.timestamp }
        ?.let { TimeSaved.of(it.transcript, it.duration) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HowItsCountedSheet(latest: TimeSaved.Result?, onDismiss: () -> Unit) {
    val dark = LocalIsCosmicDark.current
    val ink = if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
    val sub = if (dark) TextOnDarkSecondary else Color(0xFF64748B)
    val (fill, line) = cardColors()
    // Before any note there is nothing of the person's own to show, so a small sample stands in.
    val example = latest ?: TimeSaved.of(35, 12_000L)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp).navigationBarsPadding().widthIn(max = 560.dp)) {
            Text("How it's counted", fontSize = 21.sp, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold, color = ink)
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    append("The average person types about ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = ink)) { append("${StatsConfig.TYPING_WPM} words a minute") }
                    append(". Srutam compares that with how long you actually spent talking.")
                },
                fontSize = 14.sp, lineHeight = 21.sp, color = sub
            )
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(fill).border(1.dp, line, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    (if (latest != null) "YOUR LATEST NOTE" else "AN EXAMPLE") + " · ${example.words} WORDS",
                    fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = sub
                )
                Spacer(Modifier.height(10.dp))
                val spokenShare = if (example.typingSeconds > 0) (example.spokenSeconds.toFloat() / example.typingSeconds).coerceIn(0.04f, 1f) else 1f
                CompareLine("Typing it", "~${TimeSaved.short(example.typingSeconds)}", 1f, Slate, ink, sub)
                Spacer(Modifier.height(9.dp))
                CompareLine("Saying it", TimeSaved.short(example.spokenSeconds), spokenShare, Cobalt, ink, sub)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(line))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Time saved", fontSize = 14.sp, color = sub)
                    Text(
                        TimeSaved.short(example.savedSeconds), fontSize = 24.sp, fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.ExtraBold,
                        color = if (dark) Glow else CobaltBlue, style = LINING, maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Srutam adds this up for every note. It only uses each note's length and word count, never what it says.",
                fontSize = 12.sp, lineHeight = 17.sp, color = sub, modifier = Modifier.padding(horizontal = 2.dp)
            )
            Spacer(Modifier.height(14.dp))
            PillButton("Got it", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompareLine(label: String, value: String, share: Float, bar: Color, ink: Color, sub: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, fontSize = 13.sp, color = sub, modifier = Modifier.width(74.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.Transparent)) {
            Box(Modifier.fillMaxWidth(share).height(8.dp).clip(CircleShape).background(if (bar == Cobalt) Brush.horizontalGradient(listOf(Cobalt, Glow)) else Brush.horizontalGradient(listOf(bar, bar))))
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1, modifier = Modifier.widthIn(min = 52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
