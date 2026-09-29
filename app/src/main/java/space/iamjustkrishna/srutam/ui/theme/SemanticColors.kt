package space.iamjustkrishna.srutam.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Colours that follow the active theme. Use these instead of fixed light values so a screen
 * automatically looks right in both the light ceramic and cosmic dark themes.
 */
object Sem {
    private val dark @Composable @ReadOnlyComposable get() = LocalIsCosmicDark.current

    val scaffold @Composable @ReadOnlyComposable get() = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8)
    val card @Composable @ReadOnlyComposable get() = if (dark) CosmicVoidCard else CeramicWhite
    val border @Composable @ReadOnlyComposable get() = if (dark) CosmicVoidCardBorder else SlateBorder
    /** Fill for the selected segment in a segmented control: lifted from its track in both themes. */
    val selected @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF243357) else Color.White
    val chip @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF1B2540) else Color(0xFFF2F2F7)
    val soft @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF0C1225) else SlateSurface

    val text @Composable @ReadOnlyComposable get() = if (dark) TextOnDarkPrimary else TextPrimary
    val textSecondary @Composable @ReadOnlyComposable get() = if (dark) TextOnDarkSecondary else TextSecondary
    val textMuted @Composable @ReadOnlyComposable get() = if (dark) TextOnDarkSecondary.copy(alpha = 0.7f) else Color(0xFF8E8E93)

    val accent @Composable @ReadOnlyComposable get() = if (dark) CosmicGlowBlue else CobaltBlue
    val accentContainer @Composable @ReadOnlyComposable get() = if (dark) CobaltBlue.copy(alpha = 0.22f) else CobaltContainer
    val accentBorder @Composable @ReadOnlyComposable get() = if (dark) CosmicGlowBlue.copy(alpha = 0.35f) else CobaltBorder

    val amber @Composable @ReadOnlyComposable get() = if (dark) StardustGold else Color(0xFFD97706)
    val amberText @Composable @ReadOnlyComposable get() = if (dark) StardustGold else Color(0xFFB45309)
    val amberContainer @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7)
    val amberSoft @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF78350F).copy(alpha = 0.2f) else Color(0xFFFFFBEB)
    val amberBorder @Composable @ReadOnlyComposable get() = if (dark) StardustGold.copy(alpha = 0.3f) else Color(0xFFFDE68A)

    val emerald @Composable @ReadOnlyComposable get() = if (dark) CosmicAuroraGreen else EmeraldSuccess
    val emeraldContainer @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF06281E) else EmeraldContainer
    val onEmerald @Composable @ReadOnlyComposable get() = if (dark) CosmicAuroraGreen else OnEmeraldContainer
    val emeraldBorder @Composable @ReadOnlyComposable get() = if (dark) CosmicAuroraGreen.copy(alpha = 0.3f) else Color(0xFFA7F3D0)

    val error @Composable @ReadOnlyComposable get() = if (dark) Color(0xFFF87171) else Color(0xFFDC2626)
    val errorContainer @Composable @ReadOnlyComposable get() = if (dark) Color(0xFF3B1212) else Color(0xFFFEF2F2)
    val errorBorder @Composable @ReadOnlyComposable get() = if (dark) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFFFECACA)
    val onErrorContainer @Composable @ReadOnlyComposable get() = if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
}
