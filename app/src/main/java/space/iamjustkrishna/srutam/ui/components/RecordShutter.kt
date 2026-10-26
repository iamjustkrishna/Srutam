package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The app's record button, as on the right of the bottom bar: a camera-shutter of a soft red ring,
 * a red disc and a dark core when ready; a red circle with a white rounded square while recording.
 * Drawn at any [size] (the bottom bar's is 56 dp), so other screens look the same.
 */
@Composable
fun RecordShutter(recording: Boolean, size: Dp, modifier: Modifier = Modifier) {
    if (recording) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFEF4444)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size * 0.32f)
                    .clip(RoundedCornerShape(size * 0.07f))
                    .background(Color.White)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFFECACA))
                .border(size * 0.018f, Color(0xFFFCA5A5).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size * 0.75f)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFB91C1C)))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(size * 0.43f)
                        .clip(CircleShape)
                        .background(Color(0xFF991B1B))
                )
            }
        }
    }
}
