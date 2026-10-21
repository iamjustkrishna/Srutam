package space.iamjustkrishna.srutam.ui.screens

import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.StoragePermission
import kotlin.math.PI
import kotlin.math.sin

private data class IntroPage(val step: String, val tag: String, val title: String, val body: String)

private val introPages = listOf(
    IntroPage(
        step = "intro_1",
        tag = "JUST SPEAK",
        title = "Speak, and it's transcribed on your phone",
        body = "A lecture, a meeting or a passing thought. Srutam turns your voice into text right on the device."
    ),
    IntroPage(
        step = "intro_2",
        tag = "TRY IT",
        title = "Every note comes back organised",
        body = "Tap the mic to see what Srutam pulls out of a voice note."
    ),
    IntroPage(
        step = "intro_3",
        tag = "YOURS",
        title = "Your notes stay with you",
        body = "Each note is kept on your phone, inside its own recording. Sign in to Srutam Cloud any time to sync it, and to let your AI tools use it."
    )
)

/**
 * First-run intro: three illustrated pages. The first asks for access to recordings (so a
 * reinstall finds the notes saved inside them), the second is a tap-to-try demo of a note
 * becoming a summary, a task and a reminder, the third says where notes live. The mic and
 * notifications are asked later, when a feature needs them.
 */
