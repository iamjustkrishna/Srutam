package space.iamjustkrishna.srutam.matrix

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import space.iamjustkrishna.srutam.ui.screens.*
import java.io.File

abstract class BaseScreenMatrixTest(private val deviceFolder: String) {

    @get:Rule
    val composeRule = createComposeRule()

    protected fun captureScreen(screenName: String, content: @Composable () -> Unit) {
        val outputFile = File("../screenshots/screen-matrix/$deviceFolder/$screenName.png").canonicalFile
        outputFile.parentFile?.mkdirs()
        composeRule.setContent {
            content()
        }
        composeRule.onRoot().captureRoboImage(outputFile.absolutePath)
    }

    @Test
    fun capture_01_splash() {
        captureScreen("01_splash") {
            MatrixSplashPreview()
        }
    }

    @Test
    fun capture_02_permissions() {
        captureScreen("02_permissions") {
            MatrixPermissionsPreview()
        }
    }

    @Test
    fun capture_02b_byok_onboarding() {
        captureScreen("02b_byok_onboarding") {
            MatrixBYOKOnboardingPreview()
        }
    }

    @Test
    fun capture_02c_byok_expanded() {
        captureScreen("02c_byok_expanded") {
            MatrixBYOKExpandedPreview()
        }
    }

    @Test
    fun capture_03_feed_empty() {
        captureScreen("03_feed_empty") {
            MatrixFeedEmptyPreview()
        }
    }

    @Test
    fun capture_04_feed_populated() {
        captureScreen("04_feed_populated") {
            MatrixFeedPopulatedPreview()
        }
    }

    @Test
    fun capture_05_detail() {
        captureScreen("05_detail") {
            MatrixDetailPreview()
        }
    }

    @Test
    fun capture_05_detail_insights() {
        captureScreen("05_detail_insights") {
            MatrixDetailInsightsPreview()
        }
    }

    @Test
    fun capture_06_insights_hub() {
        captureScreen("06_insights_hub") {
            MatrixInsightsPreview()
        }
    }

    @Test
    fun capture_06_insights_ideas() {
        captureScreen("06_insights_ideas") {
            MatrixInsightsIdeasPreview()
        }
    }

    @Test
    fun capture_06_insights_decisions() {
        captureScreen("06_insights_decisions") {
            MatrixInsightsDecisionsPreview()
        }
    }

    @Test
    fun capture_07_copilot_chat() {
        captureScreen("07_copilot_chat") {
            MatrixCopilotPreview()
        }
    }

    @Test
    fun capture_04b_feed_cosmic_dark() {
        captureScreen("04b_feed_cosmic_dark") {
            MatrixFeedCosmicDarkPreview()
        }
    }

    @Test
    fun capture_08_settings() {
        captureScreen("08_settings") {
            MatrixSettingsPreview()
        }
    }

    @Test
    fun capture_08b_settings_cosmic_dark() {
        captureScreen("08b_settings_cosmic_dark") {
            MatrixSettingsCosmicDarkPreview()
        }
    }

    @Test
    fun capture_09_tablet_workspace() {
        captureScreen("09_tablet_workspace") {
            MatrixTabletWorkspacePreview()
        }
    }

    @Test
    fun capture_09b_tablet_workspace_cosmic_dark() {
        captureScreen("09b_tablet_workspace_cosmic_dark") {
            MatrixTabletWorkspaceCosmicDarkPreview()
        }
    }
}
