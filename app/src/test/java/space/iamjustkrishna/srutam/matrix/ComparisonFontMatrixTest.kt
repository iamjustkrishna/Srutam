package space.iamjustkrishna.srutam.matrix

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.screens.GlobalChatMessage
import space.iamjustkrishna.srutam.ui.screens.GlobalCopilotContent
import space.iamjustkrishna.srutam.ui.screens.InsightsContent
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class ComparisonFontPortraitMatrixTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(name: String) {
        val artifactDir = File("C:/Users/krish/.gemini/antigravity-ide/brain/d04e589c-b70f-4b55-ae86-2296b5c54257")
        if (artifactDir.exists()) {
            rule.onRoot().captureRoboImage(File(artifactDir, "$name.png").absolutePath)
        }
        val localOutput = File("src/test/screenshots/font_comparison/$name.png")
        localOutput.parentFile?.mkdirs()
        rule.onRoot().captureRoboImage(localOutput.absolutePath)
    }

    @Test
    fun insightsFont16sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    InsightsContent(
                        state = InsightsFixtures.state(listOf(InsightsFixtures.idea, InsightsFixtures.decision)),
                        accentFontSize = 16.sp
                    )
                }
            }
        }
        capture("insights_16sp")
    }

    @Test
    fun insightsFont20sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    InsightsContent(
                        state = InsightsFixtures.state(listOf(InsightsFixtures.idea, InsightsFixtures.decision)),
                        accentFontSize = 20.sp
                    )
                }
            }
        }
        capture("insights_20sp")
    }

    @Test
    fun insightsFont22sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    InsightsContent(
                        state = InsightsFixtures.state(listOf(InsightsFixtures.idea, InsightsFixtures.decision)),
                        accentFontSize = 22.sp
                    )
                }
            }
        }
        capture("insights_22sp")
    }

    @Test
    fun aiFont16sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    GlobalCopilotContent(
                        messages = listOf(
                            GlobalChatMessage(
                                text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
                                isUser = false
                            )
                        ),
                        inputText = "",
                        accentFontSize = 16.sp
                    )
                }
            }
        }
        capture("ai_16sp")
    }

    @Test
    fun aiFont20sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    GlobalCopilotContent(
                        messages = listOf(
                            GlobalChatMessage(
                                text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
                                isUser = false
                            )
                        ),
                        inputText = "",
                        accentFontSize = 20.sp
                    )
                }
            }
        }
        capture("ai_20sp")
    }

    @Test
    fun aiFont22sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    GlobalCopilotContent(
                        messages = listOf(
                            GlobalChatMessage(
                                text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
                                isUser = false
                            )
                        ),
                        inputText = "",
                        accentFontSize = 22.sp
                    )
                }
            }
        }
        capture("ai_22sp")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-xhdpi", application = Application::class)
class ComparisonFontLandscapeMatrixTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(name: String) {
        val artifactDir = File("C:/Users/krish/.gemini/antigravity-ide/brain/d04e589c-b70f-4b55-ae86-2296b5c54257")
        if (artifactDir.exists()) {
            rule.onRoot().captureRoboImage(File(artifactDir, "$name.png").absolutePath)
        }
        val localOutput = File("src/test/screenshots/font_comparison/$name.png")
        localOutput.parentFile?.mkdirs()
        rule.onRoot().captureRoboImage(localOutput.absolutePath)
    }

    @Test
    fun insightsLandscape20sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    InsightsContent(
                        state = InsightsFixtures.state(listOf(InsightsFixtures.idea)),
                        accentFontSize = 20.sp
                    )
                }
            }
        }
        capture("insights_landscape_20sp")
    }

    @Test
    fun aiLandscape20sp() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    GlobalCopilotContent(
                        messages = listOf(
                            GlobalChatMessage(
                                text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
                                isUser = false
                            )
                        ),
                        inputText = "",
                        accentFontSize = 20.sp
                    )
                }
            }
        }
        capture("ai_landscape_20sp")
    }
}
