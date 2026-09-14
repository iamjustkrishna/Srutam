package space.iamjustkrishna.srutam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT,
    COSMIC_DARK,
    SYSTEM
}

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalIsCosmicDark = compositionLocalOf { false }

private val CosmicVoidColorScheme = darkColorScheme(
    primary = CosmicGlowBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = StudioCrimson,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF451214),
    onSecondaryContainer = Color(0xFFFCA5A5),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2E1065),
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = CosmicVoidBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = CosmicVoidCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CosmicVoidCardBorder,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CosmicVoidCardBorder,
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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.COSMIC_DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CosmicVoidColorScheme
    } else {
        AppleStudioLightColorScheme
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalIsCosmicDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}