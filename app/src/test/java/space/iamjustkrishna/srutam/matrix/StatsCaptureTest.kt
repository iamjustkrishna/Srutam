package space.iamjustkrishna.srutam.matrix

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.player.PlaybackState
import space.iamjustkrishna.srutam.ui.screens.FeedScreenContent
import space.iamjustkrishna.srutam.ui.screens.ScreenMatrixMocks
import space.iamjustkrishna.srutam.ui.screens.StatsScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/** Notes with the time-saved line and its one-time tip, and the Your Srutam screen, at several shapes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val longTranscript = List(220) { "word$it" }.joinToString(" ")

    /** The mock notes, each with a long transcript so there is time saved to show. */
    private val recordings = ScreenMatrixMocks.mockRecordings.mapValues { (_, r) -> r.copy(transcript = longTranscript) }

    private fun shot(name: String) {
        val file = File("../screenshots/screen-matrix/stats/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    private fun feed(mode: ThemeMode, name: String, fontScale: Float = 1f) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                SrutamTheme(themeMode = mode) {
                    FeedScreenContent(
                        audioFiles = ScreenMatrixMocks.mockAudioFiles,
                        recordingsByPath = recordings,
                        playbackState = PlaybackState(),
                        isLoading = false,
                        isOnline = true
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(2_500)
        shot(name)
    }

    private fun stats(mode: ThemeMode, name: String, empty: Boolean = false) {
        composeRule.setContent {
            SrutamTheme(themeMode = mode) {
                StatsScreen(recordings = if (empty) emptyList() else recordings.values, onNavigateBack = {})
            }
        }
        shot(name)
    }

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun feedPhoneWithTipLight() = feed(ThemeMode.LIGHT, "feed-phone-tip-light")

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun feedPhoneWithTipDark() = feed(ThemeMode.COSMIC_DARK, "feed-phone-tip-dark")

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi", sdk = [34])
    fun feedSmallPhoneLargestText() = feed(ThemeMode.LIGHT, "feed-small-phone-2x-text", fontScale = 2f)

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun statsPhoneLight() = stats(ThemeMode.LIGHT, "stats-phone-light")

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun statsPhoneDark() = stats(ThemeMode.COSMIC_DARK, "stats-phone-dark")

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun statsPhoneEmpty() = stats(ThemeMode.LIGHT, "stats-phone-empty", empty = true)

    @Test @Config(qualifiers = "w780dp-h360dp-land-xxhdpi", sdk = [34])
    fun statsPhoneLandscape() = stats(ThemeMode.LIGHT, "stats-phone-landscape")

    @Test @Config(qualifiers = "w800dp-h1280dp-xhdpi", sdk = [34])
    fun statsTablet() = stats(ThemeMode.LIGHT, "stats-tablet")
}
