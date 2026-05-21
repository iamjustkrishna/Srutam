package space.iamjustkrishna.srutam.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PremiumDarkColorScheme = darkColorScheme(
    primary = PremiumBlue40,           // Deep blue primary
    secondary = PremiumPurple40,       // Deep purple secondary
    tertiary = PremiumAccentCyan,      // Bright cyan accent
    background = PremiumSurfaceDark,   // Very dark background
    surface = Color(0xFF1A2332),       // Dark surface
    onPrimary = Color(0xFFFFFFFF),     // White text on primary
    onSecondary = Color(0xFFFFFFFF),   // White text on secondary
    onBackground = Color(0xFFE8E8E8),  // Light text on background
    onSurface = Color(0xFFE8E8E8),     // Light text on surface
    primaryContainer = Color(0xFF254B7A), // Darker blue container
    secondaryContainer = Color(0xFF4A235A), // Darker purple container
    tertiaryContainer = Color(0xFF004D5C)  // Dark teal container
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = PremiumBlue40,           // Deep blue primary
    secondary = PremiumPurple40,       // Deep purple secondary
    tertiary = PremiumAccentCyan,      // Bright cyan accent
    background = PremiumSurfaceLight,  // Light background
    surface = Color(0xFFFFFFFF),       // White surface
    onPrimary = Color(0xFFFFFFFF),     // White text on primary
    onSecondary = Color(0xFFFFFFFF),   // White text on secondary
    onBackground = Color(0xFF1A1A1A),  // Dark text on light background
    onSurface = Color(0xFF1A1A1A),     // Dark text on surface
    primaryContainer = Color(0xFFCFDEFF), // Light blue container
    secondaryContainer = Color(0xFFF3E5F5), // Light purple container
    tertiaryContainer = Color(0xFFB2EBF2)  // Light teal container
)

@Composable
fun SrutamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled dynamic color for consistent premium theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        PremiumDarkColorScheme
    } else {
        PremiumLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}