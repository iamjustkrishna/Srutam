package space.iamjustkrishna.srutam.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp

private val Ring = Color(0xFFFECACA)
private val RingBorder = Color(0xFFFCA5A5)
private val Red = Color(0xFFEF4444)
private val RedMid = Color(0xFFDC2626)
private val RedDeep = Color(0xFFB91C1C)
private val Core = Color(0xFF991B1B)

/**
 * The app's record button, as on the right of the bottom bar: a camera-shutter of a soft red ring,
 * a red disc and a dark core when ready; a red circle with a white rounded square while recording.
 * Switching morphs one into the other (the disc shrinks and squares off into the stop square).
 * Drawn at any [size] (the bottom bar's is 56 dp), so other screens look the same.
 */
@Composable
fun RecordShutter(recording: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(
        targetValue = if (recording) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "shutterMorph"
    )
    val t = p.coerceIn(0f, 1f)
    val innerSize = lerp(size * 0.75f, size * 0.32f, t)
    val innerCorner = lerp(innerSize / 2, size * 0.07f, t)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(lerp(Ring, Red, t))
            .border(size * 0.018f, RingBorder.copy(alpha = 0.5f * (1f - t)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCorner))
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(Red, Color.White, t), lerp(RedMid, Color.White, t), lerp(RedDeep, Color.White, t))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size * 0.43f * (1f - t))
                    .alpha(1f - t)
                    .clip(CircleShape)
                    .background(Core)
            )
        }
    }
}
