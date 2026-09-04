package space.iamjustkrishna.srutam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleStudioDarkColorScheme = darkColorScheme(
    primary = CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = CobaltBlueDark,
    onPrimaryContainer = Color.White,
    secondary = StudioCrimson,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF451214),
    onSecondaryContainer = Color(0xFFFCA5A5),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkSurfaceBase,
    onBackground = TextOnDarkPrimary,
    surface = DarkSurfaceCard,
    onSurface = TextOnDarkPrimary,
    surfaceVariant = DarkSurfaceBorder,
    onSurfaceVariant = TextOnDarkSecondary,
    outline = DarkSurfaceBorder,
    error = StudioCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF451214),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val AppleStudioLightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = StudioCrimson,
    onSecondary = Color.White,
    secondaryContainer = StudioCrimsonContainer,
    onSecondaryContainer = OnStudioCrimsonContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    background = CeramicWhite,
    onBackground = TextPrimary,
    surface = CeramicWhite,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = StudioCrimson,
    onError = Color.White,
    errorContainer = StudioCrimsonContainer,
    onErrorContainer = OnStudioCrimsonContainer
)

@Composable
fun SrutamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        AppleStudioDarkColorScheme
    } else {
        AppleStudioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}