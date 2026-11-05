package space.iamjustkrishna.srutam.matrix

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.height
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.components.TimeSavedLine
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.utils.StatsConfig
import space.iamjustkrishna.srutam.utils.TimeSaved
import java.io.File

/** The title and the buttons sit in the same place on Notes, Insights and AI, with or without the time-saved line. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
class HeaderConsistencyTest {
    @get:Rule val rule = createComposeRule()

    private enum class Tab { NOTES, NOTES_WITH_LINE, INSIGHTS, AI }

    private val totals = TimeSaved.Totals(3_700L, 3_700L, 3, 600, 300, 1_000)

    @Test fun titleAndButtonsDoNotMoveBetweenTabs() {
        var tab by mutableStateOf(Tab.NOTES)
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                when (tab) {
                    Tab.NOTES -> SrutamTopAppBar(title = "Srutam", actions = {
                        SquircleActionButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = {})
                        SquircleActionButton(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
                    })
                    Tab.NOTES_WITH_LINE -> SrutamTopAppBar(title = "Srutam", belowTitle = { TimeSavedLine(totals = totals, onClick = {}) }, actions = {
                        SquircleActionButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = {})
                        SquircleActionButton(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
                    })
                    Tab.INSIGHTS -> SrutamTopAppBar(title = "Srutam", accentText = "Insights", actions = {
                        SquircleActionButton(icon = Icons.Default.History, contentDescription = "History", onClick = {})
                        SquircleActionButton(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
                    })
                    Tab.AI -> SrutamTopAppBar(title = "Srutam", accentText = "AI", actions = {
                        SquircleActionButton(icon = Icons.Default.History, contentDescription = "History", onClick = {})
                        SquircleActionButton(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
                    })
                }
            }
        }
        fun measure(): Triple<Float, Float, Float> {
            rule.waitForIdle()
            val title = rule.onNodeWithText("Srutam").getBoundsInRoot()
            val settings = rule.onNodeWithContentDescription("Settings").getBoundsInRoot()
            return Triple(title.top.value, title.height.value, settings.top.value)
        }
        val notes = measure()
        tab = Tab.NOTES_WITH_LINE; val withLine = measure()
        tab = Tab.INSIGHTS; val insights = measure()
        tab = Tab.AI; val ai = measure()
        assertEquals("Notes and Insights titles", notes, insights)
        assertEquals("Notes with the line and Insights", withLine, insights)
        assertEquals("AI and Insights", ai, insights)
    }

    @Test fun lineStartsAtTheConfiguredMinimum() {
        val atMinimum = TimeSaved.Totals(StatsConfig.HEADER_LINE_SECONDS, StatsConfig.HEADER_LINE_SECONDS, 1, 10, 5, 12)
        val below = TimeSaved.Totals(StatsConfig.HEADER_LINE_SECONDS - 1, StatsConfig.HEADER_LINE_SECONDS - 1, 1, 10, 5, 12)
        assertEquals(true, atMinimum.worthShowingInHeader)
        assertEquals(false, below.worthShowingInHeader)
        // The Your Srutam screen opens at or before the line.
        assertEquals(true, StatsConfig.MIN_SECONDS_TO_SHOW <= StatsConfig.HEADER_LINE_SECONDS)
    }

    @Test fun capture() {
        rule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { SrutamTopAppBar(title = "Srutam", belowTitle = { TimeSavedLine(totals = totals, onClick = {}) }, actions = {
            SquircleActionButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = {})
            SquircleActionButton(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
        }) } }
        val file = File("../screenshots/screen-matrix/header-compare/notes-with-line.png").canonicalFile
        file.parentFile?.mkdirs()
        rule.onRoot().captureRoboImage(file.absolutePath)
    }
}
