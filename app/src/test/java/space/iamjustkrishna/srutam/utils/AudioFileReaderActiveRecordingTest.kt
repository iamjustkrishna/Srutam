package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** The file still being recorded is half written, so it must not show up as a note. */
class AudioFileReaderActiveRecordingTest {
    private val finished = File("/music/Srutam/standup.m4a")
    private val recording = File("/music/Srutam/recording_1.m4a")

    @Test fun theFileBeingRecordedIsLeftOut() {
        val listed = AudioFileReader.withoutActiveRecording(arrayOf(finished, recording), recording.absolutePath)

        assertEquals(listOf(finished), listed)
    }

    @Test fun everythingIsListedWhenNothingIsBeingRecorded() {
        val listed = AudioFileReader.withoutActiveRecording(arrayOf(finished, recording), null)

        assertEquals(listOf(finished, recording), listed)
    }

    @Test fun onlyTheExactFileIsLeftOut() {
        val other = File("/music/Srutam/recording_10.m4a")

        val listed = AudioFileReader.withoutActiveRecording(arrayOf(recording, other), recording.absolutePath)

        assertEquals(listOf(other), listed)
    }
}
