package space.iamjustkrishna.srutam.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.components.TimeSavedLine
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.utils.TimeSaved
import java.io.File

/**
 * The line under "Srutam" in the Notes header at several widths and text sizes: it must always
 * fit beside the icons, never wrap, and hide until a minute has been saved.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1200dp-xhdpi", sdk = [34])
class TimeSavedLineCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun totals(month: Long, all: Long = month) = TimeSaved.Totals(month, all, 12, 900, 400, 1500)

    private fun setHeaders(mode: ThemeMode, rows: List<Triple<String, Int, Float>>, t: TimeSaved.Totals) {
        composeRule.setContent {
            SrutamTheme(themeMode = mode) {
                Column(Modifier.background(Sem.scaffold).padding(8.dp)) {
                    rows.forEach { (label, widthDp, fontScale) ->
                        Text(label, fontSize = 11.sp)
                        val d = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                            Box(Modifier.width(widthDp.dp)) {
                                SrutamTopAppBar(
                                    title = "Srutam",
                                    belowTitle = { TimeSavedLine(t, onClick = {}) },
                                    actions = {
                                        SquircleActionButton(Icons.Default.Search, "Search", onClick = {})
                                        SquircleActionButton(Icons.Default.Settings, "Settings", onClick = {})
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    private val rows = listOf(
        Triple("412 dp", 412, 1f),
        Triple("360 dp", 360, 1f),
        Triple("320 dp", 320, 1f),
        Triple("280 dp (tiny)", 280, 1f),
        Triple("360 dp, text 1.3x", 360, 1.3f),
        Triple("360 dp, text 2x", 360, 2f),
        Triple("320 dp, text 2x", 320, 2f)
    )

    private fun shot(name: String) {
        val file = File("../screenshots/screen-matrix/time-saved-line/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun sizesLight() {
        setHeaders(ThemeMode.LIGHT, rows, totals(6 * 3600L + 23 * 60))
        shot("sizes-light")
    }

    @Test fun sizesDark() {
        setHeaders(ThemeMode.COSMIC_DARK, rows, totals(6 * 3600L + 23 * 60))
        shot("sizes-dark")
    }

    @Test fun earlyInTheMonthFallsBackToAllTime() {
        setHeaders(ThemeMode.LIGHT, rows.take(3), totals(month = 20, all = 4 * 3600L + 5 * 60))
        shot("all-time-fallback")
    }

    @Test fun lineNeverCrossesTheIcons() {
        setHeaders(ThemeMode.LIGHT, listOf(Triple("320 dp, text 2x", 320, 2f)), totals(6 * 3600L + 23 * 60))
        val line = composeRule.onNode(hasContentDescription("saved", substring = true))
        line.assertIsDisplayed()
        val lineBounds = line.getUnclippedBoundsInRoot()
        val iconBounds = composeRule.onNodeWithContentDescription("Search").getUnclippedBoundsInRoot()
        assertTrue("line ${lineBounds.right} runs under the search icon at ${iconBounds.left}", lineBounds.right <= iconBounds.left)
    }

    @Test fun hiddenUnderTheMinimum() {
        // under StatsConfig.HEADER_LINE_SECONDS (10 s)
        setHeaders(ThemeMode.LIGHT, rows.take(1), totals(month = 4, all = 8))
        composeRule.onAllNodes(hasContentDescription("saved", substring = true)).assertCountEquals(0)
    }
}
