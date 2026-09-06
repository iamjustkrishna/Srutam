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
    fun capture_06_insights_hub() {
        captureScreen("06_insights_hub") {
            MatrixInsightsPreview()
        }
    }

    @Test
    fun capture_07_copilot_chat() {
        captureScreen("07_copilot_chat") {
            MatrixCopilotPreview()
        }
    }

    @Test
    fun capture_08_settings() {
        captureScreen("08_settings") {
            MatrixSettingsPreview()
        }
    }
}
