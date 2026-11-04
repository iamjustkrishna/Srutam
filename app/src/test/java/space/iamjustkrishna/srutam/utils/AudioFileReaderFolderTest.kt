package space.iamjustkrishna.srutam.utils

import android.app.Application
import android.os.Environment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Looking for recordings must not leave a Music/Srutam folder behind. An empty one made every later
 * install believe there were "recordings from before" to bring back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AudioFileReaderFolderTest {

    private val srutamFolder: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Srutam")

    @Test fun scanningWithNoRecordingsCreatesNoFolder() {
        srutamFolder.deleteRecursively()

        val files = AudioFileReader.getAudioFiles()

        assertEquals(0, files.size)
        assertFalse("the scan left an empty folder", srutamFolder.exists())
    }

    @Test fun askingForTheFolderCreatesNothingEither() {
        srutamFolder.deleteRecursively()

        AudioFileReader.getRecordingsDirectory()

        assertFalse(srutamFolder.exists())
    }

    @Test fun recordingsAreStillFoundWhenTheFolderHasThem() {
        srutamFolder.mkdirs()
        File(srutamFolder, "a.m4a").writeBytes(ByteArray(10))
        try {
            assertTrue(AudioFileReader.getAudioFiles().any { it.fileName == "a.m4a" })
        } finally {
            srutamFolder.deleteRecursively()
        }
    }
}