@Composable
fun OnboardingIntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    var pageShownAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var storageGranted by remember { mutableStateOf(StoragePermission.isGranted(context)) }

    fun timeOnPage() = SystemClock.elapsedRealtime() - pageShownAt
    fun next() {
        if (pagerState.currentPage == introPages.lastIndex) onFinished()
        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "storage", granted, "onboarding")
        storageGranted = granted
        next()
    }

    LaunchedEffect(pagerState.currentPage) {
        pageShownAt = SystemClock.elapsedRealtime()
        Analytics.onboardingStepViewed(context, introPages[pagerState.currentPage].step)
    }

    val page = pagerState.currentPage
    val isLastPage = page == introPages.lastIndex
    val asksStorage = page == 0 && !storageGranted

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Sem.scaffold)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = {
                        Analytics.onboardingStepResult(context, introPages[page].step, Analytics.RESULT_SKIPPED, timeOnPage())
                        onFinished()
                    }) {
                        Text("Skip", color = Sem.textSecondary)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                IntroPageContent(
                    page = introPages[index],
                    isVisible = pagerState.currentPage == index,
                    storageGranted = storageGranted
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                introPages.indices.forEach { i ->
                    val selected = i == page
                    val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dotWidth")
                    val color by animateColorAsState(
                        if (selected) Sem.accent else Sem.accentBorder,
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = width, height = 8.dp)
                            .background(color, CircleShape)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (asksStorage) {
                            Analytics.onboardingStepResult(context, introPages[page].step, "allow_storage", timeOnPage())
                            storageLauncher.launch(StoragePermission.permission)
                        } else {
                            Analytics.onboardingStepResult(context, introPages[page].step, Analytics.RESULT_NEXT, timeOnPage())
                            next()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = when {
                            asksStorage -> "Allow and continue"
                            isLastPage -> "Get started"
                            else -> "Next"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                if (asksStorage) {
                    TextButton(onClick = {
                        Analytics.onboardingStepResult(context, introPages[page].step, "storage_not_now", timeOnPage())
                        next()
                    }) {
                        Text("Not now", fontSize = 14.sp, color = Sem.textSecondary)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage, isVisible: Boolean, storageGranted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page.step) {
            "intro_1" -> ListenIllustration(Modifier.size(width = 280.dp, height = 210.dp))
            "intro_2" -> InsightDemo(isVisible = isVisible)
            else -> PhoneCloudIllustration(Modifier.size(width = 280.dp, height = 200.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = page.tag,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Sem.accent
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = page.title,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Sem.text
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.body,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = Sem.textSecondary,
            modifier = Modifier.widthIn(max = 340.dp)
        )
        if (page.step == "intro_1") {
            Spacer(Modifier.height(18.dp))
            RecordingsAccessCard(granted = storageGranted)
        }
    }
}

/** Why the first page asks for access to recordings. */
@Composable
private fun RecordingsAccessCard(granted: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Sem.card,
        border = BorderStroke(1.dp, Sem.border),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = if (granted) Sem.emerald else Sem.accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (granted) "Your recordings are found" else "Find your recordings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Sem.text
                )
                Text(
                    text = "Used Srutam before? With access to Music and audio, notes saved in your recordings come back, even offline.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = Sem.textSecondary
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------
// Illustrations, drawn so they follow the theme in light and dark
// -------------------------------------------------------------------------------------------

/** A person speaking towards a phone: sound waves travel across and the phone's waveform moves. */
@Composable
private fun ListenIllustration(modifier: Modifier = Modifier) {
    val accent = Sem.accent
    val soft = Sem.accentContainer
    val card = Sem.card
    val border = Sem.border
    val figure = Sem.textSecondary
    val transition = rememberInfiniteTransition(label = "listen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier.semantics { contentDescription = "A person speaking to a phone that shows a moving waveform" }) {
        val w = size.width
        val h = size.height
        drawCircle(soft, radius = h * 0.48f, center = Offset(w * 0.5f, h * 0.52f))

        // Person: head and shoulders on the left.
        val headCenter = Offset(w * 0.24f, h * 0.38f)
        val headRadius = h * 0.12f
        drawCircle(figure.copy(alpha = 0.85f), radius = headRadius, center = headCenter)
        val shoulders = Path().apply {
            moveTo(w * 0.08f, h * 0.86f)
            cubicTo(w * 0.08f, h * 0.58f, w * 0.40f, h * 0.58f, w * 0.40f, h * 0.86f)
            close()
        }
        drawPath(shoulders, figure.copy(alpha = 0.85f))

        // Sound waves: three arcs that brighten one after another.
        for (i in 0 until 3) {
            val local = ((phase * 3f) - i).mod(3f) / 3f
            val alpha = (1f - local).coerceIn(0.15f, 1f)
            val r = h * (0.10f + i * 0.07f)
            drawArc(
                color = accent.copy(alpha = alpha),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(headCenter.x + headRadius * 0.6f - r, headCenter.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Phone on the right with a live waveform.
        val phoneW = w * 0.30f
        val phoneH = h * 0.74f
        val phoneTopLeft = Offset(w * 0.62f, h * 0.14f)
        drawRoundRect(card, phoneTopLeft, Size(phoneW, phoneH), CornerRadius(18.dp.toPx()))
        drawRoundRect(border, phoneTopLeft, Size(phoneW, phoneH), CornerRadius(18.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        val bars = 7
        val barGap = phoneW * 0.07f
        val barW = (phoneW - barGap * (bars + 1)) / bars
        val midY = phoneTopLeft.y + phoneH * 0.45f
        for (i in 0 until bars) {
            val wave = (sin((phase * 2 * PI) + i * 0.9).toFloat() + 1f) / 2f
            val barH = phoneH * (0.08f + 0.30f * wave)
            val x = phoneTopLeft.x + barGap + i * (barW + barGap)
            drawRoundRect(
                color = accent,
                topLeft = Offset(x, midY - barH / 2),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2)
            )
        }
        // Text lines appearing under the waveform: the transcript.
        val lineY = phoneTopLeft.y + phoneH * 0.74f
        val lineW = phoneW * 0.7f * (0.4f + 0.6f * phase)
        drawRoundRect(figure.copy(alpha = 0.35f), Offset(phoneTopLeft.x + phoneW * 0.15f, lineY), Size(lineW, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(figure.copy(alpha = 0.25f), Offset(phoneTopLeft.x + phoneW * 0.15f, lineY + 9.dp.toPx()), Size(phoneW * 0.45f, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
    }
}

/** A phone and a cloud joined by a dotted line, with a note travelling between them. */
@Composable
private fun PhoneCloudIllustration(modifier: Modifier = Modifier) {
    val accent = Sem.accent
    val soft = Sem.accentContainer
    val card = Sem.card
    val border = Sem.border
    val muted = Sem.textSecondary
    val emerald = Sem.emerald
    val transition = rememberInfiniteTransition(label = "sync")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "progress"
    )
    Canvas(modifier.semantics { contentDescription = "A phone keeping notes, with an optional sync to the cloud" }) {
        val w = size.width
        val h = size.height
        drawCircle(soft, radius = h * 0.46f, center = Offset(w * 0.5f, h * 0.5f))

        // Phone, bottom left, holding note lines and a lock.
        val phoneTopLeft = Offset(w * 0.12f, h * 0.20f)
        val phoneSize = Size(w * 0.26f, h * 0.70f)
        drawRoundRect(card, phoneTopLeft, phoneSize, CornerRadius(16.dp.toPx()))
        drawRoundRect(border, phoneTopLeft, phoneSize, CornerRadius(16.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        for (i in 0 until 3) {
            drawRoundRect(
                muted.copy(alpha = 0.35f),
                Offset(phoneTopLeft.x + phoneSize.width * 0.15f, phoneTopLeft.y + phoneSize.height * (0.22f + i * 0.12f)),
                Size(phoneSize.width * (0.7f - i * 0.12f), 4.dp.toPx()),
                CornerRadius(2.dp.toPx())
            )
        }
        val lockCenter = Offset(phoneTopLeft.x + phoneSize.width / 2, phoneTopLeft.y + phoneSize.height * 0.75f)
        drawRoundRect(emerald, Offset(lockCenter.x - 9.dp.toPx(), lockCenter.y - 4.dp.toPx()), Size(18.dp.toPx(), 14.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawArc(
            emerald, 180f, 180f, false,
            topLeft = Offset(lockCenter.x - 6.dp.toPx(), lockCenter.y - 12.dp.toPx()),
            size = Size(12.dp.toPx(), 14.dp.toPx()),
            style = Stroke(2.5.dp.toPx())
        )

        // Cloud, top right.
        val cloudCenter = Offset(w * 0.74f, h * 0.32f)
        val r = h * 0.10f
        // Drawn twice: a slightly larger outline pass first, so the cloud stands out in dark mode too.
        fun cloud(color: Color, grow: Float) {
            drawCircle(color, r * 1.25f + grow, cloudCenter)
            drawCircle(color, r + grow, Offset(cloudCenter.x - r * 1.2f, cloudCenter.y + r * 0.35f))
            drawCircle(color, r * 0.95f + grow, Offset(cloudCenter.x + r * 1.15f, cloudCenter.y + r * 0.4f))
            drawRoundRect(
                color,
                Offset(cloudCenter.x - r * 2.1f - grow, cloudCenter.y + r * 0.2f - grow),
                Size(r * 4.1f + grow * 2, r * 1.1f + grow * 2),
                CornerRadius(r * 0.55f + grow)
            )
        }
        cloud(accent.copy(alpha = 0.45f), 1.5.dp.toPx())
        cloud(card, 0f)

        // The optional sync: a dotted curve with a note dot moving along it.
        val path = Path().apply {
            moveTo(phoneTopLeft.x + phoneSize.width, phoneTopLeft.y + phoneSize.height * 0.3f)
            quadraticTo(w * 0.55f, h * 0.18f, cloudCenter.x - r * 1.4f, cloudCenter.y + r * 1.2f)
        }
        drawPath(
            path, accent.copy(alpha = 0.6f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)))
        )
        val measure = PathMeasure().apply { setPath(path, false) }
        val dot = measure.getPosition(measure.length * progress)
        drawCircle(accent, 6.dp.toPx(), dot)
    }
}

// -------------------------------------------------------------------------------------------
// Tap-to-try demo: a voice note becoming a transcript, a summary, a task and a reminder
// -------------------------------------------------------------------------------------------

private enum class DemoStage { IDLE, LISTENING, TRANSCRIBING, INSIGHTS }

private const val DEMO_TRANSCRIPT =
    "Quick one: send the release notes to Priya, and remind me to check the install numbers on Monday morning."

@Composable
private fun InsightDemo(isVisible: Boolean) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(DemoStage.IDLE) }
    var run by remember { mutableIntStateOf(0) }
    var typed by remember { mutableIntStateOf(0) }

    LaunchedEffect(run, isVisible) {
        if (run == 0 || !isVisible) return@LaunchedEffect
        Analytics.log(context, "onboarding_demo_played", mapOf(Analytics.PARAM_STEP to "intro_2"))
        stage = DemoStage.LISTENING
        typed = 0
        delay(1800)
        stage = DemoStage.TRANSCRIBING
        while (typed < DEMO_TRANSCRIPT.length) {
            typed = (typed + 3).coerceAtMost(DEMO_TRANSCRIPT.length)
            delay(28)
        }
        delay(250)
        stage = DemoStage.INSIGHTS
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
    ) {
        DemoMicButton(
            listening = stage == DemoStage.LISTENING,
            done = stage == DemoStage.INSIGHTS,
            onClick = { if (stage == DemoStage.IDLE || stage == DemoStage.INSIGHTS) run++ }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (stage) {
                DemoStage.IDLE -> "Tap to try"
                DemoStage.LISTENING -> "Listening…"
                DemoStage.TRANSCRIBING -> "Transcribing on your phone…"
                DemoStage.INSIGHTS -> "Tap to play again"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Sem.accent
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Sem.card,
            border = BorderStroke(1.dp, Sem.border),
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
        ) {
            Text(
                text = if (stage == DemoStage.IDLE || stage == DemoStage.LISTENING) {
                    "Your words appear here as you speak."
                } else {
                    "“" + DEMO_TRANSCRIPT.take(typed) + if (typed < DEMO_TRANSCRIPT.length) "▍" else "”"
                },
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = if (stage == DemoStage.IDLE || stage == DemoStage.LISTENING) Sem.textMuted else Sem.text,
                modifier = Modifier.padding(14.dp)
            )
        }

        DemoInsight(
            visible = stage == DemoStage.INSIGHTS,
            delayMs = 0,
            icon = null,
            label = "SUMMARY",
            text = "Two follow-ups before Monday: release notes and install numbers."
        )
        DemoInsight(
            visible = stage == DemoStage.INSIGHTS,
            delayMs = 140,
            icon = Icons.Default.RadioButtonUnchecked,
            label = "TASK",
            text = "Send the release notes to Priya"
        )
        DemoInsight(
            visible = stage == DemoStage.INSIGHTS,
            delayMs = 280,
            icon = Icons.Default.NotificationsActive,
            label = "REMINDER",
            text = "Check the install numbers · Mon, 9:00 AM"
        )
    }
}

@Composable
private fun DemoMicButton(listening: Boolean, done: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ring = Sem.accent
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        if (listening || !done) {
            Canvas(Modifier.size(104.dp)) {
                val scale = if (listening) pulse else 1f + (pulse - 1f) * 0.3f
                drawCircle(ring.copy(alpha = if (listening) 0.18f else 0.10f), radius = size.minDimension / 2 * 0.7f * scale)
            }
        }
        Surface(
            shape = CircleShape,
            color = CobaltBlue,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(68.dp)
                .scale(if (listening) 1.05f else 1f)
                .clickable(onClick = onClick)
                .semantics { contentDescription = if (done) "Play the demo again" else "Try the demo" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (done) Icons.Default.Replay else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun DemoInsight(visible: Boolean, delayMs: Int, icon: ImageVector?, label: String, text: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, delayMillis = delayMs)) +
            slideInVertically(tween(300, delayMillis = delayMs)) { it / 2 }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Sem.accentContainer,
            border = BorderStroke(1.dp, Sem.accentBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(18.dp))
                } else {
                    Text("✦", fontSize = 15.sp, color = Sem.accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Sem.accent)
                    Text(text, fontSize = 13.5.sp, lineHeight = 18.sp, color = Sem.text)
                }
            }
        }
    }
}
