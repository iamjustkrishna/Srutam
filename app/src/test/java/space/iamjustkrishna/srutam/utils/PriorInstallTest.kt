package space.iamjustkrishna.srutam.utils

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/** Access to Music and audio is asked for only when an earlier install left its Srutam folder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PriorInstallTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun music(withSrutamFolder: Boolean): File {
        val dir = Files.createTempDirectory("music").toFile()
        if (withSrutamFolder) File(dir, "Srutam").mkdirs()
        return dir
    }

    @Test fun aNewInstallWithNoFolderIsNeverAsked() {
        PriorInstall.checkOnce(context, music(withSrutamFolder = false))

        assertFalse(PriorInstall.recordingsFromBefore(context))
        assertFalse(PriorInstall.shouldAskForAccess(context))
    }

    @Test fun anExistingFolderMeansAskOnce() {
        PriorInstall.checkOnce(context, music(withSrutamFolder = true))

        assertTrue(PriorInstall.shouldAskForAccess(context))

        PriorInstall.markPrompted(context)
        assertFalse("it asks once", PriorInstall.shouldAskForAccess(context))
    }

    @Test fun theAnswerIsDecidedOnTheFirstLaunchOnly() {
        PriorInstall.checkOnce(context, music(withSrutamFolder = false))
        // A folder showing up later (this install saved a recording) changes nothing.
        PriorInstall.checkOnce(context, music(withSrutamFolder = true))

        assertFalse(PriorInstall.shouldAskForAccess(context))
    }
}
