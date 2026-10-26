package space.iamjustkrishna.srutam.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.shadow
import space.iamjustkrishna.srutam.ui.components.RecordShutter
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.navigation.FirstNote
import space.iamjustkrishna.srutam.service.AiProcessingWorker
import space.iamjustkrishna.srutam.service.LiveLevel
import space.iamjustkrishna.srutam.service.MicPermission
import space.iamjustkrishna.srutam.service.QuickRecordNotification
import space.iamjustkrishna.srutam.service.RecordingCoordinator
import space.iamjustkrishna.srutam.service.RecordingForegroundService
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.AppPreferences

/** Where the first-run flow is. */
private enum class FirstRunStage { WELCOME, RECORDING, RESULT }

private val RecordRed = Color(0xFFE53935)

/**
 * First run, start to first note, without leaving onboarding:
 *  1. Welcome: what Srutam does, and one action, the app's own mic button.
 *  2. Recording: a focused screen with a live waveform and a timer. The mic is asked for only
 *     when the button is tapped; a denial keeps the user on the welcome screen.
 *  3. Srutam at work: the note being written and understood, step by step, then the user's own
 *     summary, tasks and reminders. Continue goes to Home.
 * Nothing else is asked here. Recordings from an earlier install, the floating button and the
 * welcome note all come later, on Home, when they mean something.
 */
@Composable
fun WelcomeScreen(
    /** The microphone, plus storage on Android 10, where saving into Music/Srutam needs it. */
    micPermissions: Array<String>,
    /** [result] is how first run ended: note, skip. */
    onFinished: (result: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(FirstRunStage.WELCOME) }
    var recordingPath by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(stage) { Analytics.onboardingStepViewed(context, "first_run_${stage.name.lowercase()}") }

    Box(
        modifier
            .fillMaxSize()
            .background(Sem.scaffold)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn(tween(320, delayMillis = 80)) + slideInVertically(tween(320, delayMillis = 80)) { it / 12 })
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "firstRun"
        ) { current ->
            when (current) {
                FirstRunStage.WELCOME -> WelcomeStage(
                    micPermissions = micPermissions,
                    onStartRecording = {
                        // So the first note gets its summary the moment recording stops.
                                        QuickRecordNotification.restoreIdle(context)
                        if (RecordingCoordinator.requestStart(context, "onboarding")) stage = FirstRunStage.RECORDING
                    },
                    onSkip = { onFinished("skip") }
                )
                FirstRunStage.RECORDING -> RecordingStage(
                    onStopped = { path ->
                        recordingPath = path
                        stage = FirstRunStage.RESULT
                    },
                    onStartOver = {
                        RecordingCoordinator.requestCancel(context)
                        stage = FirstRunStage.WELCOME
                    }
                )
                FirstRunStage.RESULT -> ResultStage(
                    recordingPath = recordingPath,
                    onRetry = { stage = FirstRunStage.WELCOME },
                    onContinue = { onFinished("note") }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------
// 1. Welcome
// -------------------------------------------------------------------------------------------

@Composable
private fun WelcomeStage(
    micPermissions: Array<String>,
    onStartRecording: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // null: not asked yet; "denied": Android can still ask; "blocked": only Settings can turn it on.
    var micState by rememberSaveable { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results[Manifest.permission.RECORD_AUDIO] == true
        Analytics.permissionResult(context, "mic", granted, "welcome")
        if (granted) {
            onStartRecording()
        } else {
            val activity = context as? Activity
            val blocked = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            micState = if (blocked) "blocked" else "denied"
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Everything fits on one screen: the illustration and spacing give way on shorter phones
        // (a 1080x2400 phone has about 750 dp once the system bars are taken out).
        val roomy = maxHeight >= 820.dp
        val compact = maxHeight < 720.dp
        val illustrationHeight = (maxHeight * 0.19f).coerceIn(84.dp, 170.dp)
        val micSize = if (roomy) 84.dp else 74.dp

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) { Text("Skip", color = Sem.textSecondary) }
            }

            // Centred in the space left, and never clipped: on a very small screen it scrolls.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Entrance(0) {
                        NoteHeroIllustration(illustrationHeight)
                    }
                    Spacer(Modifier.height(if (roomy) 26.dp else 16.dp))
                    Entrance(1) {
                        Text(
                            text = "Capture a thought.\nGet a useful note.",
                            fontSize = if (compact) 25.sp else 28.sp,
                            lineHeight = if (compact) 30.sp else 34.sp,
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Sem.text
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Entrance(2) {
                        Text(
                            text = "Just talk. Srutam writes it down on your phone, then pulls out what matters.",
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Sem.textSecondary,
                            modifier = Modifier.widthIn(max = 330.dp)
                        )
                    }
                    Spacer(Modifier.height(if (roomy) 22.dp else 16.dp))
                    Entrance(3) {
                        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
                            ValueLine(Icons.Outlined.PhoneAndroid, "Transcribed on your phone, even offline")
                            ValueLine(Icons.Outlined.AutoAwesome, "A summary, tasks and decisions")
                            ValueLine(Icons.Outlined.NotificationsActive, "Reminders from what you said")
                        }
                    }
                }
            }

            Entrance(4) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(bottom = if (compact) 10.dp else 18.dp, top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MicButton(
                        recording = false,
                        size = micSize,
                        description = "Record your first note",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when {
                                MicPermission.isGranted(context) -> onStartRecording()
                                micState == "blocked" -> openAppSettings(context)
                                else -> micLauncher.launch(micPermissions)
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (micState == "blocked") "Allow the microphone to start" else "Tap to record your first note",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Sem.text
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = micState,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "micNote"
                    ) { state ->
                        Text(
                            text = when (state) {
                                null -> "Say anything on your mind. Your audio stays on this phone."
                                "blocked" -> "The microphone is off for Srutam. Tap to open Settings, or skip for now."
                                else -> "Srutam needs the microphone to hear you. Tap again whenever you're ready."
                            },
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            color = if (state == null) Sem.textMuted else Sem.amberText,
                            modifier = Modifier.widthIn(max = 320.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Fades and lifts content in, one beat after the previous item, so the screen settles in. */
@Composable
private fun Entrance(index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(90L * index)
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 18.dp.toPx()
        }
    ) { content() }
}

/**
 * The app's own record button ([RecordShutter], as in the bottom bar), larger, with a soft red
 * pulse around it and a little give when pressed.
 */
@Composable
private fun MicButton(recording: Boolean, size: Dp, description: String, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "micPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (recording) 1100 else 2000, easing = LinearEasing)),
        label = "pulse"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f), label = "press")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.5f)) {
        Canvas(Modifier.size(size * 1.5f)) {
            val base = size.toPx() / 2
            for (ring in 0 until 2) {
                val t = (pulse + ring * 0.5f) % 1f
                drawCircle(
                    color = RecordRed.copy(alpha = (1f - t) * if (recording) 0.26f else 0.16f),
                    radius = base + t * base * 0.5f
                )
            }
        }
        RecordShutter(
            recording = recording,
            size = size,
            modifier = Modifier
                .scale(press)
                .shadow(10.dp, CircleShape, ambientColor = RecordRed, spotColor = RecordRed)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }
        )
    }
}

