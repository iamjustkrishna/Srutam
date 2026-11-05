package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.utils.StatsConfig

enum class PillStyle {
    /** The one action: a soft blue with a light edge and a glow. */
    Primary,

    /** The quiet choice beside it: an outline that follows the sheet's light or dark. */
    Secondary,

    /** Just words, for "Not now" under a primary button. */
    Text
}

/**
 * The button for the stats screens and their sheets: a pill, like the record button, in a few
 * sizes from [StatsConfig]. [onDark] says whether it sits on a dark surface.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    compact: Boolean = false,
    enabled: Boolean = true,
    onDark: Boolean = LocalIsCosmicDark.current
) {
    val height = (if (compact) StatsConfig.BUTTON_COMPACT_DP else StatsConfig.BUTTON_HEIGHT_DP).dp
    val shape = CircleShape
    val textColor: Color
    val base = modifier.height(height)
    val styled = when (style) {
        PillStyle.Primary -> {
            textColor = Color.White
            base
                .shadow(if (compact) 4.dp else 8.dp, shape, ambientColor = Cobalt.copy(alpha = 0.5f), spotColor = Cobalt.copy(alpha = 0.6f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Cobalt)))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)), shape)
        }
        PillStyle.Secondary -> {
            textColor = if (onDark) Ice else Color(0xFF0F172A)
            base
                .clip(shape)
                .background(if (onDark) Color.White.copy(alpha = 0.08f) else Color.White)
                .border(1.dp, if (onDark) Color.White.copy(alpha = 0.18f) else Color(0xFFD6E0EC), shape)
        }
        PillStyle.Text -> {
            textColor = if (onDark) Glow else Color(0xFF64748B)
            base.clip(shape)
        }
    }
    Row(
        styled
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .then(Modifier.padding(horizontal = if (compact) 14.dp else if (style == PillStyle.Text) 12.dp else 22.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(if (compact) 14.dp else 16.dp))
        Text(text, fontSize = if (compact) 13.sp else 14.5.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
    }
}
