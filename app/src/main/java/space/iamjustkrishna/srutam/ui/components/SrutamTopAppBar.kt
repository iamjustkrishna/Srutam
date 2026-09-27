package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.*

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

@Composable
fun SquircleActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val isDark = LocalIsCosmicDark.current
    val resolvedTint = tint ?: if (isDark) TextOnDarkPrimary else Color(0xFF1E2229)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) CosmicVoidCard else Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFD6E0EC).copy(alpha = 0.85f)),
        shadowElevation = if (isDark) 0.dp else 1.dp,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SrutamTopAppBar(
    title: String = "Srutam",
    accentText: String? = null,
    subtitle: String? = null,
    subtitleIcon: ImageVector? = null,
    subtitleColor: Color? = null,
    titleFontWeight: FontWeight = FontWeight.Bold,
    accentFontWeight: FontWeight = FontWeight.Medium,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val resolvedSubtitleColor = subtitleColor ?: if (isDark) TextOnDarkSecondary else TextSecondary
    val titleFontSize = if (accentText != null) 24.sp else 30.sp
    val accentFontSize = if (accentText != null) 24.sp else 30.sp

    Surface(
        color = if (isDark) CosmicVoidBackground.copy(alpha = 0.85f) else Color(0xFFF4F5F8).copy(alpha = 0.85f),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = if (isDark) CosmicVoidCardBorder.copy(alpha = 0.6f) else Color(0xFFD6E0EC).copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = titleFontSize,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = titleFontWeight,
                        color = if (isDark) TextOnDarkPrimary else Color(0xFF1E2229),
                        maxLines = 1
                    )
                    if (accentText != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = accentText,
                            fontSize = accentFontSize,
                            fontFamily = PlayfairDisplayFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = accentFontWeight,
                            color = if (isDark) CosmicGlowBlue else CobaltBlue,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (subtitleIcon != null) {
                        Icon(
                            imageVector = subtitleIcon,
                            contentDescription = null,
                            tint = resolvedSubtitleColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = resolvedSubtitleColor,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
