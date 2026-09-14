package space.iamjustkrishna.srutam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.StardustGold
import space.iamjustkrishna.srutam.ui.theme.StardustWhite

private data class StarSeed(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val alpha: Float,
    val isGold: Boolean
)

// 48 deterministic star coordinates evenly and naturally distributed across screen viewport
private val StaticStarField = listOf(
    StarSeed(0.08f, 0.06f, 1.2f, 0.16f, false),
    StarSeed(0.24f, 0.04f, 0.9f, 0.12f, true),
    StarSeed(0.42f, 0.08f, 1.4f, 0.18f, false),
    StarSeed(0.68f, 0.05f, 1.0f, 0.14f, false),
    StarSeed(0.88f, 0.07f, 1.3f, 0.15f, true),
    StarSeed(0.15f, 0.14f, 0.8f, 0.11f, false),
    StarSeed(0.35f, 0.16f, 1.5f, 0.20f, false),
    StarSeed(0.55f, 0.12f, 1.1f, 0.13f, true),
    StarSeed(0.78f, 0.15f, 0.9f, 0.12f, false),
    StarSeed(0.92f, 0.18f, 1.2f, 0.16f, false),
    StarSeed(0.05f, 0.25f, 1.0f, 0.13f, false),
    StarSeed(0.22f, 0.28f, 1.4f, 0.18f, true),
    StarSeed(0.48f, 0.23f, 0.8f, 0.10f, false),
    StarSeed(0.65f, 0.27f, 1.3f, 0.15f, false),
    StarSeed(0.82f, 0.24f, 1.1f, 0.14f, true),
    StarSeed(0.12f, 0.36f, 1.3f, 0.17f, false),
    StarSeed(0.30f, 0.34f, 0.9f, 0.12f, false),
    StarSeed(0.58f, 0.38f, 1.5f, 0.19f, false),
    StarSeed(0.74f, 0.33f, 1.0f, 0.13f, true),
    StarSeed(0.94f, 0.39f, 0.8f, 0.11f, false),
    StarSeed(0.06f, 0.46f, 1.1f, 0.14f, true),
    StarSeed(0.26f, 0.48f, 1.4f, 0.18f, false),
    StarSeed(0.44f, 0.44f, 0.9f, 0.12f, false),
    StarSeed(0.66f, 0.47f, 1.2f, 0.15f, false),
    StarSeed(0.86f, 0.45f, 1.3f, 0.16f, true),
    StarSeed(0.18f, 0.56f, 0.8f, 0.10f, false),
    StarSeed(0.38f, 0.54f, 1.5f, 0.20f, true),
    StarSeed(0.52f, 0.58f, 1.0f, 0.13f, false),
    StarSeed(0.72f, 0.55f, 1.3f, 0.16f, false),
    StarSeed(0.90f, 0.57f, 0.9f, 0.12f, false),
    StarSeed(0.08f, 0.66f, 1.2f, 0.15f, false),
    StarSeed(0.28f, 0.64f, 1.0f, 0.13f, true),
    StarSeed(0.48f, 0.68f, 1.4f, 0.18f, false),
    StarSeed(0.68f, 0.63f, 0.8f, 0.11f, false),
    StarSeed(0.84f, 0.67f, 1.3f, 0.16f, true),
    StarSeed(0.14f, 0.76f, 0.9f, 0.12f, false),
    StarSeed(0.32f, 0.74f, 1.5f, 0.19f, false),
    StarSeed(0.56f, 0.77f, 1.1f, 0.14f, true),
    StarSeed(0.76f, 0.73f, 1.0f, 0.13f, false),
    StarSeed(0.92f, 0.78f, 1.2f, 0.15f, false),
    StarSeed(0.06f, 0.86f, 1.3f, 0.17f, true),
    StarSeed(0.24f, 0.88f, 0.8f, 0.11f, false),
    StarSeed(0.45f, 0.84f, 1.4f, 0.18f, false),
    StarSeed(0.64f, 0.87f, 1.0f, 0.13f, false),
    StarSeed(0.85f, 0.85f, 1.2f, 0.16f, true),
    StarSeed(0.18f, 0.95f, 1.0f, 0.13f, false),
    StarSeed(0.50f, 0.93f, 1.3f, 0.16f, false),
    StarSeed(0.80f, 0.96f, 0.9f, 0.12f, true)
)

/**
 * Renders a lightweight cosmic starfield and subtle nebula gradient.
 * Completely static with zero animation loops to eliminate battery drain while providing
 * an authentic space atmosphere in Cosmic Void dark mode.
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier
) {
    val isCosmic = LocalIsCosmicDark.current
    if (!isCosmic) return

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // Deep space void radial gradient with subtle purple-blue center
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F172A).copy(alpha = 0.35f),
                    Color(0xFF090D20).copy(alpha = 0.65f),
                    CosmicVoidBackground
                ),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.maxDimension * 0.85f
            )
        )

        // Static stardust dots
        for (i in StaticStarField.indices) {
            val star = StaticStarField[i]
            val x = star.x * size.width
            val y = star.y * size.height
            val color = if (star.isGold) {
                StardustGold.copy(alpha = star.alpha)
            } else {
                StardustWhite.copy(alpha = star.alpha)
            }
            drawCircle(
                color = color,
                radius = star.radiusDp.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
