package space.iamjustkrishna.srutam.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.core.app.ActivityCompat
import space.iamjustkrishna.srutam.service.MicPermission
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.StoragePermission
import kotlin.math.PI
import kotlin.math.sin

/**
 * First run, one screen: what Srutam does, and a way to try it right away. "Record your first
 * thought" asks for the microphone only when tapped and starts recording; the note it makes is
 * the user's first real taste of the app. "See a sample" opens the welcome note instead. People
 * who used Srutam before can restore their notes (that's when Music and audio access is asked).
 * Nothing else is asked here: the floating button is offered after the first note.
 */
@Composable
fun WelcomeScreen(
    /** The microphone, plus storage on Android 10, where saving into Music/Srutam needs it. */
    micPermissions: Array<String>,
    onRecord: () -> Unit,
    onSeeSample: () -> Unit,
    onRestored: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // null: not asked yet; false: denied (and Android can still ask); "blocked": only Settings can turn it on.
    var micState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { Analytics.log(context, EVENT_WELCOME_VIEWED) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results[Manifest.permission.RECORD_AUDIO] == true
        Analytics.permissionResult(context, "mic", granted, "welcome")
        if (granted) {
            onRecord()
        } else {
            val activity = context as? Activity
            val blocked = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            micState = if (blocked) "blocked" else "denied"
        }
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "storage", granted, "welcome_restore")
        // Home finds the recordings and brings their notes back; if denied, its card offers it again.
        onRestored()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Sem.scaffold)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    Analytics.log(context, EVENT_WELCOME_CHOICE, mapOf(Analytics.PARAM_RESULT to "skip"))
                    onSkip()
                }) {
                    Text("Skip", color = Sem.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ListenIllustration(Modifier.size(width = 260.dp, height = 190.dp))
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Capture a thought.\nGet a useful note.",
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Sem.text
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Just talk. Srutam writes it down on your phone, then pulls out what matters.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    color = Sem.textSecondary,
                    modifier = Modifier.widthIn(max = 340.dp)
                )
                Spacer(Modifier.height(22.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.widthIn(max = 340.dp)
                ) {
                    ValueLine(Icons.Outlined.PhoneAndroid, "Transcribed on your phone, even offline")
                    ValueLine(Icons.Outlined.AutoAwesome, "A summary, tasks, ideas and decisions")
                    ValueLine(Icons.Outlined.NotificationsActive, "Reminders from what you said")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        Analytics.log(context, EVENT_WELCOME_CHOICE, mapOf(Analytics.PARAM_RESULT to "record"))
                        when {
                            MicPermission.isGranted(context) -> onRecord()
                            micState == "blocked" -> openAppSettings(context)
                            else -> micLauncher.launch(micPermissions)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (micState == "blocked") "Allow the microphone in Settings" else "Record your first thought",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = micState,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "micNote"
                ) { state ->
                    Text(
                        text = when (state) {
                            null -> "Srutam will ask for the microphone. Your audio stays on this phone."
                            "blocked" -> "The microphone is off for Srutam. You can still look around first."
                            else -> "No problem. Look at a sample first, and record whenever you like."
                        },
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        color = if (state == null) Sem.textMuted else Sem.amberText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        Analytics.log(context, EVENT_WELCOME_CHOICE, mapOf(Analytics.PARAM_RESULT to "sample"))
                        onSeeSample()
                    },
                    border = BorderStroke(1.dp, Sem.border),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("See a sample note", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Sem.text)
                }
                TextButton(onClick = {
                    Analytics.log(context, EVENT_WELCOME_CHOICE, mapOf(Analytics.PARAM_RESULT to "restore"))
                    if (StoragePermission.isGranted(context)) onRestored() else storageLauncher.launch(StoragePermission.permission)
                }) {
                    Text("Used Srutam before? Restore your notes", fontSize = 13.sp, color = Sem.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private const val EVENT_WELCOME_VIEWED = "welcome_viewed"
private const val EVENT_WELCOME_CHOICE = "welcome_choice"

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
                .background(Sem.accentContainer, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Sem.text, fontWeight = FontWeight.Medium)
    }
}

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
