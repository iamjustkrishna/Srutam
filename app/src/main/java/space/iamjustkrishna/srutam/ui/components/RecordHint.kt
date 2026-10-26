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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark

/**
 * A one-time hint above the record button: "Tap to record · hold to talk". It rises in a moment
 * after Home first opens, says it once, and leaves after a few seconds or at the first tap.
 * Hold-to-talk and tap-to-lock aren't obvious from the button alone; this is the only place
 * Srutam says it.
 */
@Composable
fun RecordHint(enabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled || RecordHintState.wasShown(context)) return@LaunchedEffect
        delay(1_100L)
        RecordHintState.markShown(context)
        visible = true
        delay(6_000L)
        visible = false
    }
    // Recording started (the button was used): the hint has done its job.
    LaunchedEffect(enabled) { if (!enabled) visible = false }

    val dark = LocalIsCosmicDark.current
    val fill = if (dark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val strong = if (dark) Color(0xFF0F172A) else Color.White
    val soft = if (dark) Color(0xFF475569) else Color(0xFFCBD5E1)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { visible = false }
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = fill, shadowElevation = 6.dp) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = strong, fontWeight = FontWeight.SemiBold)) { append("Tap to record") }
                        withStyle(SpanStyle(color = soft)) { append("  ·  hold to talk") }
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
            // A small pointer down to the button's centre (the button is 56 dp wide).
            Canvas(Modifier.padding(end = 22.dp).size(width = 12.dp, height = 6.dp)) {
                drawPath(
                    Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height)
                        close()
                    },
                    fill
                )
            }
        }
    }
}

/** Whether the record hint has been shown on this install. */
internal object RecordHintState {
    private const val PREFS = "srutam_hints"
    private const val KEY = "record_hint_shown"

    fun wasShown(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markShown(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}