// -------------------------------------------------------------------------------------------
// 2. Recording
// -------------------------------------------------------------------------------------------

private val prompts = listOf(
    "“Tomorrow I need to…”",
    "an idea you don't want to lose",
    "what was decided in a meeting",
    "“Remind me to… on Friday”"
)

@Composable
private fun RecordingStage(onStopped: (path: String?) -> Unit, onStartOver: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var path by remember { mutableStateOf<String?>(null) }
    var promptIndex by remember { mutableIntStateOf(0) }
    val levels = remember { mutableStateListOf<Float>().apply { repeat(BAR_COUNT) { add(0f) } } }
    val level by LiveLevel.level.collectAsState()

    BackHandler { onStartOver() }

    LaunchedEffect(Unit) {
        while (true) {
            elapsedMs = RecordingForegroundService.elapsedDurationMs
            RecordingCoordinator.activeFilePath?.let { path = it }
            levels.removeAt(0)
            levels.add(level)
            delay(80)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            promptIndex = (promptIndex + 1) % prompts.size
        }
    }

    val canStop = elapsedMs >= 1_000L

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dot = rememberInfiniteTransition(label = "dot").animateFloat(
                0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dotAlpha"
            )
            Box(Modifier.size(9.dp).alpha(dot.value).background(RecordRed, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Listening", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Sem.textSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "%d:%02d".format(elapsedMs / 60_000, (elapsedMs / 1000) % 60),
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            color = Sem.text
        )
        Spacer(Modifier.height(28.dp))
        LiveWaveform(levels, Modifier.fillMaxWidth().height(96.dp))
        Spacer(Modifier.height(32.dp))
        Text("Talk about anything, like", fontSize = 13.sp, color = Sem.textMuted)
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = promptIndex,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 })
                    .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 })
            },
            label = "prompt"
        ) { index ->
            Text(
                prompts[index],
                fontSize = 18.sp,
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Sem.text,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))

        MicButton(
            recording = true,
            size = 84.dp,
            description = "Finish recording",
            onClick = {
                if (!canStop) return@MicButton
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val finished = RecordingCoordinator.activeFilePath ?: path
                RecordingCoordinator.requestStop(context)
                onStopped(finished)
            }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (canStop) "Tap to finish" else "Keep talking…",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Sem.text
        )
        TextButton(onClick = onStartOver) {
            Text("Start over", fontSize = 13.sp, color = Sem.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private const val BAR_COUNT = 36

/** Bars for the last few seconds of sound, newest on the right, mirrored around the middle. */
@Composable
private fun LiveWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val accent = Sem.accent
    val quiet = Sem.border
    Canvas(modifier.semantics { contentDescription = "Live sound level" }) {
        val gap = size.width / levels.size
        val barWidth = gap * 0.55f
        levels.forEachIndexed { i, raw ->
            val level = raw.coerceIn(0f, 1f)
            val h = (size.height * (0.06f + 0.94f * level)).coerceAtLeast(4.dp.toPx())
            val x = i * gap + (gap - barWidth) / 2
            // Older bars fade a little, so the eye follows the newest sound.
            val age = i.toFloat() / levels.size
            drawRoundRect(
                color = if (level < 0.04f) quiet else accent.copy(alpha = 0.35f + 0.65f * age),
                topLeft = Offset(x, (size.height - h) / 2),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------
// 3. Srutam at work, then the user's own note
// -------------------------------------------------------------------------------------------

@Composable
private fun ResultStage(recordingPath: String?, onRetry: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    var noteId by remember { mutableStateOf<Long?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var waitedLong by remember { mutableStateOf(false) }

    LaunchedEffect(recordingPath) {
        val id = FirstNote.awaitSavedId(context, recordingPath, timeoutMs = 15_000L)
        if (id == null) notFound = true else noteId = id
    }
    LaunchedEffect(Unit) {
        delay(45_000L)
        waitedLong = true
    }

    val recording by remember(noteId) {
        noteId?.let { db.recordingDao().getRecordingByIdFlow(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    val insights by remember(noteId) {
        noteId?.let { db.insightDao().getInsightsByRecordingIdFlow(it) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val reminders by remember(noteId) {
        noteId?.let { db.reminderDao().getRemindersByRecordingIdFlow(it) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val note = recording
    val transcript = note?.transcript?.takeIf { it.isNotBlank() }
    val ready = note?.aiStatus == RecordingAiStatus.READY && !note.summary.isNullOrBlank()
    val waitingForInternet = note?.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE
    val failed = note?.aiStatus == RecordingAiStatus.ERROR

    LaunchedEffect(ready) { if (ready) Analytics.log(context, "first_run_note_ready") }

    // Make sure the note is being worked on: if it was saved but AI wasn't queued (Auto AI off,
    // or the recording service didn't get to it), queue it here. Waits briefly first, so the
    // recording service's own queueing wins when it happens.
    LaunchedEffect(noteId) {
        val id = noteId ?: return@LaunchedEffect
        delay(1_500L)
        val dao = db.recordingDao()
        val current = dao.getRecordingById(id) ?: return@LaunchedEffect
        val untouched = !current.isProcessing && current.summary.isNullOrBlank() &&
            current.aiStatus != RecordingAiStatus.SUMMARY_PENDING_OFFLINE &&
            current.aiStatus != RecordingAiStatus.READY
        if (untouched) {
            dao.updateRecording(
                current.copy(
                    isProcessing = true,
                    aiStatus = if (current.transcript.isNullOrBlank()) RecordingAiStatus.TRANSCRIBING else RecordingAiStatus.SUMMARY_PROCESSING,
                    processingError = null
                )
            )
            AiProcessingWorker.enqueueProcessing(context, listOf(id))
        }
    }

    if (notFound) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("That recording didn't save", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Sem.text)
            Spacer(Modifier.height(8.dp))
            Text(
                "It may have been too short. Try again and say a sentence or two.",
                fontSize = 15.sp, color = Sem.textSecondary, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue), shape = RoundedCornerShape(16.dp)) {
                Text("Try again", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onContinue) { Text("Continue to Srutam", color = Sem.textSecondary) }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 12.dp)
        ) {
            AnimatedContent(
                targetState = ready,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                label = "heading"
            ) { done ->
                Text(
                    text = if (done) "Your first note" else "Srutam is on it",
                    fontSize = 28.sp,
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Sem.text
                )
            }
            Spacer(Modifier.height(18.dp))

            // The work, step by step.
            Surface(shape = RoundedCornerShape(18.dp), color = Sem.card, border = BorderStroke(1.dp, Sem.border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WorkStep("Saved on your phone", done = noteId != null)
                    WorkStep("Writing down what you said", done = transcript != null, active = noteId != null)
                    WorkStep(
                        label = when {
                            waitingForInternet -> "Summary waits for the internet. It'll finish on its own."
                            failed -> "Couldn't finish the summary. Srutam will try again."
                            else -> "Finding the summary, tasks and reminders"
                        },
                        done = ready,
                        active = transcript != null && !waitingForInternet && !failed,
                        warning = waitingForInternet || failed
                    )
                }
            }

            AnimatedVisibility(
                visible = transcript != null,
                enter = fadeIn(tween(350)) + expandVertically(tween(350))
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    SectionTitle("WHAT YOU SAID")
                    Text(
                        text = "“${transcript.orEmpty()}”",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Sem.text,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!ready && transcript != null && !waitingForInternet && !failed) {
                Spacer(Modifier.height(18.dp))
                ShimmerLines()
            }

            AnimatedVisibility(
                visible = ready,
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100)) { it / 6 }
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Sem.accentContainer,
                        border = BorderStroke(1.dp, Sem.accentBorder)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(note?.name.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Sem.text)
                            Spacer(Modifier.height(6.dp))
                            Text(note?.summary.orEmpty(), fontSize = 14.5.sp, lineHeight = 21.sp, color = Sem.text)
                            val tasks = insights.filter { it.kind == InsightKind.ACTION }.map { it.text }.ifEmpty { legacyTasks(note?.actionItems) }
                            if (tasks.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                SectionTitle("TASKS")
                                tasks.take(3).forEach { task ->
                                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = Sem.accent, modifier = Modifier.size(17.dp).padding(top = 2.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(task, fontSize = 14.sp, color = Sem.text)
                                    }
                                }
                            }
                            val upcoming = reminders.filter { it.status == ReminderStatus.ACTIVE }
                            if (upcoming.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                SectionTitle("REMINDERS")
                                upcoming.take(2).forEach { reminder ->
                                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.NotificationsActive, null, tint = Sem.accent, modifier = Modifier.size(17.dp).padding(top = 2.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(reminder.title, fontSize = 14.sp, color = Sem.text)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Every note works like this. Find it, and the welcome note, on your Home screen.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Sem.textSecondary
                    )
                }
            }
        }

        val primary = ready || waitingForInternet || failed || waitedLong
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 18.dp, top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (primary) {
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continue to Srutam", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            } else {
                TextButton(onClick = onContinue, modifier = Modifier.height(56.dp)) {
                    Text("Continue, it'll finish in the background", fontSize = 14.sp, color = Sem.textSecondary)
                }
            }
        }
    }
}

private fun legacyTasks(json: String?): List<String> = runCatching {
    Gson().fromJson(json ?: "[]", Array<String>::class.java).orEmpty()
        .map { it.removePrefix("[ ]").removePrefix("[x]").trim() }
        .filter { it.isNotBlank() }
}.getOrDefault(emptyList())

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Sem.accent)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun WorkStep(label: String, done: Boolean, active: Boolean = true, warning: Boolean = false) {
    val textAlpha by animateFloatAsState(if (done || active) 1f else 0.45f, tween(250), label = "stepAlpha")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(textAlpha)) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = when {
                    done -> 2
                    warning -> 3
                    active -> 1
                    else -> 0
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "stepIcon"
            ) { state ->
                when (state) {
                    2 -> Box(Modifier.size(22.dp).background(Sem.emerald, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp).scale(1f))
                    }
                    1 -> Spinner(Modifier.size(20.dp))
                    3 -> Box(Modifier.size(10.dp).background(Sem.amber, CircleShape))
                    else -> Box(Modifier.size(10.dp).background(Sem.border, CircleShape))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.5.sp, color = if (warning) Sem.amberText else Sem.text, fontWeight = FontWeight.Medium)
    }
}

/** A ring with an arc that turns: the step being worked on. */
@Composable
private fun Spinner(modifier: Modifier = Modifier) {
    val track = Sem.accentBorder
    val arc = Sem.accent
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle"
    )
    Canvas(modifier) {
        val stroke = 2.5.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
        drawArc(arc, angle, 100f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Placeholder lines with a moving sheen while the summary is being written. */
@Composable
private fun ShimmerLines() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    val base = Sem.border.copy(alpha = 0.7f)
    val shine = Sem.card
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(1f, 0.92f, 0.7f, 0.5f).forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(base, shine, base),
                            start = Offset(x * 600f, 0f),
                            end = Offset(x * 600f + 300f, 0f)
                        ),
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun ValueLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .background(Sem.accentContainer, RoundedCornerShape(9.dp))
                .border(1.dp, Sem.accentBorder, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Sem.text, fontWeight = FontWeight.Medium)
    }
}

/**
 * What Srutam does, in one picture: a phone hearing you (a live waveform), writing it down (lines
 * filling in), and what comes out of it floating around: a summary, a task and a reminder.
 * Everything gently moves; the chips drift in one after another.
 */
@Composable
private fun NoteHeroIllustration(height: Dp) {
    val transition = rememberInfiniteTransition(label = "hero")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "phase"
    )
    val phoneHeight = height * 0.92f
    val phoneWidth = phoneHeight * 0.58f
    val showChips = height >= 110.dp
    val accent = Sem.accent
    val soft = Sem.accentContainer
    val muted = Sem.textSecondary

    Box(
        modifier = Modifier
            .size(width = height * 1.85f, height = height)
            .semantics { contentDescription = "A phone turning what you say into a note with a summary, a task and a reminder" },
        contentAlignment = Alignment.Center
    ) {
        // Soft glow behind the phone.
        Canvas(Modifier.size(height)) {
            drawCircle(
                Brush.radialGradient(listOf(soft, soft.copy(alpha = 0f))),
                radius = size.minDimension / 2
            )
        }

        // The phone.
        Surface(
            shape = RoundedCornerShape(phoneWidth * 0.2f),
            color = Sem.card,
            border = BorderStroke(1.dp, Sem.border),
            shadowElevation = 10.dp,
            modifier = Modifier.size(width = phoneWidth, height = phoneHeight)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Speaker slot.
                drawRoundRect(
                    muted.copy(alpha = 0.25f),
                    topLeft = Offset(w * 0.38f, h * 0.06f),
                    size = Size(w * 0.24f, h * 0.025f),
                    cornerRadius = CornerRadius(h * 0.0125f)
                )
                // Live waveform.
                val bars = 9
                val gap = w * 0.06f
                val barW = (w * 0.8f - gap * (bars - 1)) / bars
                val midY = h * 0.3f
                for (i in 0 until bars) {
                    val wave = (sin(phase * 2 * Math.PI * 2 + i * 0.8).toFloat() + 1f) / 2f
                    val barH = h * (0.04f + 0.16f * wave)
                    drawRoundRect(
                        accent,
                        topLeft = Offset(w * 0.1f + i * (barW + gap), midY - barH / 2),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2)
                    )
                }
                // The note being written: lines fill in, one after another.
                val lineH = h * 0.03f
                listOf(0.8f, 0.68f, 0.74f, 0.5f).forEachIndexed { i, full ->
                    val start = i * 0.18f
                    val progress = ((phase - start) / 0.3f).coerceIn(0f, 1f)
                    val y = h * (0.52f + i * 0.085f)
                    drawRoundRect(
                        muted.copy(alpha = 0.14f),
                        topLeft = Offset(w * 0.1f, y),
                        size = Size(w * full, lineH),
                        cornerRadius = CornerRadius(lineH / 2)
                    )
                    drawRoundRect(
                        muted.copy(alpha = 0.42f),
                        topLeft = Offset(w * 0.1f, y),
                        size = Size(w * full * progress, lineH),
                        cornerRadius = CornerRadius(lineH / 2)
                    )
                }
            }
        }

        if (showChips) {
            val drift = sin(phase * 2 * Math.PI).toFloat()
            HeroChip(
                index = 0, icon = Icons.Outlined.AutoAwesome, label = "Summary",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = height * 0.12f)
                    .graphicsLayer { translationY = drift * 4.dp.toPx() }
            )
            HeroChip(
                index = 1, icon = Icons.Default.RadioButtonUnchecked, label = "Send notes",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = height * 0.05f)
                    .graphicsLayer { translationY = -drift * 4.dp.toPx() }
            )
            HeroChip(
                index = 2, icon = Icons.Outlined.NotificationsActive, label = "Fri, 9 AM",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = height * 0.1f, start = height * 0.06f)
                    .graphicsLayer { translationY = drift * 3.dp.toPx() }
            )
        }
    }
}

/** A small floating card in the illustration, drifting in after the ones before it. */
@Composable
private fun HeroChip(index: Int, icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(450L + 220L * index)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Sem.card,
        border = BorderStroke(1.dp, Sem.accentBorder),
        modifier = modifier.graphicsLayer {
            alpha = appear.value.coerceIn(0f, 1f)
            scaleX = 0.8f + 0.2f * appear.value
            scaleY = 0.8f + 0.2f * appear.value
        }
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Sem.text, maxLines = 1)
        }
    }
}
